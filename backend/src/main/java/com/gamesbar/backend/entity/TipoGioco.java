package com.gamesbar.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_gioco")

public class TipoGioco {
    
    @Id             //chiave primaria
    @GeneratedValue(strategy=GenerationType.IDENTITY)  //valore del id viene generata dal database
    private Long id;

    @Column(name = "amministratore_id")
    private Long adminId;

    @Column(nullable = false)
    private String codice;

    @Column(nullable=false)
    private String nome;

    @Column
    private String descrizione;

    @Column
    private String versione_gestore;

    @Column(nullable=false)
    private boolean attivo;

    @Column(nullable=false)
    private Instant data_creazione;

    @Column(nullable=false)
    private Instant data_aggiornamento;


    //costrutore
    protected TipoGioco(){}

     //getter e setters
    public Long getId() {
        return id;
    }
    public String getNome() {
        return nome;
    }

    public Long getAmministratore_id() {
        return adminId;
    }

    public String getCodice() {
        return codice;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getVersione_gestore() {
        return versione_gestore;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public Instant getData_creazione() {
        return data_creazione;
    }

    public Instant getData_aggiornamento() {
        return data_aggiornamento;
    }

    
    public void setAmministratore_id(Long amministratore_id) {
        this.adminId = amministratore_id;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public void setVersione_gestore(String versione_gestore) {
        this.versione_gestore = versione_gestore;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

   
    
}



/*
tabella nel database
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    amministratore_id   BIGINT       REFERENCES utente(id) ON DELETE SET NULL,
    codice              VARCHAR(40)  NOT NULL,
    nome                VARCHAR(100) NOT NULL,
    descrizione         TEXT,
    versione_gestore    VARCHAR(20),
    attivo              BOOLEAN      NOT NULL DEFAULT FALSE,
    data_creazione      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    data_aggiornamento  TIMESTAMPTZ  NOT NULL DEFAULT now(),
     */