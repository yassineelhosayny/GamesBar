-- Suite di verifica dei vincoli: ogni comando elencato DEVE essere respinto.
BEGIN;

CREATE OR REPLACE FUNCTION deve_fallire(descrizione TEXT, comando TEXT) RETURNS TEXT AS $$
BEGIN
    BEGIN
        EXECUTE comando;
        RAISE EXCEPTION 'non_bloccato';
    EXCEPTION
        WHEN check_violation OR foreign_key_violation OR unique_violation
             OR not_null_violation OR exclusion_violation THEN
            RETURN 'OK       ' || descrizione;
        WHEN raise_exception THEN
            RETURN '>>> FALLITO: ' || descrizione;
    END;
END;
$$ LANGUAGE plpgsql;

-- =========================== SCENARIO DI PROVA ==============================
INSERT INTO utente (nome, cognome, email, hash_password, identificativo_google, ruolo) VALUES
  ('Anna','Bianchi','anna@example.com','$2a$10$x',NULL,'AMMINISTRATORE_PIATTAFORMA'),
  ('Luca','Verdi','luca@example.com','$2a$10$x',NULL,'AMMINISTRATORE_LOCALE'),
  -- utente registrato solo con Google: nessuna password
  ('Sara','Neri','sara@example.com',NULL,'110248495','GIOCATORE'),
  -- utente con entrambi i metodi di autenticazione
  ('Marco','Gialli','marco@example.com','$2a$10$x','110248496','GIOCATORE');

INSERT INTO locale (amministratore_id, nome, indirizzo, citta, codice_postale, provincia, stato_verifica)
SELECT id,'Bar Centrale','Via Roma 1','Vercelli','13100','VC','APPROVATO' FROM utente WHERE email='luca@example.com';
INSERT INTO locale (amministratore_id, nome, indirizzo, citta, codice_postale, stato_verifica)
SELECT id,'Bar Sport','Via Milano 9','Vercelli','13100','APPROVATO' FROM utente WHERE email='luca@example.com';

INSERT INTO richiesta_verifica_locale (locale_id, richiedente_id)
SELECT l.id, l.amministratore_id FROM locale l WHERE l.nome='Bar Centrale';

INSERT INTO edge (locale_id, codice, nome, identificativo_client_mqtt, hash_credenziale, stato)
SELECT id,'EDGE-1','Edge principale','edge-bar-centrale-1','$2a$10$y','ONLINE' FROM locale WHERE nome='Bar Centrale';
INSERT INTO edge (locale_id, codice, nome, identificativo_client_mqtt, hash_credenziale)
SELECT id,'EDGE-1','Edge sport','edge-bar-sport-1','$2a$10$y' FROM locale WHERE nome='Bar Sport';

UPDATE tipo_gioco SET attivo = TRUE WHERE codice='FRECCETTE';
INSERT INTO tipo_gioco (codice, nome, attivo) VALUES ('CALCIOBALILLA','Calciobalilla',TRUE);

INSERT INTO gioco_installato (tipo_gioco_id, codice_tipo_gioco, locale_id, edge_id,
                              codice_locale, codice_accesso, nome_visualizzato, stato)
SELECT tg.id, tg.codice, l.id, e.id, 'FRE-01','FREC01','Freccettiera 1','ATTIVO'
FROM tipo_gioco tg, locale l, edge e
WHERE tg.codice='FRECCETTE' AND l.nome='Bar Centrale' AND e.identificativo_client_mqtt='edge-bar-centrale-1';

INSERT INTO gioco_installato (tipo_gioco_id, codice_tipo_gioco, locale_id, codice_locale,
                              codice_accesso, nome_visualizzato, stato)
SELECT tg.id, tg.codice, l.id, 'CAL-01','CALC01','Calciobalilla verde','ATTIVO'
FROM tipo_gioco tg, locale l WHERE tg.codice='CALCIOBALILLA' AND l.nome='Bar Centrale';

