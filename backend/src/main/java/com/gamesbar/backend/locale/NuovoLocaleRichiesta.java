package com.gamesbar.backend.locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

//il JSON ricevuto per creare un locale.
public record NuovoLocaleRichiesta(
    @NotBlank(message = "il nome è obbligatorio")
    @Size(max = 150, message = "nome troppo lungo; può avere al massimo 150 caratteri")
    String nome,

    String descrizione,

    @NotBlank(message = "l'indirizzo è obbligatorio")
    @Size(max = 255, message = "indirizzo trppo lungo; può avere al massimo 255 caratteri")
    String indirizzo,

    @NotBlank(message = "la città è obbligatoria")
    @Size(max = 100, message = "città può avere al massimo 100 caratteri")
    String citta,

    @NotBlank(message = "il codice postale è obbligatorio")
    @Size(max = 10, message = "codice postale può avere al massimo 10 caratteri")
    String codicePostale,

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "la provincia deve avere 2 lettere")
    String provincia,

    @Size(max = 30, message = "il telefono può avere al massimo 30 caratteri")
    String numeroTelefono,

    String messaggio
){}
