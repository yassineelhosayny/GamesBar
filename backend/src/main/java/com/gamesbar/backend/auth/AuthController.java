package com.gamesbar.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gamesbar.backend.utente.UtenteRisposta;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    //@RequestBody: trasforma il JSON ricevuto in RegistrazioneRichiesta
    //@Valid: controlla le annotazioni del DTO prima di entrare nel metodo
    @PostMapping("/registrazione")
    @ResponseStatus(HttpStatus.CREATED)     //201 N_U
    public UtenteRisposta registra(@Valid @RequestBody RegistrazioneRichiesta richiesta) {
        return authService.registra(richiesta);
    }
}
