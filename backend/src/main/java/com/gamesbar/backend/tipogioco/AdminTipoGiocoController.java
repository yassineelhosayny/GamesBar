package com.gamesbar.backend.tipogioco;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

//gestione dei tipi di gioco da parte dell'amministratore del gioco.
@RestController
@RequestMapping("/api/admin-gioco/tipi-gioco")
public class AdminTipoGiocoController {
    private final TipoGiocoService tipoGiocoService;

    public AdminTipoGiocoController(TipoGiocoService tipoGiocoService) {
        this.tipoGiocoService = tipoGiocoService;
    }

    @GetMapping
    public List<TipoGiocoRisposta> tuttiITipi() {
        return tipoGiocoService.trovaTutti();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoGiocoRisposta creaTipo(@Valid @RequestBody NuovoTipoGiocoRichiesta richiesta, Authentication autenticazione) {
        return tipoGiocoService.creaTipo(richiesta, autenticazione.getName());
    }

    @PutMapping("/{id}/attiva")
    public TipoGiocoRisposta attiva(@PathVariable Long id) {
        return tipoGiocoService.attiva(id);
    }

    @PutMapping("/{id}/disattiva")
    public TipoGiocoRisposta disattiva(@PathVariable Long id) {
        return tipoGiocoService.disattiva(id);
    }
}
