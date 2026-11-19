package com.gamesbar.backend.locale;

//lanciata quando l'id richiesto non corrisponde a nessun locale
public class LocaleNonTrovatoException extends RuntimeException {

    public LocaleNonTrovatoException(Long id) {
        super("il locale con id " + id + " non esiste.");
    }
}