INSERT INTO sensore (gioco_installato_id, codice_tipo_gioco, tipo_sensore_id, codice, nome, simulato, stato)
SELECT gi.id, gi.codice_tipo_gioco, ts.id, 'S1','Rilevatore simulato', TRUE, 'ONLINE'
FROM gioco_installato gi, tipo_sensore ts
WHERE gi.codice_accesso='FREC01' AND ts.codice='RILEVATORE_TIRO';

INSERT INTO configurazione_gioco_freccette (gioco_installato_id, modalita_predefinita_id, chiusura_doppia_predefinita)
SELECT gi.id, m.id, TRUE FROM gioco_installato gi, modalita_freccette m
WHERE gi.codice_accesso='FREC01' AND m.codice='501';

INSERT INTO partita (identificativo_sorgente, gioco_installato_id, codice_tipo_gioco,
                     edge_utilizzato_id, tipo_partecipazione, stato, data_inizio)
SELECT '3f9a-0001', gi.id, gi.codice_tipo_gioco, gi.edge_id, 'INDIVIDUALE','IN_CORSO', now()
FROM gioco_installato gi WHERE gi.codice_accesso='FREC01';

INSERT INTO configurazione_partita_freccette (partita_id, modalita_freccette_id, punteggio_iniziale, chiusura_doppia, frecce_per_turno)
SELECT p.id, m.id, m.punteggio_iniziale, TRUE, 3
FROM partita p, modalita_freccette m WHERE p.identificativo_sorgente='3f9a-0001' AND m.codice='501';

INSERT INTO partecipazione_partita (partita_id, tipo_partecipazione, utente_id, ordine_turno)
SELECT p.id,'INDIVIDUALE', u.id, 1 FROM partita p, utente u
WHERE p.identificativo_sorgente='3f9a-0001' AND u.email='sara@example.com';
INSERT INTO partecipazione_partita (partita_id, tipo_partecipazione, nome_ospite, ordine_turno)
SELECT p.id,'INDIVIDUALE','Ospite Giorgio', 2 FROM partita p WHERE p.identificativo_sorgente='3f9a-0001';

INSERT INTO evento_partita (partita_id, gioco_installato_id, codice_tipo_gioco, tipo_evento_id,
                            codice_tipo_evento, sensore_id, identificativo_evento_sorgente, data_evento)
SELECT p.id, p.gioco_installato_id, p.codice_tipo_gioco, te.id, te.codice, s.id, 'evt-0001', now()
FROM partita p, tipo_evento_gioco te, sensore s
WHERE p.identificativo_sorgente='3f9a-0001' AND te.codice='TIRO_RILEVATO' AND s.codice='S1';

INSERT INTO tiro_freccette (evento_partita_id, partita_id, partecipazione_id, numero_turno,
                            numero_tiro_nel_turno, settore, moltiplicatore, punteggio_calcolato,
                            punteggio_prima_del_tiro, punteggio_dopo_il_tiro)
SELECT e.id, e.partita_id, pp.id, 1, 1, 20, 3, 60, 501, 441
FROM evento_partita e, partecipazione_partita pp
WHERE e.identificativo_evento_sorgente='evt-0001' AND pp.ordine_turno=1 AND pp.partita_id=e.partita_id;

-- torneo a squadre
INSERT INTO torneo (tipo_gioco_id, codice_tipo_gioco, creatore_id, nome, tipo_partecipazione, data_inizio)
SELECT tg.id, tg.codice, u.id, 'Coppa Vercelli','SQUADRE', now()
FROM tipo_gioco tg, utente u WHERE tg.codice='FRECCETTE' AND u.email='anna@example.com';
INSERT INTO torneo_locale (torneo_id, locale_id)
SELECT t.id, l.id FROM torneo t, locale l WHERE t.nome='Coppa Vercelli';
INSERT INTO squadra (torneo_id, nome) SELECT id,'I Bomber' FROM torneo WHERE nome='Coppa Vercelli';
INSERT INTO membro_squadra (squadra_id, utente_id)
SELECT s.id, u.id FROM squadra s, utente u WHERE s.nome='I Bomber' AND u.email='sara@example.com';

