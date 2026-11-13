package com.gamesbar.backend.auth;

//lanciata quando si prova a registrare un'email che esiste già
public class EmailGiaRegistrataException extends RuntimeException {

    public EmailGiaRegistrataException(String email) {
        super("l'email " + email + " è già registrata.");
    }
}
