package com.gamesbar.backend.tipogioco.evento;

import com.gamesbar.backend.tipogioco.TipoGioco;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//un tipo di evento che un gioco può registrare registra TIRO_RILEVATO per il gioco X
//"abilitata" è la regola di registrazione: un evento disabilitato non viene registrato
@Entity
@Table(name = "tipo_evento_gioco")
public class TipoEventoGioco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_gioco_id", nullable = false)
    private Long tipoGiocoId;

    //copia del codice del gioco (esempio "FRECCETTE"): insieme a tipo_gioco_id forma la chiave esterna composta,
    //così il DB controlla che sensori ed eventi delle partite appartengano allo stesso gioco
    @Column(name = "codice_tipo_gioco", nullable = false, length = 40)
    private String codiceTipoGioco;

    @Column(name = "codice", nullable = false, length = 60)
    private String codice;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descrizione")
    private String descrizione;

    @Column(name = "abilitata", nullable = false)
    private boolean abilitata;


    //costrutore per JPA
    protected TipoEventoGioco() {}

    //il tipo di gioco viene passato intero, così id e codice sono sicuramente quelli giusti
    public TipoEventoGioco(TipoGioco tipoGioco, String codice, String nome, String descrizione) {
        if (codice == null || codice.isBlank()) {
            throw new IllegalArgumentException("il codice dell'evento è obbligatorio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("il nome dell'evento è obbligatorio.");
        }
        this.tipoGiocoId = tipoGioco.getId();
        this.codiceTipoGioco = tipoGioco.getCodice();
        this.codice = codice.trim().toUpperCase();   //il DB accetta solo codici maiuscoli
        this.nome = nome.trim();
        this.descrizione = descrizione;
        this.abilitata = true;
    }

    public void abilita() {
        this.abilitata = true;
    }

    public void disabilita() {
        this.abilitata = false;
    }

    //getter
    public Long getId() {
        return id;
    }

    public Long getTipoGiocoId() {
        return tipoGiocoId;
    }

    public String getCodiceTipoGioco() {
        return codiceTipoGioco;
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

    public boolean isAbilitata() {
        return abilitata;
    }
}

/*
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_gioco_id      BIGINT       NOT NULL,
    codice_tipo_gioco  VARCHAR(40)  NOT NULL,
    codice             VARCHAR(60)  NOT NULL,
    nome               VARCHAR(100) NOT NULL,
    descrizione        TEXT,
    abilitata          BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT tipo_evento_codice_univoco   UNIQUE (tipo_gioco_id, codice),
    CONSTRAINT tipo_evento_codice_formato   CHECK (codice ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT tipo_evento_tipo_gioco_fk    FOREIGN KEY (tipo_gioco_id, codice_tipo_gioco)
        REFERENCES tipo_gioco(id, codice) ON UPDATE CASCADE ON DELETE CASCADE
 */