\echo '=== SCENARIO INSERITO CORRETTAMENTE ==='

-- ======================= VERIFICHE DI VIOLAZIONE ============================
\echo ''
\echo '--- Utenti e autenticazione'
SELECT deve_fallire('utente senza password ne'' identificativo Google',
 $$INSERT INTO utente (nome,cognome,email,ruolo) VALUES ('X','Y','x@example.com','GIOCATORE')$$);
SELECT deve_fallire('email non normalizzata in minuscolo',
 $$INSERT INTO utente (nome,cognome,email,hash_password) VALUES ('X','Y','Maiuscola@Example.com','h')$$);
SELECT deve_fallire('ruolo inesistente',
 $$INSERT INTO utente (nome,cognome,email,hash_password,ruolo) VALUES ('X','Y','r@example.com','h','SUPERADMIN')$$);

\echo ''
\echo '--- Locali ed edge'
SELECT deve_fallire('seconda richiesta di verifica in attesa per lo stesso locale',
 $$INSERT INTO richiesta_verifica_locale (locale_id,richiedente_id)
   SELECT l.id,l.amministratore_id FROM locale l WHERE l.nome='Bar Centrale'$$);
SELECT deve_fallire('richiesta rifiutata senza motivo di rifiuto',
 $$INSERT INTO richiesta_verifica_locale (locale_id,richiedente_id,stato,revisore_id,data_revisione)
   SELECT l.id,l.amministratore_id,'RIFIUTATA',l.amministratore_id,now() FROM locale l WHERE l.nome='Bar Sport'$$);
SELECT deve_fallire('richiesta in attesa con revisore gia'' valorizzato',
 $$INSERT INTO richiesta_verifica_locale (locale_id,richiedente_id,revisore_id)
   SELECT l.id,l.amministratore_id,l.amministratore_id FROM locale l WHERE l.nome='Bar Sport'$$);
SELECT deve_fallire('gioco associato a un edge di un altro locale',
 $$INSERT INTO gioco_installato (tipo_gioco_id,codice_tipo_gioco,locale_id,edge_id,codice_locale,codice_accesso,nome_visualizzato)
   SELECT tg.id,tg.codice,l.id,e.id,'FRE-99','FREC99','Freccettiera intrusa'
   FROM tipo_gioco tg, locale l, edge e
   WHERE tg.codice='FRECCETTE' AND l.nome='Bar Centrale' AND e.identificativo_client_mqtt='edge-bar-sport-1'$$);
SELECT deve_fallire('codice di accesso duplicato tra locali diversi',
 $$INSERT INTO gioco_installato (tipo_gioco_id,codice_tipo_gioco,locale_id,codice_locale,codice_accesso,nome_visualizzato)
   SELECT tg.id,tg.codice,l.id,'FRE-01','FREC01','Doppione'
   FROM tipo_gioco tg, locale l WHERE tg.codice='FRECCETTE' AND l.nome='Bar Sport'$$);

\echo ''
\echo '--- Separazione fra dati generici e dati specifici delle freccette'
SELECT deve_fallire('configurazione freccette su un calciobalilla',
 $$INSERT INTO configurazione_gioco_freccette (gioco_installato_id,modalita_predefinita_id,chiusura_doppia_predefinita)
   SELECT gi.id,m.id,FALSE FROM gioco_installato gi, modalita_freccette m
   WHERE gi.codice_accesso='CALC01' AND m.codice='501'$$);
SELECT deve_fallire('sensore di tipo freccette su un calciobalilla',
 $$INSERT INTO sensore (gioco_installato_id,codice_tipo_gioco,tipo_sensore_id,codice,nome)
   SELECT gi.id,gi.codice_tipo_gioco,ts.id,'S9','Sensore sbagliato'
   FROM gioco_installato gi, tipo_sensore ts
   WHERE gi.codice_accesso='CALC01' AND ts.codice='RILEVATORE_TIRO'$$);

