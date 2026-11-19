package com.gamesbar.backend.locale;

//descrive il JSON di un locale restituito dall'API
public record LocaleRisposta(
    Long id,
    Long amministratoreId,
    String nome,
    String descrizione,
    String indirizzo,
    String citta,
    String codicePostale,
    String provincia,
    String numeroTelefono,
    StatoVerificaLocale statoVerifica,
    boolean attivo
){
    //trasforma l'Entity in risposta
    public static LocaleRisposta daLocale(Locale locale) {
        return new LocaleRisposta(
            locale.getId(),
            locale.getAmministratoreId(),
            locale.getNome(),
            locale.getDescrizione(),
            locale.getIndirizzo(),
            locale.getCitta(),
            locale.getCodicePostale(),
            locale.getProvincia(),
            locale.getNumeroTelefono(),
            locale.getStatoVerifica(),
            locale.isAttivo()
        );
    }
}
