-- =============================================================================
-- PISSIR - Connected Games Platform
-- V1 : schema iniziale del database centrale (PostgreSQL)
--
-- Convenzioni adottate:
--   * chiavi primarie surrogate  : id BIGINT GENERATED ALWAYS AS IDENTITY
--   * chiavi esterne             : <entita>_id
--   * date e orari               : TIMESTAMPTZ (il backend lavora in UTC)
--   * valori enumerati           : VARCHAR + vincolo CHECK (niente tabelle di
--                                  lookup e niente tipi ENUM nativi, che sono
--                                  scomodi da far evolvere con Flyway)
--
-- Nota sulle colonne "codice_tipo_gioco" / "codice_tipo_evento":
--   sono copie controllate del codice del tipo, propagate lungo le chiavi
--   esterne composite. Servono a rendere verificabili dal database vincoli
--   che altrimenti richiederebbero un trigger, per esempio:
--     - una configurazione freccette puo' esistere solo per un gioco di tipo
--       FRECCETTE;
--     - un evento di partita puo' avere solo un tipo di evento previsto dal
--       tipo di gioco di quella partita;
--     - un tiro puo' riferirsi solo a un evento TIRO_RILEVATO.
--   Sono ridondanze volute e non aggiornabili in modo incoerente, perche'
--   ogni copia e' ancorata all'originale da una chiave esterna composita.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Funzione condivisa per l'aggiornamento automatico di data_aggiornamento
-- -----------------------------------------------------------------------------
-- Viene usata clock_timestamp() e non now(): now() restituisce l'istante di
-- inizio della transazione, quindi una riga creata e poi modificata nella
-- stessa transazione risulterebbe non aggiornata.
CREATE OR REPLACE FUNCTION imposta_data_aggiornamento()
RETURNS TRIGGER AS $$
BEGIN
    NEW.data_aggiornamento := clock_timestamp();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- 1/ UTENTI
CREATE TABLE utente (
    id                     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                   VARCHAR(80)  NOT NULL,
    cognome                VARCHAR(80)  NOT NULL,
    email                  VARCHAR(255) NOT NULL,
    hash_password          VARCHAR(255),
    identificativo_google  VARCHAR(255),
    immagine_profilo       VARCHAR(512),
    ruolo                  VARCHAR(30)  NOT NULL DEFAULT 'GIOCATORE',
    stato                  VARCHAR(20)  NOT NULL DEFAULT 'ATTIVO',
    data_creazione         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_aggiornamento     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_ultimo_accesso    TIMESTAMPTZ,

    CONSTRAINT utente_email_univoca        UNIQUE (email),
    CONSTRAINT utente_google_univoco       UNIQUE (identificativo_google),
    CONSTRAINT utente_email_minuscola      CHECK (email = lower(email)),
    CONSTRAINT utente_email_formato        CHECK (email LIKE '%_@_%.__%'),
    CONSTRAINT utente_ruolo_valido         CHECK (ruolo IN (
                                               'GIOCATORE',
                                               'ADMIN_LOCALE',
                                               'ADMIN_GIOCO',
                                               'ADMIN_PIATTAFORMA')),
    CONSTRAINT utente_stato_valido         CHECK (stato IN ('ATTIVO','SOSPESO','BLOCCATO')),
    -- deve essere presente almeno un metodo di autenticazione
    CONSTRAINT utente_metodo_autenticazione CHECK (
        hash_password IS NOT NULL OR identificativo_google IS NOT NULL)
);

CREATE TRIGGER utente_aggiornamento
    BEFORE UPDATE ON utente
    FOR EACH ROW EXECUTE FUNCTION imposta_data_aggiornamento();

COMMENT ON COLUMN utente.identificativo_google IS
    'Identificativo stabile restituito da Google (claim "sub"), non l''email.';


-- =============================================================================
-- 2. LOCALI
-- =============================================================================
CREATE TABLE locale (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    amministratore_id   BIGINT       NOT NULL REFERENCES utente(id) ON DELETE RESTRICT,
    nome                VARCHAR(150) NOT NULL,
    descrizione         TEXT,
    indirizzo           VARCHAR(255) NOT NULL,
    citta               VARCHAR(100) NOT NULL,
    codice_postale      VARCHAR(10)  NOT NULL,
    provincia           CHAR(2),
    numero_telefono     VARCHAR(30),
    stato_verifica      VARCHAR(20)  NOT NULL DEFAULT 'IN_ATTESA',
    attivo              BOOLEAN      NOT NULL DEFAULT TRUE,
    data_creazione      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_aggiornamento  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT locale_stato_verifica_valido CHECK (stato_verifica IN (
        'IN_ATTESA','APPROVATO','RIFIUTATO','SOSPESO')),
    CONSTRAINT locale_provincia_maiuscola   CHECK (provincia IS NULL OR provincia ~ '^[A-Z]{2}$')
);

