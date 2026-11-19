package com.gamesbar.backend.locale;

import java.time.Instant;

//iil JSON di una richiesta di verifica: dati richiesta + locale da verificare
public record RichiestaLocaleRisposta(
    Long id,
    StatoRichiestaVerifica stato,
    Long richiedenteId,
    String messaggioRichiedente,
    String motivoRifiuto,
    Instant dataRichiesta,
    Instant dataRevisione,
    LocaleRisposta locale
){
    public static RichiestaLocaleRisposta daRichiesta(RichiestaVerificaLocale richiesta, Locale locale) {
        return new RichiestaLocaleRisposta(
            richiesta.getId(),
            richiesta.getStato(),
            richiesta.getRichiedenteId(),
            richiesta.getMessaggioRichiedente(),
            richiesta.getMotivoRifiuto(),
            richiesta.getDataRichiesta(),
            richiesta.getDataRevisione(),
            LocaleRisposta.daLocale(locale)
        );
    }
}
