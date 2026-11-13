package com.gamesbar.backend.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;
import com.gamesbar.backend.utente.UtenteRisposta;

@Service
public class AuthService {
    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder) {
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UtenteRisposta registra(RegistrazioneRichiesta richiesta) {
        String email = richiesta.email().trim().toLowerCase();

        if (utenteRepository.existsByEmail(email)) {
            throw new EmailGiaRegistrataException(email);
        }

        String hashPassword = passwordEncoder.encode(richiesta.password());

        //per ora il ruolo GIOCATORE e stato ATTIVO li imposta il costruttore di Utente
        Utente utente = new Utente(richiesta.nome(), richiesta.cognome(), email, hashPassword);
        Utente salvato = utenteRepository.save(utente);

        return new UtenteRisposta(
            salvato.getId(),
            salvato.getNome(),
            salvato.getCognome(),
            salvato.getEmail(),
            salvato.getRuolo()
        );
    }
}
