package com.gamesbar.backend.tipogioco;

//lanciata quando un codice esiste già un tipo di gioco,
//anche un evento o un sensore dentro lo stesso gioco.
public class CodiceGiaUsatoException extends RuntimeException {

    public CodiceGiaUsatoException(String messaggio) {
        super(messaggio);
    }
}