\echo ''
\echo '--- Sincronizzazione offline e idempotenza'
SELECT deve_fallire('reinvio della stessa partita creata offline',
 $$INSERT INTO partita (identificativo_sorgente,gioco_installato_id,codice_tipo_gioco,tipo_partecipazione,stato,data_inizio)
   SELECT '3f9a-0001',gi.id,gi.codice_tipo_gioco,'INDIVIDUALE','IN_CORSO',now()
   FROM gioco_installato gi WHERE gi.codice_accesso='FREC01'$$);
SELECT deve_fallire('reinvio dello stesso evento dopo la riconnessione',
 $$INSERT INTO evento_partita (partita_id,gioco_installato_id,codice_tipo_gioco,tipo_evento_id,codice_tipo_evento,identificativo_evento_sorgente,data_evento)
   SELECT p.id,p.gioco_installato_id,p.codice_tipo_gioco,te.id,te.codice,'evt-0001',now()
   FROM partita p, tipo_evento_gioco te
   WHERE p.identificativo_sorgente='3f9a-0001' AND te.codice='TIRO_RILEVATO'$$);
SELECT deve_fallire('evento con sensore appartenente a un altro gioco installato',
 $$INSERT INTO evento_partita (partita_id,gioco_installato_id,codice_tipo_gioco,tipo_evento_id,codice_tipo_evento,sensore_id,identificativo_evento_sorgente,data_evento)
   SELECT p.id,p.gioco_installato_id,p.codice_tipo_gioco,te.id,te.codice,99999,'evt-0009',now()
   FROM partita p, tipo_evento_gioco te
   WHERE p.identificativo_sorgente='3f9a-0001' AND te.codice='TIRO_RILEVATO'$$);

\echo ''
\echo '--- Partite, partecipazioni e squadre'
SELECT deve_fallire('partecipazione con utente e squadra insieme',
 $$INSERT INTO partecipazione_partita (partita_id,tipo_partecipazione,utente_id,squadra_id,ordine_turno)
   SELECT p.id,'INDIVIDUALE',u.id,s.id,7 FROM partita p, utente u, squadra s
   WHERE p.identificativo_sorgente='3f9a-0001' AND u.email='marco@example.com' AND s.nome='I Bomber'$$);
SELECT deve_fallire('squadra iscritta a una partita individuale',
 $$INSERT INTO partecipazione_partita (partita_id,tipo_partecipazione,squadra_id,ordine_turno)
   SELECT p.id,'INDIVIDUALE',s.id,8 FROM partita p, squadra s
   WHERE p.identificativo_sorgente='3f9a-0001' AND s.nome='I Bomber'$$);
SELECT deve_fallire('tipo di partecipazione diverso da quello della partita',
 $$INSERT INTO partecipazione_partita (partita_id,tipo_partecipazione,squadra_id,ordine_turno)
   SELECT p.id,'SQUADRE',s.id,9 FROM partita p, squadra s
   WHERE p.identificativo_sorgente='3f9a-0001' AND s.nome='I Bomber'$$);
SELECT deve_fallire('stesso giocatore due volte nella stessa partita',
 $$INSERT INTO partecipazione_partita (partita_id,tipo_partecipazione,utente_id,ordine_turno)
   SELECT p.id,'INDIVIDUALE',u.id,10 FROM partita p, utente u
   WHERE p.identificativo_sorgente='3f9a-0001' AND u.email='sara@example.com'$$);
SELECT deve_fallire('ordine di turno duplicato',
 $$INSERT INTO partecipazione_partita (partita_id,tipo_partecipazione,utente_id,ordine_turno)
   SELECT p.id,'INDIVIDUALE',u.id,1 FROM partita p, utente u
   WHERE p.identificativo_sorgente='3f9a-0001' AND u.email='marco@example.com'$$);
