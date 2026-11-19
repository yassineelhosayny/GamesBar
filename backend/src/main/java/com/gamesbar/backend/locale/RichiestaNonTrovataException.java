package com.gamesbar.backend.locale;

//lanciata quando l'id richiesto non corrisponde a nessuna richiesta di verifica
public class RichiestaNonTrovataException extends RuntimeException {

    public RichiestaNonTrovataException(Long id) {
        super("la richiesta con id " + id + " non esiste.");
    }
}