CREATE TRIGGER locale_aggiornamento
    BEFORE UPDATE ON locale
    FOR EACH ROW EXECUTE FUNCTION imposta_data_aggiornamento();

COMMENT ON COLUMN locale.stato_verifica IS
    'Esito della revisione da parte della piattaforma. Denormalizzazione voluta
     dell''ultima richiesta_verifica_locale: il backend la mantiene allineata.';
COMMENT ON COLUMN locale.attivo IS
    'Interruttore in mano al gestore del locale, indipendente da stato_verifica.
     Un locale e'' utilizzabile solo se APPROVATO e attivo.';


-- =============================================================================
-- 3. RICHIESTE DI VERIFICA DEL LOCALE
-- =============================================================================
CREATE TABLE richiesta_verifica_locale (
    id                    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locale_id             BIGINT      NOT NULL REFERENCES locale(id) ON DELETE CASCADE,
    richiedente_id        BIGINT      NOT NULL REFERENCES utente(id) ON DELETE RESTRICT,
    stato                 VARCHAR(20) NOT NULL DEFAULT 'IN_ATTESA',
    messaggio_richiedente TEXT,
    revisore_id           BIGINT      REFERENCES utente(id) ON DELETE SET NULL,
    motivo_rifiuto        TEXT,
    data_richiesta        TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_revisione        TIMESTAMPTZ,

    CONSTRAINT richiesta_stato_valido CHECK (stato IN ('IN_ATTESA','APPROVATA','RIFIUTATA')),
    -- finche' la richiesta e' in attesa non esiste ne' revisore ne' data di revisione
    CONSTRAINT richiesta_revisione_coerente CHECK (
        (stato = 'IN_ATTESA' AND revisore_id IS NULL AND data_revisione IS NULL)
     OR (stato <> 'IN_ATTESA' AND revisore_id IS NOT NULL AND data_revisione IS NOT NULL)),
    CONSTRAINT richiesta_motivo_rifiuto CHECK (
        (stato = 'RIFIUTATA' AND motivo_rifiuto IS NOT NULL)
     OR (stato <> 'RIFIUTATA' AND motivo_rifiuto IS NULL)),
    CONSTRAINT richiesta_revisione_non_anteriore CHECK (
        data_revisione IS NULL OR data_revisione >= data_richiesta)
);

-- un locale puo' avere molte richieste nel tempo, ma una sola aperta
CREATE UNIQUE INDEX richiesta_una_sola_in_attesa
    ON richiesta_verifica_locale (locale_id)
    WHERE stato = 'IN_ATTESA';


-- =============================================================================
-- 4. EDGE
-- =============================================================================
CREATE TABLE edge (
    id                        BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locale_id                 BIGINT       NOT NULL REFERENCES locale(id) ON DELETE RESTRICT,
    codice                    VARCHAR(40)  NOT NULL,
    nome                      VARCHAR(100) NOT NULL,
    identificativo_client_mqtt VARCHAR(128) NOT NULL,
    hash_credenziale          VARCHAR(255) NOT NULL,
    stato                     VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    data_ultimo_contatto      TIMESTAMPTZ,
    data_registrazione        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_aggiornamento        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT edge_codice_univoco_nel_locale UNIQUE (locale_id, codice),
    CONSTRAINT edge_client_mqtt_univoco       UNIQUE (identificativo_client_mqtt),
    CONSTRAINT edge_stato_valido              CHECK (stato IN (
        'ONLINE','OFFLINE','DISATTIVATO','ERRORE')),
    -- riferimento composito usato da gioco_installato per garantire che l'edge
    -- associato a un gioco appartenga allo stesso locale del gioco
    CONSTRAINT edge_id_locale UNIQUE (id, locale_id)
);

CREATE TRIGGER edge_aggiornamento
    BEFORE UPDATE ON edge
    FOR EACH ROW EXECUTE FUNCTION imposta_data_aggiornamento();


-- =============================================================================
-- 5. TIPI DI GIOCO
-- =============================================================================
CREATE TABLE tipo_gioco (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    amministratore_id   BIGINT       REFERENCES utente(id) ON DELETE SET NULL,
    codice              VARCHAR(40)  NOT NULL,
    nome                VARCHAR(100) NOT NULL,
    descrizione         TEXT,
    versione_gestore    VARCHAR(20),
    attivo              BOOLEAN      NOT NULL DEFAULT FALSE,
    data_creazione      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_aggiornamento  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT tipo_gioco_codice_univoco UNIQUE (codice),
    CONSTRAINT tipo_gioco_codice_formato CHECK (codice ~ '^[A-Z][A-Z0-9_]*$'),
    -- riferimento composito per la propagazione del codice
    CONSTRAINT tipo_gioco_id_codice      UNIQUE (id, codice)
);