SELECT deve_fallire('partita individuale dentro un torneo a squadre',
 $$INSERT INTO partita (identificativo_sorgente,gioco_installato_id,codice_tipo_gioco,torneo_id,tipo_partecipazione,stato)
   SELECT '3f9a-0002',gi.id,gi.codice_tipo_gioco,t.id,'INDIVIDUALE','CREATA'
   FROM gioco_installato gi, torneo t WHERE gi.codice_accesso='FREC01' AND t.nome='Coppa Vercelli'$$);
SELECT deve_fallire('partita di calciobalilla dentro un torneo di freccette',
 $$INSERT INTO partita (identificativo_sorgente,gioco_installato_id,codice_tipo_gioco,torneo_id,tipo_partecipazione,stato)
   SELECT '3f9a-0003',gi.id,gi.codice_tipo_gioco,t.id,'SQUADRE','CREATA'
   FROM gioco_installato gi, torneo t WHERE gi.codice_accesso='CALC01' AND t.nome='Coppa Vercelli'$$);
SELECT deve_fallire('partita terminata senza data di fine',
 $$UPDATE partita SET stato='TERMINATA' WHERE identificativo_sorgente='3f9a-0001'$$);
SELECT deve_fallire('stesso utente due volte attivo nella stessa squadra',
 $$INSERT INTO membro_squadra (squadra_id,utente_id)
   SELECT s.id,u.id FROM squadra s, utente u WHERE s.nome='I Bomber' AND u.email='sara@example.com'$$);

\echo ''
\echo '--- Tiri di freccette'
SELECT deve_fallire('punteggio non coerente con settore e moltiplicatore',
 $$INSERT INTO tiro_freccette (evento_partita_id,partita_id,partecipazione_id,numero_turno,numero_tiro_nel_turno,settore,moltiplicatore,punteggio_calcolato,punteggio_prima_del_tiro,punteggio_dopo_il_tiro)
   SELECT e.id,e.partita_id,pp.id,2,1,20,3,61,441,380
   FROM evento_partita e, partecipazione_partita pp
   WHERE e.identificativo_evento_sorgente='evt-0001' AND pp.ordine_turno=1 AND pp.partita_id=e.partita_id$$);
SELECT deve_fallire('triplo sul bull (25x3) inesistente',
 $$INSERT INTO tiro_freccette (evento_partita_id,partita_id,partecipazione_id,numero_turno,numero_tiro_nel_turno,settore,moltiplicatore,punteggio_calcolato,punteggio_prima_del_tiro,punteggio_dopo_il_tiro)
   SELECT e.id,e.partita_id,pp.id,3,1,25,3,75,441,366
   FROM evento_partita e, partecipazione_partita pp
   WHERE e.identificativo_evento_sorgente='evt-0001' AND pp.ordine_turno=1 AND pp.partita_id=e.partita_id$$);
SELECT deve_fallire('tiro sballato che modifica comunque il punteggio',
 $$INSERT INTO tiro_freccette (evento_partita_id,partita_id,partecipazione_id,numero_turno,numero_tiro_nel_turno,settore,moltiplicatore,punteggio_calcolato,punteggio_prima_del_tiro,punteggio_dopo_il_tiro,sballato)
   SELECT e.id,e.partita_id,pp.id,4,1,20,1,20,15,-5,TRUE
   FROM evento_partita e, partecipazione_partita pp
   WHERE e.identificativo_evento_sorgente='evt-0001' AND pp.ordine_turno=1 AND pp.partita_id=e.partita_id$$);
SELECT deve_fallire('secondo tiro collegato allo stesso evento',
 $$INSERT INTO tiro_freccette (evento_partita_id,partita_id,partecipazione_id,numero_turno,numero_tiro_nel_turno,settore,moltiplicatore,punteggio_calcolato,punteggio_prima_del_tiro,punteggio_dopo_il_tiro)
   SELECT e.id,e.partita_id,pp.id,1,2,5,1,5,441,436
   FROM evento_partita e, partecipazione_partita pp
   WHERE e.identificativo_evento_sorgente='evt-0001' AND pp.ordine_turno=1 AND pp.partita_id=e.partita_id$$);

