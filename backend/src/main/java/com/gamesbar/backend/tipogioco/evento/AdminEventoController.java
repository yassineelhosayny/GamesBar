package com.gamesbar.backend.tipogioco.evento;

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

//eventi dei tipi di gioco, per ADMIN_GIOCO.
@RestController
@RequestMapping("/api/admin-gioco")
public class AdminEventoController {
    private final EventoService eventoService;

    public AdminEventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/tipi-gioco/{id}/eventi")
    public List<TipoEventoRisposta> eventiDelGioco(@PathVariable Long id) {
        return eventoService.trovaEventi(id);
    }

    @PostMapping("/tipi-gioco/{id}/eventi")
    @ResponseStatus(HttpStatus.CREATED)
    public TipoEventoRisposta creaEvento(@PathVariable Long id, @Valid @RequestBody NuovoTipoEventoRichiesta richiesta) {
        return eventoService.creaEvento(id, richiesta);
    }

    @PutMapping("/eventi/{id}/abilita")
    public TipoEventoRisposta abilitaEvento(@PathVariable Long id) {
        return eventoService.abilitaEvento(id);
    }

    @PutMapping("/eventi/{id}/disabilita")
    public TipoEventoRisposta disabilitaEvento(@PathVariable Long id) {
        return eventoService.disabilitaEvento(id);
    }
}
