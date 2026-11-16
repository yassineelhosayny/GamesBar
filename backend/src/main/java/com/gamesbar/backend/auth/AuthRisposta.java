package com.gamesbar.backend.auth;

import com.gamesbar.backend.utente.UtenteRisposta;

//descrive il JSON restituito dopo un login riuscito:
//il token da usare nelle richieste successive + i dati dell'utente
public record AuthRisposta(
    String token,
    UtenteRisposta utente
){}