-- tiro collegato a un evento che non e' un TIRO_RILEVATO
INSERT INTO evento_partita (partita_id,gioco_installato_id,codice_tipo_gioco,tipo_evento_id,codice_tipo_evento,identificativo_evento_sorgente,data_evento)
SELECT p.id,p.gioco_installato_id,p.codice_tipo_gioco,te.id,te.codice,'evt-0002',now()
FROM partita p, tipo_evento_gioco te
WHERE p.identificativo_sorgente='3f9a-0001' AND te.codice='TURNO_TERMINATO';

SELECT deve_fallire('tiro collegato a un evento TURNO_TERMINATO',
 $$INSERT INTO tiro_freccette (evento_partita_id,partita_id,partecipazione_id,numero_turno,numero_tiro_nel_turno,settore,moltiplicatore,punteggio_calcolato,punteggio_prima_del_tiro,punteggio_dopo_il_tiro)
   SELECT e.id,e.partita_id,pp.id,5,1,10,1,10,441,431
   FROM evento_partita e, partecipazione_partita pp
   WHERE e.identificativo_evento_sorgente='evt-0002' AND pp.ordine_turno=1 AND pp.partita_id=e.partita_id$$);

-- seconda partita, per verificare l'incrocio fra partite diverse
INSERT INTO partita (identificativo_sorgente,gioco_installato_id,codice_tipo_gioco,tipo_partecipazione,stato,data_inizio)
SELECT '3f9a-0010',gi.id,gi.codice_tipo_gioco,'INDIVIDUALE','IN_CORSO',now()
FROM gioco_installato gi WHERE gi.codice_accesso='FREC01';
INSERT INTO partecipazione_partita (partita_id,tipo_partecipazione,utente_id,ordine_turno)
SELECT p.id,'INDIVIDUALE',u.id,1 FROM partita p, utente u
WHERE p.identificativo_sorgente='3f9a-0010' AND u.email='marco@example.com';

SELECT deve_fallire('tiro attribuito a un partecipante di un''altra partita',
 $$INSERT INTO tiro_freccette (evento_partita_id,partita_id,partecipazione_id,numero_turno,numero_tiro_nel_turno,settore,moltiplicatore,punteggio_calcolato,punteggio_prima_del_tiro,punteggio_dopo_il_tiro)
   SELECT e.id,e.partita_id,pp.id,6,1,19,1,19,441,422
   FROM evento_partita e, partecipazione_partita pp, partita p2
   WHERE e.identificativo_evento_sorgente='evt-0002'
     AND p2.identificativo_sorgente='3f9a-0010' AND pp.partita_id=p2.id$$);

\echo ''
\echo '--- Risultati'
UPDATE partecipazione_partita SET posizione_finale=1, punteggio_finale=0, stato='TERMINATO'
WHERE ordine_turno=1 AND partita_id=(SELECT id FROM partita WHERE identificativo_sorgente='3f9a-0001');
SELECT deve_fallire('due primi classificati nella stessa partita',
 $$UPDATE partecipazione_partita SET posizione_finale=1
   WHERE ordine_turno=2 AND partita_id=(SELECT id FROM partita WHERE identificativo_sorgente='3f9a-0001')$$);

\echo ''
\echo '--- Verifica dell''aggiornamento automatico di data_aggiornamento'
WITH t AS (
    UPDATE utente SET nome='Anna Maria' WHERE email='anna@example.com'
    RETURNING data_creazione, data_aggiornamento
)
SELECT CASE WHEN data_aggiornamento > data_creazione
            THEN 'OK       data_aggiornamento aggiornata dal trigger'
            ELSE '>>> FALLITO: trigger non attivo' END
FROM t;

ROLLBACK;