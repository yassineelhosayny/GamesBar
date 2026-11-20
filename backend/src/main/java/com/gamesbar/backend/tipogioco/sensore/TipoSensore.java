package com.gamesbar.backend.tipogioco.sensore;

import java.util.Objects;

import com.gamesbar.backend.tipogioco.TipoGioco;
import com.gamesbar.backend.tipogioco.evento.TipoEventoGioco;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//un tipo di sensore previsto da un gioco (es. RILEVATORE_TIRO per le freccette)
//e l'evento che produce (es. TIRO_RILEVATO)
@Entity
@Table(name = "tipo_sensore")
public class TipoSensore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_gioco_id", nullable = false)
    private Long tipoGiocoId;

    //copia del codice del gioco, come in TipoEventoGioco (chiave esterna composta)
    @Column(name = "codice_tipo_gioco", nullable = false, length = 40)
    private String codiceTipoGioco;

    @Column(name = "codice", nullable = false, length = 60)
    private String codice;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descrizione")
    private String descrizione;

    //l'evento che il sensore produce: deve essere dello stesso gioco (lo controlla anche il DB)
    @Column(name = "tipo_evento_prodotto_id", nullable = false)
    private Long tipoEventoProdottoId;

    //true = un gioco installato di questo tipo deve avere questo sensore
    @Column(name = "obbligatorio", nullable = false)
    private boolean obbligatorio;

    @Column(name = "attivo", nullable = false)
    private boolean attivo;


    //costrutore per JPA
    protected TipoSensore() {}

    public TipoSensore(TipoGioco tipoGioco, String codice, String nome, String descrizione,
                       TipoEventoGioco eventoProdotto, boolean obbligatorio) {
        if (codice == null || codice.isBlank()) {
            throw new IllegalArgumentException("il codice del sensore è obbligatorio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("il nome del sensore è obbligatorio.");
        }
        //stesso controllo della chiave esterna composta, fatto anche in Java per un errore più chiaro
        //(Objects.equals confronta due Long anche se uno dei due è null, senza NullPointerException)
        if (!Objects.equals(eventoProdotto.getTipoGiocoId(), tipoGioco.getId())) {
            throw new IllegalArgumentException("l'evento prodotto deve appartenere allo stesso gioco del sensore.");
        }
        this.tipoGiocoId = tipoGioco.getId();
        this.codiceTipoGioco = tipoGioco.getCodice();
        this.codice = codice.trim().toUpperCase();
        this.nome = nome.trim();
        this.descrizione = descrizione;
        this.tipoEventoProdottoId = eventoProdotto.getId();
        this.obbligatorio = obbligatorio;
        this.attivo = true;
    }

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

    public Long getTipoEventoProdottoId() {
        return tipoEventoProdottoId;
    }

    public boolean isObbligatorio() {
        return obbligatorio;
    }

    public boolean isAttivo() {
        return attivo;
    }
}

/*
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
    CONSTRAINT tipo_sensore_tipo_gioco_fk  FOREIGN KEY (tipo_gioco_id, codice_tipo_gioco)
        REFERENCES tipo_gioco(id, codice) ON UPDATE CASCADE ON DELETE CASCADE,
    -- l'evento prodotto deve appartenere allo stesso tipo di gioco del sensore
    CONSTRAINT tipo_sensore_evento_fk      FOREIGN KEY (tipo_evento_prodotto_id, codice_tipo_gioco)
        REFERENCES tipo_evento_gioco(id, codice_tipo_gioco) ON UPDATE CASCADE
 */
