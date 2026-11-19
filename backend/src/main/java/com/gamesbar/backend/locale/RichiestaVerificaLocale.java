package com.gamesbar.backend.locale;

import java.time.Instant;

import org.hibernate.annotations.Generated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//la richiesta con cui un gestore chiede all'admin della piattaforma di verificare il suo locale.
@Entity
@Table(name = "richiesta_verifica_locale")
public class RichiestaVerificaLocale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "locale_id", nullable = false)
    private Long localeId;

    @Column(name = "richiedente_id", nullable = false)
    private Long richiedenteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 20)
    private StatoRichiestaVerifica stato;

    @Column(name = "messaggio_richiedente")
    private String messaggioRichiedente;

    //chi ha deciso (l'admin)
    @Column(name = "revisore_id")
    private Long revisoreId;

    @Column(name = "motivo_rifiuto")
    private String motivoRifiuto;

    @Generated
    @Column(name = "data_richiesta", nullable = false, insertable = false, updatable = false)
    private Instant dataRichiesta;

    @Column(name = "data_revisione")
    private Instant dataRevisione;


    //costrutore JPA
    protected RichiestaVerificaLocale() {}

    public RichiestaVerificaLocale(Long localeId, Long richiedenteId, String messaggioRichiedente) {
        this.localeId = localeId;
        this.richiedenteId = richiedenteId;
        this.messaggioRichiedente = messaggioRichiedente;
        this.stato = StatoRichiestaVerifica.IN_ATTESA;
    }

    public boolean isInAttesa() {
        return stato == StatoRichiestaVerifica.IN_ATTESA;
    }

    public void approva(Long revisoreId) {
        this.stato = StatoRichiestaVerifica.APPROVATA;
        this.revisoreId = revisoreId;
        this.dataRevisione = momentoRevisione();
    }

    public void rifiuta(Long revisoreId, String motivo) {
        //il CHECK richiesta_motivo_rifiuto vuole sempre un motivo per il rifiuto
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("il motivo del rifiuto è obbligatorio.");
        }
        this.stato = StatoRichiestaVerifica.RIFIUTATA;
        this.revisoreId = revisoreId;
        this.motivoRifiuto = motivo.trim();
        this.dataRevisione = momentoRevisione();
    }
    private Instant momentoRevisione() {
        Instant adesso = Instant.now();
        if (dataRichiesta != null && adesso.isBefore(dataRichiesta)) {
            adesso = dataRichiesta;
        }
        return adesso;
    }

    //getter
    public Long getId() {
        return id;
    }

    public Long getLocaleId() {
        return localeId;
    }

    public Long getRichiedenteId() {
        return richiedenteId;
    }

    public StatoRichiestaVerifica getStato() {
        return stato;
    }

    public String getMessaggioRichiedente() {
        return messaggioRichiedente;
    }

    public Long getRevisoreId() {
        return revisoreId;
    }

    public String getMotivoRifiuto() {
        return motivoRifiuto;
    }

    public Instant getDataRichiesta() {
        return dataRichiesta;
    }

    public Instant getDataRevisione() {
        return dataRevisione;
    }
}

/*
    id                    BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    locale_id             BIGINT      NOT NULL REFERENCES locale(id) ON DELETE CASCADE,
    richiedente_id        BIGINT      NOT NULL REFERENCES utente(id) ON DELETE RESTRICT,
    stato                 VARCHAR(20) NOT NULL DEFAULT 'IN_ATTESA',
    messaggio_richiedente TEXT,
    revisore_id           BIGINT      REFERENCES utente(id) ON DELETE SET NULL,
    motivo_rifiuto        TEXT,
    data_richiesta        TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_revisione        TIMESTAMPTZ,

    CONSTRAINT richiesta_stato_valido CHECK (stato IN ('IN_ATTESA','APPROVATA','RIFIUTATA')),
    CONSTRAINT richiesta_revisione_coerente CHECK (
        (stato = 'IN_ATTESA' AND revisore_id IS NULL AND data_revisione IS NULL)
     OR (stato <> 'IN_ATTESA' AND revisore_id IS NOT NULL AND data_revisione IS NOT NULL)),
    CONSTRAINT richiesta_motivo_rifiuto CHECK (
        (stato = 'RIFIUTATA' AND motivo_rifiuto IS NOT NULL)
     OR (stato <> 'RIFIUTATA' AND motivo_rifiuto IS NULL)),
    CONSTRAINT richiesta_revisione_non_anteriore CHECK (
        data_revisione IS NULL OR data_revisione >= data_richiesta)
 */
