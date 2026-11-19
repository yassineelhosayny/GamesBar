package com.gamesbar.backend.utente;

//lanciata quando l'id richiesto non corrisponde a nessun utente
public class UtenteNonTrovatoException extends RuntimeException {

    public UtenteNonTrovatoException(Long id) {
        super("l'utente con id " + id + " non esiste.");
    }
}