CREATE TRIGGER tipo_gioco_aggiornamento
    BEFORE UPDATE ON tipo_gioco
    FOR EACH ROW EXECUTE FUNCTION imposta_data_aggiornamento();

COMMENT ON COLUMN tipo_gioco.codice IS
    'Codice stabile con cui il backend seleziona l''implementazione Java del
     GestoreGioco. Non contiene il nome completo di una classe.';
COMMENT ON COLUMN tipo_gioco.attivo IS
    'Abilitato dall''amministratore della piattaforma dopo l''implementazione.';


-- =============================================================================
-- 6. TIPI DI EVENTO PREVISTI DA UN TIPO DI GIOCO
-- =============================================================================
CREATE TABLE tipo_evento_gioco (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_gioco_id      BIGINT       NOT NULL,
    codice_tipo_gioco  VARCHAR(40)  NOT NULL,
    codice             VARCHAR(60)  NOT NULL,
    nome               VARCHAR(100) NOT NULL,
    descrizione        TEXT,
    abilitata          BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT tipo_evento_codice_univoco   UNIQUE (tipo_gioco_id, codice),
    CONSTRAINT tipo_evento_codice_formato   CHECK (codice ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT tipo_evento_id_tipo_gioco    UNIQUE (id, codice_tipo_gioco),
    CONSTRAINT tipo_evento_id_codice        UNIQUE (id, codice),
    CONSTRAINT tipo_evento_tipo_gioco_fk    FOREIGN KEY (tipo_gioco_id, codice_tipo_gioco)
        REFERENCES tipo_gioco(id, codice) ON UPDATE CASCADE ON DELETE CASCADE
);

COMMENT ON COLUMN tipo_evento_gioco.abilitata IS
    'Deciso dall''amministratore del gioco: se falso l''evento non viene
     registrato dal backend. Non incide sugli eventi gia'' memorizzati.';


-- =============================================================================
-- 7. TIPI DI SENSORE PREVISTI DA UN TIPO DI GIOCO
-- =============================================================================
CREATE TABLE tipo_sensore (
    id                      BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_gioco_id           BIGINT       NOT NULL,
    codice_tipo_gioco       VARCHAR(40)  NOT NULL,
    codice                  VARCHAR(60)  NOT NULL,
    nome                    VARCHAR(100) NOT NULL,
    descrizione             TEXT,
    tipo_evento_prodotto_id BIGINT       NOT NULL,
    obbligatorio            BOOLEAN      NOT NULL DEFAULT FALSE,
    attivo                  BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT tipo_sensore_codice_univoco UNIQUE (tipo_gioco_id, codice),
    CONSTRAINT tipo_sensore_codice_formato CHECK (codice ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT tipo_sensore_id_tipo_gioco  UNIQUE (id, codice_tipo_gioco),
    CONSTRAINT tipo_sensore_tipo_gioco_fk  FOREIGN KEY (tipo_gioco_id, codice_tipo_gioco)
        REFERENCES tipo_gioco(id, codice) ON UPDATE CASCADE ON DELETE CASCADE,
    -- l'evento prodotto deve appartenere allo stesso tipo di gioco del sensore
    CONSTRAINT tipo_sensore_evento_fk      FOREIGN KEY (tipo_evento_prodotto_id, codice_tipo_gioco)
        REFERENCES tipo_evento_gioco(id, codice_tipo_gioco) ON UPDATE CASCADE
);


-- =============================================================================
-- 8. GIOCHI INSTALLATI
-- =============================================================================
CREATE TABLE gioco_installato (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_gioco_id       BIGINT       NOT NULL,
    codice_tipo_gioco   VARCHAR(40)  NOT NULL,
    locale_id           BIGINT       NOT NULL REFERENCES locale(id) ON DELETE RESTRICT,
    edge_id             BIGINT,
    codice_locale       VARCHAR(40)  NOT NULL,
    codice_accesso      CHAR(6)      NOT NULL,
    nome_visualizzato   VARCHAR(120) NOT NULL,
    descrizione         TEXT,
    stato               VARCHAR(20)  NOT NULL DEFAULT 'DISATTIVATO',
    data_installazione  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_aggiornamento  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT gioco_codice_univoco_nel_locale UNIQUE (locale_id, codice_locale),
    -- globalmente univoco: il giocatore lo digita senza indicare il locale
    CONSTRAINT gioco_codice_accesso_univoco    UNIQUE (codice_accesso),
    CONSTRAINT gioco_codice_accesso_formato    CHECK (codice_accesso ~ '^[A-Z0-9]{6}$'),
    CONSTRAINT gioco_stato_valido              CHECK (stato IN (
        'ATTIVO','DISATTIVATO','MANUTENZIONE','OFFLINE')),
    CONSTRAINT gioco_id_tipo_gioco             UNIQUE (id, codice_tipo_gioco),
    CONSTRAINT gioco_tipo_gioco_fk             FOREIGN KEY (tipo_gioco_id, codice_tipo_gioco)
        REFERENCES tipo_gioco(id, codice) ON UPDATE CASCADE,
    -- l'edge associato deve appartenere allo stesso locale del gioco.
    -- edge_id e' facoltativo: MATCH SIMPLE non applica il vincolo se e' NULL.
    CONSTRAINT gioco_edge_stesso_locale_fk     FOREIGN KEY (edge_id, locale_id)
        REFERENCES edge(id, locale_id) ON DELETE SET NULL (edge_id)
);

CREATE TRIGGER gioco_installato_aggiornamento
    BEFORE UPDATE ON gioco_installato
    FOR EACH ROW EXECUTE FUNCTION imposta_data_aggiornamento();

COMMENT ON COLUMN gioco_installato.codice_accesso IS
    'Codice breve stampato sulla freccettiera, usato dal giocatore per
     identificarla dal frontend. Non esiste un codice per partita.';


-- =============================================================================
-- 9. SENSORI
-- =============================================================================
CREATE TABLE sensore (
    id                   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    gioco_installato_id  BIGINT       NOT NULL,
    codice_tipo_gioco    VARCHAR(40)  NOT NULL,
    tipo_sensore_id      BIGINT       NOT NULL,
    codice               VARCHAR(40)  NOT NULL,
    nome                 VARCHAR(100) NOT NULL,
    simulato             BOOLEAN      NOT NULL DEFAULT FALSE,
    stato                VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    data_ultimo_contatto TIMESTAMPTZ,
    data_registrazione   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT sensore_codice_univoco_nel_gioco UNIQUE (gioco_installato_id, codice),
    CONSTRAINT sensore_stato_valido             CHECK (stato IN (
        'ONLINE','OFFLINE','DISATTIVATO','ERRORE')),
    CONSTRAINT sensore_id_gioco                 UNIQUE (id, gioco_installato_id),
    CONSTRAINT sensore_gioco_fk                 FOREIGN KEY (gioco_installato_id, codice_tipo_gioco)
        REFERENCES gioco_installato(id, codice_tipo_gioco) ON UPDATE CASCADE ON DELETE CASCADE,
    -- il tipo di sensore deve essere previsto dallo stesso tipo di gioco
    CONSTRAINT sensore_tipo_fk                  FOREIGN KEY (tipo_sensore_id, codice_tipo_gioco)
        REFERENCES tipo_sensore(id, codice_tipo_gioco) ON UPDATE CASCADE
);


-- =============================================================================
-- 10. MODALITA DELLE FRECCETTE
-- =============================================================================
CREATE TABLE modalita_freccette (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codice              VARCHAR(10)  NOT NULL,
    nome                VARCHAR(60)  NOT NULL,
    punteggio_iniziale  INTEGER      NOT NULL,
    chiusura_doppia     BOOLEAN      NOT NULL DEFAULT FALSE,
    frecce_per_turno    SMALLINT     NOT NULL DEFAULT 3,
    attiva              BOOLEAN      NOT NULL DEFAULT TRUE,
    data_creazione      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_aggiornamento  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT modalita_codice_univoco     UNIQUE (codice),
    CONSTRAINT modalita_punteggio_positivo CHECK (punteggio_iniziale > 0),
    CONSTRAINT modalita_frecce_valide      CHECK (frecce_per_turno BETWEEN 1 AND 6)
);

CREATE TRIGGER modalita_freccette_aggiornamento
    BEFORE UPDATE ON modalita_freccette
    FOR EACH ROW EXECUTE FUNCTION imposta_data_aggiornamento();


-- =============================================================================
-- 11. CONFIGURAZIONE PREDEFINITA DI UNA FRECCETTIERA
-- =============================================================================
CREATE TABLE configurazione_gioco_freccette (
    gioco_installato_id         BIGINT      PRIMARY KEY,
    codice_tipo_gioco           VARCHAR(40) NOT NULL DEFAULT 'FRECCETTE',
    modalita_predefinita_id     BIGINT      NOT NULL
                                REFERENCES modalita_freccette(id) ON DELETE RESTRICT,
    chiusura_doppia_predefinita BOOLEAN     NOT NULL DEFAULT FALSE,
    data_aggiornamento          TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- la configurazione puo' esistere solo per giochi di tipo FRECCETTE
    CONSTRAINT configurazione_solo_freccette CHECK (codice_tipo_gioco = 'FRECCETTE'),
    CONSTRAINT configurazione_gioco_fk       FOREIGN KEY (gioco_installato_id, codice_tipo_gioco)
        REFERENCES gioco_installato(id, codice_tipo_gioco) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TRIGGER configurazione_gioco_freccette_aggiornamento
    BEFORE UPDATE ON configurazione_gioco_freccette
    FOR EACH ROW EXECUTE FUNCTION imposta_data_aggiornamento();


-- =============================================================================
-- 12. TORNEI
-- =============================================================================
CREATE TABLE torneo (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_gioco_id       BIGINT       NOT NULL,
    codice_tipo_gioco   VARCHAR(40)  NOT NULL,
    creatore_id         BIGINT       NOT NULL REFERENCES utente(id) ON DELETE RESTRICT,
    nome                VARCHAR(150) NOT NULL,
    descrizione         TEXT,
    tipo_partecipazione VARCHAR(15)  NOT NULL,
    stato               VARCHAR(15)  NOT NULL DEFAULT 'PROGRAMMATO',
    data_inizio         TIMESTAMPTZ  NOT NULL,
    data_fine           TIMESTAMPTZ,
    data_creazione      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT torneo_partecipazione_valida CHECK (tipo_partecipazione IN ('INDIVIDUALE','SQUADRE')),
    CONSTRAINT torneo_stato_valido          CHECK (stato IN (
        'PROGRAMMATO','IN_CORSO','TERMINATO','ANNULLATO')),
    CONSTRAINT torneo_periodo_coerente      CHECK (data_fine IS NULL OR data_fine >= data_inizio),
    CONSTRAINT torneo_tipo_gioco_fk         FOREIGN KEY (tipo_gioco_id, codice_tipo_gioco)
        REFERENCES tipo_gioco(id, codice) ON UPDATE CASCADE,
    -- riferimento composito: obbliga le partite del torneo a usare lo stesso
    -- tipo di gioco e la stessa modalita' di partecipazione
    CONSTRAINT torneo_id_tipo_gioco_partecipazione UNIQUE (id, codice_tipo_gioco, tipo_partecipazione)
);


-- =============================================================================
-- 13. ASSOCIAZIONE TORNEI - LOCALI
-- =============================================================================
CREATE TABLE torneo_locale (
    torneo_id                  BIGINT      NOT NULL REFERENCES torneo(id) ON DELETE CASCADE,
    locale_id                  BIGINT      NOT NULL REFERENCES locale(id) ON DELETE RESTRICT,
    data_associazione          TIMESTAMPTZ NOT NULL DEFAULT now(),
    organizzatore_principale_id BIGINT     REFERENCES utente(id) ON DELETE SET NULL,

    CONSTRAINT torneo_locale_pk PRIMARY KEY (torneo_id, locale_id)
);


-- =============================================================================
-- 14. SQUADRE
-- =============================================================================
CREATE TABLE squadra (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    torneo_id      BIGINT       REFERENCES torneo(id) ON DELETE SET NULL,
    nome           VARCHAR(100) NOT NULL,
    capitano_id    BIGINT       REFERENCES utente(id) ON DELETE SET NULL,
    data_creazione TIMESTAMPTZ  NOT NULL DEFAULT now(),
    attiva         BOOLEAN      NOT NULL DEFAULT TRUE
);

COMMENT ON COLUMN squadra.torneo_id IS
    'Se valorizzato, la squadra e'' nata per quel torneo; se NULL e'' una
     squadra permanente riutilizzabile in partite e tornei diversi.';

-- nome univoco all'interno del torneo, e univoco tra le squadre permanenti
CREATE UNIQUE INDEX squadra_nome_univoco_nel_torneo
    ON squadra (torneo_id, lower(nome))
    WHERE torneo_id IS NOT NULL;

CREATE UNIQUE INDEX squadra_nome_univoco_permanente
    ON squadra (lower(nome))
    WHERE torneo_id IS NULL;


-- =============================================================================
-- 15. MEMBRI DELLE SQUADRE
-- =============================================================================
CREATE TABLE membro_squadra (
    squadra_id         BIGINT      NOT NULL REFERENCES squadra(id) ON DELETE CASCADE,
    utente_id          BIGINT      NOT NULL REFERENCES utente(id) ON DELETE RESTRICT,
    data_ingresso      TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_uscita        TIMESTAMPTZ,
    ruolo_nella_squadra VARCHAR(60),

    CONSTRAINT membro_squadra_pk PRIMARY KEY (squadra_id, utente_id, data_ingresso),
    CONSTRAINT membro_periodo_coerente CHECK (data_uscita IS NULL OR data_uscita > data_ingresso)
);

-- lo stesso utente non puo' essere due volte contemporaneamente nella squadra
CREATE UNIQUE INDEX membro_squadra_appartenenza_attiva
    ON membro_squadra (squadra_id, utente_id)
    WHERE data_uscita IS NULL;


-- =============================================================================
-- 16. PARTITE
-- =============================================================================
CREATE TABLE partita (
    id                      BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    identificativo_sorgente VARCHAR(64) NOT NULL,
    gioco_installato_id     BIGINT      NOT NULL,
    codice_tipo_gioco       VARCHAR(40) NOT NULL,
    edge_utilizzato_id      BIGINT      REFERENCES edge(id) ON DELETE SET NULL,
    torneo_id               BIGINT,
    creatore_id             BIGINT      REFERENCES utente(id) ON DELETE SET NULL,
    tipo_partecipazione     VARCHAR(15) NOT NULL,
    stato                   VARCHAR(15) NOT NULL DEFAULT 'CREATA',
    data_creazione          TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_inizio             TIMESTAMPTZ,
    data_fine               TIMESTAMPTZ,
    motivo_interruzione     TEXT,

    -- idempotenza della sincronizzazione: l'edge assegna l'identificativo,
    -- un reinvio dopo un periodo offline non crea una seconda partita
    CONSTRAINT partita_sorgente_univoca UNIQUE (gioco_installato_id, identificativo_sorgente),

    CONSTRAINT partita_partecipazione_valida CHECK (tipo_partecipazione IN ('INDIVIDUALE','SQUADRE')),
    CONSTRAINT partita_stato_valido          CHECK (stato IN (
        'CREATA','IN_CORSO','SOSPESA','TERMINATA','ANNULLATA')),
    -- una partita non ancora avviata puo' solo essere CREATA o ANNULLATA
    CONSTRAINT partita_inizio_coerente CHECK (
        data_inizio IS NOT NULL OR stato IN ('CREATA','ANNULLATA')),
    CONSTRAINT partita_fine_coerente CHECK (
        (data_fine IS NULL AND stato NOT IN ('TERMINATA','ANNULLATA'))
     OR (data_fine IS NOT NULL AND stato IN ('TERMINATA','ANNULLATA'))),
    CONSTRAINT partita_periodo_coerente CHECK (
        data_fine IS NULL OR data_inizio IS NULL OR data_fine >= data_inizio),
    CONSTRAINT partita_motivo_interruzione CHECK (
        motivo_interruzione IS NULL OR stato IN ('ANNULLATA','SOSPESA')),

    CONSTRAINT partita_gioco_fk FOREIGN KEY (gioco_installato_id, codice_tipo_gioco)
        REFERENCES gioco_installato(id, codice_tipo_gioco) ON UPDATE CASCADE,
    -- una partita di torneo deve usare lo stesso tipo di gioco e la stessa
    -- modalita' di partecipazione del torneo
    CONSTRAINT partita_torneo_fk FOREIGN KEY (torneo_id, codice_tipo_gioco, tipo_partecipazione)
        REFERENCES torneo(id, codice_tipo_gioco, tipo_partecipazione) ON UPDATE CASCADE,

    CONSTRAINT partita_id_tipo_gioco       UNIQUE (id, codice_tipo_gioco),
    CONSTRAINT partita_id_partecipazione   UNIQUE (id, tipo_partecipazione),
    CONSTRAINT partita_id_gioco_tipo       UNIQUE (id, gioco_installato_id, codice_tipo_gioco)
);

COMMENT ON COLUMN partita.identificativo_sorgente IS
    'UUID generato dall''edge alla creazione della partita, anche offline.
     Garantisce l''idempotenza della sincronizzazione.';
COMMENT ON COLUMN partita.edge_utilizzato_id IS
    'Fotografia storica: l''associazione edge-gioco puo'' cambiare nel tempo,
     quindi il valore non e'' derivabile da gioco_installato.';


-- =============================================================================
-- 17. CONFIGURAZIONE EFFETTIVA DI UNA PARTITA DI FRECCETTE
-- =============================================================================
CREATE TABLE configurazione_partita_freccette (
    partita_id            BIGINT      PRIMARY KEY,
    codice_tipo_gioco     VARCHAR(40) NOT NULL DEFAULT 'FRECCETTE',
    modalita_freccette_id BIGINT      NOT NULL
                          REFERENCES modalita_freccette(id) ON DELETE RESTRICT,
    punteggio_iniziale    INTEGER     NOT NULL,
    chiusura_doppia       BOOLEAN     NOT NULL,
    frecce_per_turno      SMALLINT    NOT NULL,

    CONSTRAINT configurazione_partita_solo_freccette CHECK (codice_tipo_gioco = 'FRECCETTE'),
    CONSTRAINT configurazione_partita_punteggio      CHECK (punteggio_iniziale > 0),
    CONSTRAINT configurazione_partita_frecce         CHECK (frecce_per_turno BETWEEN 1 AND 6),
    CONSTRAINT configurazione_partita_fk FOREIGN KEY (partita_id, codice_tipo_gioco)
        REFERENCES partita(id, codice_tipo_gioco) ON UPDATE CASCADE ON DELETE CASCADE
);

COMMENT ON TABLE configurazione_partita_freccette IS
    'Copia dei valori realmente usati dalla partita: una modifica successiva
     della modalita'' non altera lo storico.';


-- =============================================================================
-- 18. PARTECIPAZIONI ALLE PARTITE
-- =============================================================================
CREATE TABLE partecipazione_partita (
    id                  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    partita_id          BIGINT      NOT NULL,
    tipo_partecipazione VARCHAR(15) NOT NULL,
    utente_id           BIGINT      REFERENCES utente(id) ON DELETE RESTRICT,
    squadra_id          BIGINT      REFERENCES squadra(id) ON DELETE RESTRICT,
    nome_ospite         VARCHAR(60),
    ordine_turno        SMALLINT    NOT NULL,
    colore              CHAR(7),
    punteggio_finale    INTEGER,
    posizione_finale    SMALLINT,
    stato               VARCHAR(15) NOT NULL DEFAULT 'ATTIVO',
    data_partecipazione TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- esattamente uno tra utente, squadra e ospite
    CONSTRAINT partecipazione_soggetto_unico CHECK (
        num_nonnulls(utente_id, squadra_id, nome_ospite) = 1),
    -- nelle partite a squadre il soggetto e' sempre la squadra, e viceversa
    CONSTRAINT partecipazione_coerente_con_partita CHECK (
        (tipo_partecipazione = 'SQUADRE'     AND squadra_id IS NOT NULL)
     OR (tipo_partecipazione = 'INDIVIDUALE' AND squadra_id IS NULL)),

    CONSTRAINT partecipazione_ordine_positivo CHECK (ordine_turno > 0),
    CONSTRAINT partecipazione_posizione_positiva CHECK (
        posizione_finale IS NULL OR posizione_finale > 0),
    CONSTRAINT partecipazione_colore_formato CHECK (
        colore IS NULL OR colore ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT partecipazione_stato_valido CHECK (stato IN (
        'ATTIVO','RITIRATO','ESPULSO','TERMINATO')),

    CONSTRAINT partecipazione_ordine_univoco UNIQUE (partita_id, ordine_turno),
    CONSTRAINT partecipazione_id_partita     UNIQUE (id, partita_id),
    CONSTRAINT partecipazione_partita_fk FOREIGN KEY (partita_id, tipo_partecipazione)
        REFERENCES partita(id, tipo_partecipazione) ON UPDATE CASCADE ON DELETE CASCADE
);

-- lo stesso utente o la stessa squadra non possono comparire due volte
CREATE UNIQUE INDEX partecipazione_utente_univoco
    ON partecipazione_partita (partita_id, utente_id) WHERE utente_id IS NOT NULL;

CREATE UNIQUE INDEX partecipazione_squadra_univoca
    ON partecipazione_partita (partita_id, squadra_id) WHERE squadra_id IS NOT NULL;

-- un solo primo classificato per partita
CREATE UNIQUE INDEX partecipazione_vincitore_unico
    ON partecipazione_partita (partita_id) WHERE posizione_finale = 1;

COMMENT ON COLUMN partecipazione_partita.posizione_finale IS
    'Il vincitore e'' la partecipazione con posizione_finale = 1: non serve
     una colonna booleana separata.';


-- =============================================================================
-- 19. EVENTI DI PARTITA
-- =============================================================================
CREATE TABLE evento_partita (
    id                             BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    partita_id                     BIGINT      NOT NULL,
    gioco_installato_id            BIGINT      NOT NULL,
    codice_tipo_gioco              VARCHAR(40) NOT NULL,
    tipo_evento_id                 BIGINT      NOT NULL,
    codice_tipo_evento             VARCHAR(60) NOT NULL,
    sensore_id                     BIGINT,
    identificativo_evento_sorgente VARCHAR(64) NOT NULL,
    data_evento                    TIMESTAMPTZ NOT NULL,
    data_ricezione                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    dati_originali                 JSONB,
    sincronizzato_da_offline       BOOLEAN     NOT NULL DEFAULT FALSE,

    -- idempotenza: l'identificativo e' univoco per gioco installato, quindi
    -- non dipende dall'unicita' globale dei contatori dei singoli edge
    CONSTRAINT evento_sorgente_univoco UNIQUE (gioco_installato_id, identificativo_evento_sorgente),

    -- l'evento appartiene alla partita e al gioco su cui la partita si svolge
    CONSTRAINT evento_partita_fk FOREIGN KEY (partita_id, gioco_installato_id, codice_tipo_gioco)
        REFERENCES partita(id, gioco_installato_id, codice_tipo_gioco)
        ON UPDATE CASCADE ON DELETE CASCADE,
    -- il tipo di evento deve essere previsto dal tipo di gioco della partita
    CONSTRAINT evento_tipo_fk FOREIGN KEY (tipo_evento_id, codice_tipo_gioco)
        REFERENCES tipo_evento_gioco(id, codice_tipo_gioco) ON UPDATE CASCADE,
    CONSTRAINT evento_tipo_codice_fk FOREIGN KEY (tipo_evento_id, codice_tipo_evento)
        REFERENCES tipo_evento_gioco(id, codice) ON UPDATE CASCADE,
    -- il sensore, se presente, deve appartenere allo stesso gioco installato
    CONSTRAINT evento_sensore_fk FOREIGN KEY (sensore_id, gioco_installato_id)
        REFERENCES sensore(id, gioco_installato_id) ON UPDATE CASCADE,

    CONSTRAINT evento_id_partita_tipo UNIQUE (id, partita_id, codice_tipo_evento)
);

COMMENT ON COLUMN evento_partita.data_evento IS
    'Istante rilevato sull''edge. Puo'' precedere di molto data_ricezione dopo
     una sincronizzazione offline; non e'' vincolato a data_ricezione perche''
     l''orologio dell''edge puo'' non essere sincronizzato.';


-- =============================================================================
-- 20. TIRI DI FRECCETTE
-- =============================================================================
CREATE TABLE tiro_freccette (
    id                       BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evento_partita_id        BIGINT      NOT NULL,
    partita_id               BIGINT      NOT NULL,
    codice_tipo_evento       VARCHAR(60) NOT NULL DEFAULT 'TIRO_RILEVATO',
    partecipazione_id        BIGINT      NOT NULL,
    utente_esecutore_id      BIGINT      REFERENCES utente(id) ON DELETE SET NULL,
    numero_turno             SMALLINT    NOT NULL,
    numero_tiro_nel_turno    SMALLINT    NOT NULL,
    settore                  SMALLINT    NOT NULL,
    moltiplicatore           SMALLINT    NOT NULL,
    punteggio_calcolato      SMALLINT    NOT NULL,
    punteggio_prima_del_tiro INTEGER     NOT NULL,
    punteggio_dopo_il_tiro   INTEGER     NOT NULL,
    valido                   BOOLEAN     NOT NULL DEFAULT TRUE,
    sballato                 BOOLEAN     NOT NULL DEFAULT FALSE,

    -- relazione uno a uno con l'evento generico
    CONSTRAINT tiro_evento_univoco UNIQUE (evento_partita_id),
    CONSTRAINT tiro_solo_su_evento_tiro CHECK (codice_tipo_evento = 'TIRO_RILEVATO'),
    -- l'evento deve essere di tipo TIRO_RILEVATO e appartenere a questa partita
    CONSTRAINT tiro_evento_fk FOREIGN KEY (evento_partita_id, partita_id, codice_tipo_evento)
        REFERENCES evento_partita(id, partita_id, codice_tipo_evento)
        ON UPDATE CASCADE ON DELETE CASCADE,
    -- il partecipante deve appartenere alla stessa partita dell'evento
    CONSTRAINT tiro_partecipazione_fk FOREIGN KEY (partecipazione_id, partita_id)
        REFERENCES partecipazione_partita(id, partita_id) ON UPDATE CASCADE,

    CONSTRAINT tiro_sequenza_univoca UNIQUE (partecipazione_id, numero_turno, numero_tiro_nel_turno),

    CONSTRAINT tiro_turno_positivo CHECK (numero_turno > 0),
    CONSTRAINT tiro_numero_valido  CHECK (numero_tiro_nel_turno BETWEEN 1 AND 6),
    -- settori validi: 0 = fuori bersaglio, 1..20 spicchi, 25 = bull
    CONSTRAINT tiro_settore_valido CHECK (settore BETWEEN 0 AND 20 OR settore = 25),
    CONSTRAINT tiro_moltiplicatore_valido CHECK (moltiplicatore BETWEEN 1 AND 3),
    -- il bull ammette solo singolo (25) e doppio (50): il triplo non esiste
    CONSTRAINT tiro_bull_senza_triplo CHECK (settore <> 25 OR moltiplicatore <= 2),
    -- fuori bersaglio: nessun punto
    CONSTRAINT tiro_fuori_bersaglio CHECK (
        settore <> 0 OR (moltiplicatore = 1 AND punteggio_calcolato = 0)),
    CONSTRAINT tiro_punteggio_coerente CHECK (punteggio_calcolato = settore * moltiplicatore),
    CONSTRAINT tiro_punteggi_non_negativi CHECK (
        punteggio_prima_del_tiro >= 0 AND punteggio_dopo_il_tiro >= 0),
    -- un tiro non valido o sballato non modifica il punteggio del giocatore
    CONSTRAINT tiro_effetto_sul_punteggio CHECK (
        CASE WHEN valido AND NOT sballato
             THEN punteggio_dopo_il_tiro = punteggio_prima_del_tiro - punteggio_calcolato
             ELSE punteggio_dopo_il_tiro = punteggio_prima_del_tiro
        END)
);

COMMENT ON COLUMN tiro_freccette.partecipazione_id IS
    'Nelle partite a squadre punta alla squadra, come richiesto dalla specifica.';
COMMENT ON COLUMN tiro_freccette.utente_esecutore_id IS
    'Facoltativo: consente le statistiche individuali anche nelle partite a
     squadre, senza spostare il risultato dalla squadra al singolo.';