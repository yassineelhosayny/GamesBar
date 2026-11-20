package com.gamesbar.backend.tipogioco.sensore;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

//sensori dei tipi di gioco, per ADMIN_GIOCO.
@RestController
@RequestMapping("/api/admin-gioco")
public class AdminSensoreController {
    private final SensoreService sensoreService;

    public AdminSensoreController(SensoreService sensoreService) {
        this.sensoreService = sensoreService;
    }

    @GetMapping("/tipi-gioco/{id}/sensori")
    public List<TipoSensoreRisposta> sensoriDelGioco(@PathVariable Long id) {
        return sensoreService.trovaSensori(id);
    }

    @PostMapping("/tipi-gioco/{id}/sensori")
    @ResponseStatus(HttpStatus.CREATED)
    public TipoSensoreRisposta creaSensore(@PathVariable Long id, @Valid @RequestBody NuovoTipoSensoreRichiesta richiesta) {
        return sensoreService.creaSensore(id, richiesta);
    }

    @PutMapping("/sensori/{id}/attiva")
    public TipoSensoreRisposta attivaSensore(@PathVariable Long id) {
        return sensoreService.attivaSensore(id);
    }

    @PutMapping("/sensori/{id}/disattiva")
    public TipoSensoreRisposta disattivaSensore(@PathVariable Long id) {
        return sensoreService.disattivaSensore(id);
    }
}
