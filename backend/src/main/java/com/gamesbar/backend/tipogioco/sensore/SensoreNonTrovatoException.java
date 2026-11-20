package com.gamesbar.backend.tipogioco.sensore;

//lanciata quando l'id richiesto non corrisponde a nessun tipo di sensore
public class SensoreNonTrovatoException extends RuntimeException {

    public SensoreNonTrovatoException(Long id) {
        super("il tipo di sensore con id " + id + " non esiste.");
    }
}
