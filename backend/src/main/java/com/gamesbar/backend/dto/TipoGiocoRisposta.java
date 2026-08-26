package com.gamesbar.backend.dto;

public record TipoGiocoRisposta(  //descrive il JSON restituito dall’API.
    Long id,
    String codice,
    String nome,
    String descrizione
){}