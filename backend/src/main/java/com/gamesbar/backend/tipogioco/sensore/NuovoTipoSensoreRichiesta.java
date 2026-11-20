package com.gamesbar.backend.tipogioco.sensore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

//il JSON per aggiungere un tipo di sensore a un gioco (nasce attivo).
public record NuovoTipoSensoreRichiesta(
    @NotBlank(message = "il codice è obbligatorio")
    @Size(max = 60, message = "il codice può avere al massimo 60 caratteri")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "il codice può contenere solo lettere, numeri e _ e deve iniziare con una lettera")
    String codice,

    @NotBlank(message = "il nome è obbligatorio")
    @Size(max = 100, message = "il nome può avere al massimo 100 caratteri")
    String nome,

    String descrizione,

    @NotBlank(message = "il codice dell'evento prodotto è obbligatorio")
    String codiceEventoProdotto,

    //se manca nel JSON vale false
    boolean obbligatorio
){}
