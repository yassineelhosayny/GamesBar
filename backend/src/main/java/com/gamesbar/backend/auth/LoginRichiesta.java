package com.gamesbar.backend.auth;

import jakarta.validation.constraints.NotBlank;

//descrive il JSON ricevuto per il login.
//niente regole sulla lunghezza della password: qui si controlla solo che ci sia
public record LoginRichiesta(
    @NotBlank(message = "l'email è obbligatoria")
    String email,

    @NotBlank(message = "la password è obbligatoria")
    String password
){
    //come in RegistrazioneRichiesta: tolgo gli spazi dall'email prima della validazione
    public LoginRichiesta {
        if (email != null) {
            email = email.trim();
        }
    }
}
