package com.gamesbar.backend.tipogioco;

public record TipoGiocoRisposta(  //descrive il JSON restituito dall’API.
    Long id,
    String codice,
    String nome,
    String descrizione,
    boolean isAttivo
){}