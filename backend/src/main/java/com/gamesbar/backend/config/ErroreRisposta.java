package com.gamesbar.backend.config;

import java.util.Map;

//descrive il JSON restituito quando qualcosa va storto.
//campi: nome del campo sbagliato → messaggio (vuoto se l'errore non riguarda un campo)
public record ErroreRisposta(
    String errore,
    Map<String, String> campi
){}
