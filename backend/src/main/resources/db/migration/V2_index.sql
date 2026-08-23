-- =============================================================================
-- PISSIR - V2 : indici
--
-- PostgreSQL crea automaticamente un indice per ogni PRIMARY KEY e per ogni
-- vincolo UNIQUE. Qui vengono aggiunti soltanto:
--   * gli indici sulle chiavi esterne non gia' coperte (PostgreSQL non li crea
--     da solo: servono per le cancellazioni e per le join piu' frequenti);
--   * gli indici a supporto delle interrogazioni statistiche descritte nella
--     specifica, che vengono calcolate al volo e non memorizzate.
-- =============================================================================

-- --- chiavi esterne verso utente -------------------------------------------
CREATE INDEX idx_locale_amministratore        ON locale (amministratore_id);
CREATE INDEX idx_richiesta_richiedente        ON richiesta_verifica_locale (richiedente_id);
CREATE INDEX idx_richiesta_revisore           ON richiesta_verifica_locale (revisore_id);
CREATE INDEX idx_tipo_gioco_amministratore    ON tipo_gioco (amministratore_id);
CREATE INDEX idx_torneo_creatore              ON torneo (creatore_id);
CREATE INDEX idx_squadra_capitano             ON squadra (capitano_id);
CREATE INDEX idx_partita_creatore             ON partita (creatore_id);
CREATE INDEX idx_tiro_utente_esecutore        ON tiro_freccette (utente_esecutore_id);

-- --- struttura dei locali ---------------------------------------------------
CREATE INDEX idx_edge_locale                  ON edge (locale_id);
CREATE INDEX idx_gioco_locale                 ON gioco_installato (locale_id);
CREATE INDEX idx_gioco_edge                   ON gioco_installato (edge_id);
CREATE INDEX idx_gioco_tipo                   ON gioco_installato (tipo_gioco_id);
CREATE INDEX idx_sensore_tipo                 ON sensore (tipo_sensore_id);
CREATE INDEX idx_richiesta_locale             ON richiesta_verifica_locale (locale_id);

-- elenco dei giochi disponibili al giocatore: solo locali approvati e attivi
CREATE INDEX idx_gioco_disponibile
    ON gioco_installato (locale_id, stato)
    WHERE stato = 'ATTIVO';

-- --- partite ----------------------------------------------------------------
CREATE INDEX idx_partita_gioco_installato     ON partita (gioco_installato_id);
CREATE INDEX idx_partita_torneo               ON partita (torneo_id);
CREATE INDEX idx_partita_edge                 ON partita (edge_utilizzato_id);

-- monitoraggio delle partite in corso da parte dell'amministratore del locale
CREATE INDEX idx_partita_in_corso
    ON partita (gioco_installato_id, data_inizio DESC)
    WHERE stato IN ('CREATA','IN_CORSO','SOSPESA');

-- storico per le statistiche temporali
CREATE INDEX idx_partita_terminate
    ON partita (data_fine DESC)
    WHERE stato = 'TERMINATA';

-- --- partecipazioni ---------------------------------------------------------
-- statistiche del singolo giocatore: partite giocate, percentuale di vittorie
CREATE INDEX idx_partecipazione_utente        ON partecipazione_partita (utente_id)
    WHERE utente_id IS NOT NULL;
CREATE INDEX idx_partecipazione_squadra       ON partecipazione_partita (squadra_id)
    WHERE squadra_id IS NOT NULL;

-- --- eventi -----------------------------------------------------------------
-- ricostruzione cronologica di una partita
CREATE INDEX idx_evento_partita_cronologia    ON evento_partita (partita_id, data_evento);
CREATE INDEX idx_evento_sensore               ON evento_partita (sensore_id)
    WHERE sensore_id IS NOT NULL;
CREATE INDEX idx_evento_tipo                  ON evento_partita (tipo_evento_id);

-- diagnostica della sincronizzazione offline
CREATE INDEX idx_evento_sincronizzati
    ON evento_partita (gioco_installato_id, data_ricezione DESC)
    WHERE sincronizzato_da_offline;

-- --- tiri -------------------------------------------------------------------
-- media punti per tiro, sequenza dei turni
CREATE INDEX idx_tiro_partita                 ON tiro_freccette (partita_id);

-- --- tornei e squadre -------------------------------------------------------
CREATE INDEX idx_torneo_tipo_gioco            ON torneo (tipo_gioco_id);
CREATE INDEX idx_torneo_locale_locale         ON torneo_locale (locale_id);
CREATE INDEX idx_torneo_locale_organizzatore  ON torneo_locale (organizzatore_principale_id);
CREATE INDEX idx_squadra_torneo               ON squadra (torneo_id) WHERE torneo_id IS NOT NULL;
CREATE INDEX idx_membro_squadra_utente        ON membro_squadra (utente_id);

-- --- tipi -------------------------------------------------------------------
CREATE INDEX idx_tipo_evento_tipo_gioco       ON tipo_evento_gioco (tipo_gioco_id);
CREATE INDEX idx_tipo_sensore_tipo_gioco      ON tipo_sensore (tipo_gioco_id);
CREATE INDEX idx_tipo_sensore_evento          ON tipo_sensore (tipo_evento_prodotto_id);
CREATE INDEX idx_config_gioco_modalita        ON configurazione_gioco_freccette (modalita_predefinita_id);
CREATE INDEX idx_config_partita_modalita      ON configurazione_partita_freccette (modalita_freccette_id);