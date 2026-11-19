package com.gamesbar.backend.locale;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

//verifica dei locali da parte dell ADMIN_PIATTAFORMA
@RestController
@RequestMapping("/api/admin/richieste-locali")
public class AdminLocaleController {
    private final LocaleService localeService;

    public AdminLocaleController(LocaleService localeService) {
        this.localeService = localeService;
    }

    //le richieste ancora da esaminare
    @GetMapping
    public List<RichiestaLocaleRisposta> richiesteInAttesa() {
        return localeService.trovaRichiesteInAttesa();
    }

    @PutMapping("/{id}/approva")
    public RichiestaLocaleRisposta approva(@PathVariable Long id, Authentication autenticazione) {
        return localeService.approvaRichiesta(id, autenticazione.getName());
    }

    @PutMapping("/{id}/rifiuta")
    public RichiestaLocaleRisposta rifiuta(@PathVariable Long id, @Valid @RequestBody RifiutoRichiesta richiesta,
                                           Authentication autenticazione) {
        return localeService.rifiutaRichiesta(id, richiesta.motivo(), autenticazione.getName());
    }
}
