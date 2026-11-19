package com.gamesbar.backend.locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//il JSON per modificare un locale: pò cambiare solo nome,descrizione,telefono.
public record ModificaLocaleRichiesta(
    @NotBlank(message = "il nome è obbligatorio")
    @Size(max = 150, message = "il nome può avere al massimo 150 caratteri")
    String nome,

    String descrizione,

    @Size(max = 30, message = "il telefono può avere al massimo 30 caratteri")
    String numeroTelefono
){}
