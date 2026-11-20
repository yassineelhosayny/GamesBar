package com.gamesbar.backend.tipogioco;

import java.time.Instant;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

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

    //chi ha creato il tipo di gioco (admin del gioco)
    @Column(name = "amministratore_id")
    private Long adminId;

    @Column(name = "codice", nullable = false, length = 40)
    private String codice;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descrizione")
    private String descrizione;

    @Column(name = "versione_gestore", length = 20)
    private String versioneGestore;

    @Column(name = "attivo", nullable = false)
    private boolean attivo;

    @Generated
    @Column(name = "data_creazione", nullable = false, insertable = false, updatable = false)
    private Instant dataCreazione;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "data_aggiornamento", nullable = false, insertable = false, updatable = false)
    private Instant dataAggiornamento;


    //costrutore per JPA
    protected TipoGioco(){}

    //l'attivazione è una scelta esplicita dell'amministratore
    public TipoGioco(Long adminId, String codice, String nome, String descrizione, String versioneGestore){
        if(codice == null || codice.isBlank()){
            throw new IllegalArgumentException("il codice è obbligatorio.");
        }
        if(nome == null || nome.isBlank()){
            throw new IllegalArgumentException("il nome è obbligatorio.");
        }
        this.adminId = adminId;
        this.codice = codice.trim().toUpperCase();   //il DB accetta solo codici maiuscoli (es. FRECCETTE)
        this.nome = nome.trim();
        this.descrizione = descrizione;
        this.versioneGestore = versioneGestore;
        this.attivo = false;
    }

    //operazioni controllate al posto dei setter
    public void attiva() {
        this.attivo = true;
    }

    public void disattiva() {
        this.attivo = false;
    }

    //getter
    public Long getId() {
        return id;
    }

    public Long getAmministratore_id() {
        return adminId;
    }

    public String getCodice() {
        return codice;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getVersioneGestore() {
        return versioneGestore;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public Instant getDataCreazione() {
        return dataCreazione;
    }

    public Instant getDataAggiornamento() {
        return dataAggiornamento;
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

    CONSTRAINT tipo_gioco_codice_univoco UNIQUE (codice),
    CONSTRAINT tipo_gioco_codice_formato CHECK (codice ~ '^[A-Z][A-Z0-9_]*$'),
     */
