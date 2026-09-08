package com.gamesbar.backend.utente;

import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name= "utente")
public class utente {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    
}

/*
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
        */