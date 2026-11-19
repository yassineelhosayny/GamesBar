package com.gamesbar.backend.utente;

import jakarta.validation.constraints.NotNull;

//descrive il JSON per cambiare il ruolo di un utente: { "ruolo": "ADMIN_LOCALE" }
//se il testo non è uno dei valori di RuoloUtente, Spring risponde 400 
public record CambioRuoloRichiesta(
    @NotNull(message = "il ruolo è obbligatorio")
    RuoloUtente ruolo
){}
