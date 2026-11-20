package com.gamesbar.backend.tipogioco.evento;

//lanciata quando un tipo di evento non esiste o non esiste in quel gioco
public class EventoNonTrovatoException extends RuntimeException {

    public EventoNonTrovatoException(String messaggio) {
        super(messaggio);
    }
}
