package com.gamesbar.backend.locale;

import java.time.Instant;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//un luogo dove sono installati i giochi: bar,sala giochi,casa privata.
@Entity
@Table(name = "locale")
public class Locale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amministratore_id", nullable = false)
    private Long amministratoreId;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "descrizione")
    private String descrizione;

    @Column(name = "indirizzo", nullable = false)
    private String indirizzo;

    @Column(name = "citta", nullable = false, length = 100)
    private String citta;

    @Column(name = "codice_postale", nullable = false, length = 10)
    private String codicePostale;

    //nel DB è CHAR(2) e non VARCHAR: 
    @JdbcTypeCode(SqlTypes.CHAR) //ho trobvato questa soluzione dove lo diciamo a Hibernate, altrimenti la validazione dello schema fallisce
    @Column(name = "provincia", length = 2)
    private String provincia;

    @Column(name = "numero_telefono", length = 30)
    private String numeroTelefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato_verifica", nullable = false, length = 20)
    private StatoVerificaLocale statoVerifica;

    @Column(name = "attivo", nullable = false)
    private boolean attivo;

    @Generated
    @Column(name = "data_creazione", nullable = false, insertable = false, updatable = false)
    private Instant dataCreazione;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "data_aggiornamento", nullable = false, insertable = false, updatable = false)
    private Instant dataAggiornamento;

    //costrutore JPA
    protected Locale() {}

    //costrutore per un nuovo locale: nasce IN_ATTESA per verifica di un admin
    public Locale(Long amministratoreId, String nome, String descrizione, String indirizzo, String citta,
                  String codicePostale, String provincia, String numeroTelefono) {
        if (indirizzo == null || indirizzo.isBlank()) {
            throw new IllegalArgumentException("l'indirizzo è obbligatorio.");
        }if (citta == null || citta.isBlank()) {
            throw new IllegalArgumentException("la città è obbligatoria.");
        }
        if (codicePostale == null || codicePostale.isBlank()) {
            throw new IllegalArgumentException("il codice postale è obbligatorio.");
        }
        this.amministratoreId = amministratoreId;
        this.indirizzo = indirizzo.trim();
        this.citta = citta.trim();
        this.codicePostale = codicePostale.trim();

        if (provincia == null || provincia.isBlank()) {
            this.provincia = null;
        } else {
            this.provincia = provincia.trim().toUpperCase();
        }

        modificaDati(nome, descrizione, numeroTelefono); //redurre il codice
        this.statoVerifica = StatoVerificaLocale.IN_ATTESA;
        this.attivo = true;
    }

    //i soli dati che il gestore può cambiare dopo la creazione.
    //indirizzo, città, CAP e provincia restano quelli verificati dall'admin
    public void modificaDati(String nome, String descrizione, String numeroTelefono) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("il nome del locale è obbligatorio.");
        }
        this.nome = nome.trim();
        this.descrizione = descrizione;
        this.numeroTelefono = numeroTelefono;
    }

    public void approva() {
        this.statoVerifica = StatoVerificaLocale.APPROVATO;
    }

    public void rifiuta() {
        this.statoVerifica = StatoVerificaLocale.RIFIUTATO;
    }

    public boolean isRifiutato() {
        return statoVerifica == StatoVerificaLocale.RIFIUTATO;
    }

    //getter
    public Long getId() {
        return id;
    }

    public Long getAmministratoreId() {
        return amministratoreId;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public String getCitta() {
        return citta;
    }

    public String getCodicePostale() {
        return codicePostale;
    }

    public String getProvincia() {
        return provincia;
    }

    public String getNumeroTelefono() {
        return numeroTelefono;
    }

    public StatoVerificaLocale getStatoVerifica() {
        return statoVerifica;
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
 */
