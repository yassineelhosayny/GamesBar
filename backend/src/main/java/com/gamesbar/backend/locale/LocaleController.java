package com.gamesbar.backend.locale;

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

//locali dal punto di vista del gestore: serve il login (regola anyRequest().authenticated())
@RestController
@RequestMapping("/api/locali")
public class LocaleController {
    private final LocaleService localeService;

    public LocaleController(LocaleService localeService) {
        this.localeService = localeService;
    }

    //chi fa la richiesta diventa il gestore del locale (email presa dal token)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocaleRisposta creaLocale(@Valid @RequestBody NuovoLocaleRichiesta richiesta, Authentication autenticazione) {
        return localeService.creaLocale(richiesta, autenticazione.getName());
    }

    @GetMapping("/miei")
    public List<LocaleRisposta> mieiLocali(Authentication autenticazione) {
        return localeService.trovaMieiLocali(autenticazione.getName());
    }

    @PutMapping("/{id}")
    public LocaleRisposta modificaLocale(@PathVariable Long id, @Valid @RequestBody ModificaLocaleRichiesta richiesta,Authentication autenticazione) {
        return localeService.modificaLocale(id, richiesta, autenticazione.getName());
    }
}
