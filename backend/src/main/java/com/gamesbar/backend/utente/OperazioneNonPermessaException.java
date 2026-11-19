package com.gamesbar.backend.utente;

//lanciata quando l'operazione è vietata da una regola dell'applicazione,
//per esempio un amministratore che prova a sospendere sé stesso
public class OperazioneNonPermessaException extends RuntimeException {

    public OperazioneNonPermessaException(String messaggio) {
        super(messaggio);
    }
}
