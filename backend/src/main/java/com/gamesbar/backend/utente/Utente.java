package com.gamesbar.backend.utente;

import java.time.Instant;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Utente.java
├── mapping della tabella (ruolo/stato con RuoloUtente e StatoUtente)
├── costruttori
├── regole interne dell’utente
│   ├── cambiaNome()
│   ├── cambiaHashPassword()
│   ├── assegnaRuolo()
│   └── attiva/sospendi/blocca
└── getter
 */


@Entity
@Table(name = "utente")
public class Utente {

    @Id             //chiave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY)  //valore del id viene generato dal database
    private Long id;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "cognome", nullable = false, length = 80)
    private String cognome;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    //solo l'hash, mai la password in chiaro
    @Column(name = "hash_password")
    private String hashPassword;

    //servirà con OAuth, per ora solo mappato
    @Column(name = "identificativo_google", unique = true)
    private String identificativoGoogle;

    @Column(name = "immagine_profilo", length = 512)
    private String immagineProfilo;

    @Enumerated(EnumType.STRING)    //salva il nome dell'enum come testo, es. 'GIOCATORE'
    @Column(name = "ruolo", nullable = false, length = 30)
    private RuoloUtente ruolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 20)
    private StatoUtente stato;

    //scritta dal DB con DEFAULT now(): Java non la inserisce e non la modifica,
    //@Generated fa rileggere il valore a Hibernate subito dopo l'INSERT
    @Generated
    @Column(name = "data_creazione", nullable = false, insertable = false, updatable = false)
    private Instant dataCreazione;

    //scritta dal DB con DEFAULT now() e dal trigger utente_aggiornamento,
    //Hibernate rilegge il valore dopo ogni INSERT e UPDATE
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "data_aggiornamento", nullable = false, insertable = false, updatable = false)
    private Instant dataAggiornamento;

    @Column(name = "data_ultimo_accesso")
    private Instant dataUltimoAccesso;


    //costrutore per JPA
    protected Utente() {}

    //costrutore per la registrazione con email/password
    public Utente(String nome, String cognome, String email, String hashPassword) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("l'email è obbligatoria.");
        }
        cambiaNome(nome, cognome);
        cambiaHashPassword(hashPassword);
        this.email = email.trim().toLowerCase();   //il DB accetta solo email minuscole
        this.ruolo = RuoloUtente.GIOCATORE;
        this.stato = StatoUtente.ATTIVO;
    }

    //operazioni controllate al posto dei setter
    public void cambiaNome(String nome, String cognome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("il nome è obbligatorio.");
        }
        if (cognome == null || cognome.isBlank()) {
            throw new IllegalArgumentException("il cognome è obbligatorio.");
        }
        this.nome = nome.trim();
        this.cognome = cognome.trim();
    }

    public void cambiaHashPassword(String hashPassword) {
        if (hashPassword == null || hashPassword.isBlank()) {
            throw new IllegalArgumentException("l'hash della password è obbligatorio.");
        }
        this.hashPassword = hashPassword;
    }

    public void assegnaRuolo(RuoloUtente ruolo) {
        if (ruolo == null) {
            throw new IllegalArgumentException("il ruolo è obbligatorio.");
        }
        this.ruolo = ruolo;
    }

    public void attiva() {
        this.stato = StatoUtente.ATTIVO;
    }

    public void sospendi() {
        this.stato = StatoUtente.SOSPESO;
    }

    public void blocca() {
        this.stato = StatoUtente.BLOCCATO;
    }

    public boolean isAttivo() {
        return stato == StatoUtente.ATTIVO;
    }

    //getter
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getEmail() {
        return email;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public String getIdentificativoGoogle() {
        return identificativoGoogle;
    }

    public String getImmagineProfilo() {
        return immagineProfilo;
    }

    public RuoloUtente getRuolo() {
        return ruolo;
    }

    public StatoUtente getStato() {
        return stato;
    }

    public Instant getDataCreazione() {
        return dataCreazione;
    }

    public Instant getDataAggiornamento() {
        return dataAggiornamento;
    }

    public Instant getDataUltimoAccesso() {
        return dataUltimoAccesso;
    }
}

