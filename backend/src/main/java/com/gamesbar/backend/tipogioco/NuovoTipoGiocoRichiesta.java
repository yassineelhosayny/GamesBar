package com.gamesbar.backend.tipogioco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

//il JSON per creare un nuovo tipo di gioco.
public record NuovoTipoGiocoRichiesta(
    @NotBlank(message = "il codice è obbligatorio")
    @Size(max = 40, message = "il codice può avere al massimo 40 caratteri")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "il codice può contenere solo lettere, numeri e _ e deve iniziare con una lettera")
    String codice,

    @NotBlank(message = "il nome è obbligatorio")
    @Size(max = 100, message = "il nome può avere al massimo 100 caratteri")
    String nome,

    String descrizione,

    @Size(max = 20, message = "la versione può avere al massimo 20 caratteri")
    String versioneGestore
){}
