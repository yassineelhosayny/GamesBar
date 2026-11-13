package com.gamesbar.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//descrive il JSON ricevuto per la registrazione.
//ho tolto il ruolo ogni new utente è (sempre GIOCATORE) fichè super admin lo cambia
//le annotazioni vengono controllate con @Valid come nel progetto di stage
public record RegistrazioneRichiesta(
    @NotBlank(message = "il nome è obbligatorio")
    @Size(max = 80, message = "il nome può avere al massimo 80 caratteri")   //VARCHAR(80) nel DB
    String nome,

    @NotBlank(message = "il cognome è obbligatorio")
    @Size(max = 80, message = "il cognome può avere al massimo 80 caratteri")
    String cognome,

    @NotBlank(message = "l'email è obbligatoria")
    @Email(regexp = ".+@.+\\..{2,}", message = "l'email non è valida")  //come il CHECK del DB: almeno 2 caratteri dopo il punto
    @Size(max = 255, message = "l'email può avere al massimo 255 caratteri")
    String email,

    @NotBlank(message = "la password è obbligatoria")
    @Size(min = 8, max = 72, message = "la password deve avere tra 8 e 72 caratteri")  //BCrypt usa al massimo 72 byte
    String password
){
    //costruttore compatto: viene eseguito quando Spring crea l'oggetto dal JSON,
    //quindi prima della validazione. " mario@x.it " diventa "mario@x.it"
    public RegistrazioneRichiesta {
        if (email != null) {
            email = email.trim();
        }
    }
}
