package com.gamesbar.backend.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gamesbar.backend.security.JwtService;
import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;
import com.gamesbar.backend.utente.UtenteRisposta;

@Service
public class AuthService {
    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        return UtenteRisposta.daUtente(salvato);
    }

    public AuthRisposta login(LoginRichiesta richiesta) {
        String email = richiesta.email().trim().toLowerCase();

        //email esiste
        Optional<Utente> trovato = utenteRepository.findByEmail(email);
        if (trovato.isEmpty()) {
            throw new CredenzialiNonValideException();
        }
        Utente utente = trovato.get();

        //verifico se password è giusto con hash
        if (!passwordEncoder.matches(richiesta.password(), utente.getHashPassword())) {
            throw new CredenzialiNonValideException();
        }

        //utente attivo 
        //non scopre se l'account è bloccato)
        if (!utente.isAttivo()) {
            throw new UtenteNonAttivoException();
        }

        //ora di accesso e la creazione del token dura 24h per ora
        utente.registraAccesso();
        utenteRepository.save(utente);

        String token = jwtService.creaToken(utente);
        return new AuthRisposta(token, UtenteRisposta.daUtente(utente));
    }
}
