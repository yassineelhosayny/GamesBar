package com.gamesbar.backend.tipogioco;

//lanciata quando l'id richiesto non corrisponde a nessun tipo di gioco
public class TipoGiocoNonTrovatoException extends RuntimeException {

    public TipoGiocoNonTrovatoException(Long id) {
        super("il tipo di gioco con id " + id + " non esiste.");
    }
}
