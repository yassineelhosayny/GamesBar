package com.gamesbar.backend.utente;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

//gestione degli utenti da parte dell'amministratore della piattaforma.
//il controllo del ruolo ADMIN_PIATTAFORMA è in SecurityConfig ("/api/admin/**")
@RestController
@RequestMapping("/api/admin/utenti")
public class AdminUtenteController {
    private final UtenteService utenteService;

    public AdminUtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @GetMapping
    public List<UtenteRisposta> elencoUtenti() {
        return utenteService.trovaTutti();
    }

    //@PathVariable: prende l'{id} scritto nell'indirizzo, es. /api/admin/utenti/5/sospendi → id = 5
    @PutMapping("/{id}/sospendi")
    public UtenteRisposta sospendi(@PathVariable Long id, Authentication autenticazione) {
        return utenteService.sospendi(id, autenticazione.getName());
    }

    @PutMapping("/{id}/blocca")
    public UtenteRisposta blocca(@PathVariable Long id, Authentication autenticazione) {
        return utenteService.blocca(id, autenticazione.getName());
    }

    @PutMapping("/{id}/attiva")
    public UtenteRisposta attiva(@PathVariable Long id, Authentication autenticazione) {
        return utenteService.attiva(id, autenticazione.getName());
    }

    @PutMapping("/{id}/ruolo")
    public UtenteRisposta cambiaRuolo(@PathVariable Long id, @Valid @RequestBody CambioRuoloRichiesta richiesta,
                                      Authentication autenticazione) {
        return utenteService.cambiaRuolo(id, richiesta.ruolo(), autenticazione.getName());
    }
}
