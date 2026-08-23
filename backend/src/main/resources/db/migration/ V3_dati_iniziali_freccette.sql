-- =============================================================================
-- PISSIR - V3 : dati iniziali del gioco dimostrativo (freccette)
--
-- Contiene solo cio' che corrisponde a codice gia' implementato dallo
-- sviluppatore: il tipo di gioco, i tipi di evento che il GestoreFreccette sa
-- interpretare, il tipo di sensore previsto e le tre modalita' supportate.
-- Nessun dato di prova (utenti, locali, partite): quelli restano nei fixture
-- di test e nello scenario della demo.
--
-- Il tipo di gioco viene inserito con attivo = FALSE: l'attivazione e' un atto
-- esplicito dell'amministratore della piattaforma, come da specifica.
-- Tutte le migration sono idempotenti (ON CONFLICT DO NOTHING) per non
-- rompere una eventuale riesecuzione su basi dati gia' popolate.
-- =============================================================================

-- --- tipo di gioco ----------------------------------------------------------
INSERT INTO tipo_gioco (codice, nome, descrizione, versione_gestore, attivo)
VALUES ('FRECCETTE',
        'Freccette',
        'Gioco delle freccette con modalita'' a punteggio decrescente 301, 501 e 701.',
        '1.0.0',
        FALSE)
ON CONFLICT (codice) DO NOTHING;


-- --- tipi di evento previsti dal GestoreFreccette ---------------------------
INSERT INTO tipo_evento_gioco (tipo_gioco_id, codice_tipo_gioco, codice, nome, descrizione, abilitata)
SELECT tg.id, tg.codice, v.codice, v.nome, v.descrizione, TRUE
FROM tipo_gioco tg
CROSS JOIN (VALUES
    ('PARTITA_INIZIATA',   'Partita iniziata',   'Avvio della partita sulla freccettiera.'),
    ('TIRO_RILEVATO',      'Tiro rilevato',      'Singolo tiro rilevato dal sensore, con settore e moltiplicatore.'),
    ('TURNO_TERMINATO',    'Turno terminato',    'Conclusione del turno del partecipante corrente.'),
    ('PARTITA_TERMINATA',  'Partita terminata',  'Chiusura regolare della partita con un vincitore.'),
    ('PARTITA_INTERROTTA', 'Partita interrotta', 'Interruzione anticipata decisa dall''amministratore del locale.')
) AS v(codice, nome, descrizione)
WHERE tg.codice = 'FRECCETTE'
ON CONFLICT (tipo_gioco_id, codice) DO NOTHING;


-- --- tipo di sensore --------------------------------------------------------
-- Una sola sorgente di eventi per freccettiera: settore e moltiplicatore sono
-- attributi del tiro, non identita' di sensori distinti. Il simulatore si
-- registra come sensore con simulato = TRUE.
INSERT INTO tipo_sensore (tipo_gioco_id, codice_tipo_gioco, codice, nome, descrizione,
                          tipo_evento_prodotto_id, obbligatorio, attivo)
SELECT tg.id,
       tg.codice,
       'RILEVATORE_TIRO',
       'Rilevatore di tiro',
       'Rileva il punto di impatto della freccetta e produce settore e moltiplicatore.',
       te.id,
       TRUE,
       TRUE
FROM tipo_gioco tg
JOIN tipo_evento_gioco te
     ON te.tipo_gioco_id = tg.id AND te.codice = 'TIRO_RILEVATO'
WHERE tg.codice = 'FRECCETTE'
ON CONFLICT (tipo_gioco_id, codice) DO NOTHING;


-- --- modalita' supportate ---------------------------------------------------
INSERT INTO modalita_freccette (codice, nome, punteggio_iniziale, chiusura_doppia, frecce_per_turno, attiva)
VALUES
    ('301', 'Freccette 301', 301, FALSE, 3, TRUE),
    ('501', 'Freccette 501', 501, FALSE, 3, TRUE),
    ('701', 'Freccette 701', 701, FALSE, 3, TRUE)
ON CONFLICT (codice) DO NOTHING;