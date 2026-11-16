package com.gamesbar.backend.utente;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {
    private final UtenteService utenteService;

    public UtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    //endpoint protetto: senza token valido Spring Security risponde 401 prima di arrivare qui.
    //Authentication contiene l'utente messo dal filtro JWT; getName() restituisce la sua email
    @GetMapping("/me")
    public UtenteRisposta mioProfilo(Authentication autenticazione) {
        String email = autenticazione.getName();
        return utenteService.trovaProfilo(email);
    }
}
