package com.gamesbar.backend.locale;

import jakarta.validation.constraints.NotBlank;

//drecord o json che discrive il motivo del rifuito
public record RifiutoRichiesta(
    @NotBlank(message = "il motivo del rifiuto è obbligatorio")
    String motivo
){}
