package com.gamesbar.backend.utente;

//descrive il JSON di un utente restituito dall'API.
//non contiene mai la password né il suo hash
public record UtenteRisposta(
    Long id,
    String nome,
    String cognome,
    String email,
    RuoloUtente ruolo
){}
