package com.gamesbar.backend.auth;

//lanciata quando la password è giusta ma l'utente è SOSPESO o BLOCCATO
public class UtenteNonAttivoException extends RuntimeException {

    public UtenteNonAttivoException() {
        super("l'account è sospeso o bloccato.");
    }
}
