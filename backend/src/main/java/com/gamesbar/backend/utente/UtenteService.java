package com.gamesbar.backend.utente;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class UtenteService {
    private final UtenteRepository utenteRepository;

    public UtenteService(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    //restituisce il profilo dell'utente che ha fatto il login
    public UtenteRisposta trovaProfilo(String email) {
        Optional<Utente> trovato = utenteRepository.findByEmail(email);

        //non dovrebbe succedere: il filtro JWT ha appena letto questo utente dal DB
        if (trovato.isEmpty()) {
            throw new IllegalStateException("utente non trovato: " + email);
        }

        return UtenteRisposta.daUtente(trovato.get());
    }
}
