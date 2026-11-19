package com.gamesbar.backend.utente;

import java.util.ArrayList;
import java.util.List;
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

    // SOLO PER ADMIN_PIATTAFORMA

    //elenco di tutti gli utenti
    public List<UtenteRisposta> trovaTutti() {
        List<Utente> utenti = utenteRepository.findAllByOrderByIdAsc();

        List<UtenteRisposta> risposte = new ArrayList<>();
        for (Utente utente : utenti) {
            risposte.add(UtenteRisposta.daUtente(utente));
        }
        return risposte;
    }

    // cambia il stato
    public UtenteRisposta sospendi(Long id, String emailAdmin) {
        Utente utente = trovaPerId(id);
        controllaCheNonSiaSeStesso(utente, emailAdmin);

        utente.sospendi();
        return UtenteRisposta.daUtente(utenteRepository.save(utente));
    }

    public UtenteRisposta blocca(Long id, String emailAdmin) {
        Utente utente = trovaPerId(id);
        controllaCheNonSiaSeStesso(utente, emailAdmin);

        utente.blocca();
        return UtenteRisposta.daUtente(utenteRepository.save(utente));
    }

    public UtenteRisposta attiva(Long id, String emailAdmin) {
        Utente utente = trovaPerId(id);
        controllaCheNonSiaSeStesso(utente, emailAdmin);

        utente.attiva();
        return UtenteRisposta.daUtente(utenteRepository.save(utente));
    }
    //cambia il ruolo
    public UtenteRisposta cambiaRuolo(Long id, RuoloUtente ruolo, String emailAdmin) {
        Utente utente = trovaPerId(id);
        controllaCheNonSiaSeStesso(utente, emailAdmin);

        utente.assegnaRuolo(ruolo);
        return UtenteRisposta.daUtente(utenteRepository.save(utente));
    }

    //cerca l utente oppure lancia l eccezione che diventa 404
    private Utente trovaPerId(Long id) {
        Optional<Utente> trovato = utenteRepository.findById(id);
        if (trovato.isEmpty()) {
            throw new UtenteNonTrovatoException(id);
        }
        return trovato.get();
    }

    //un admin non può modificare il proprio account
    private void controllaCheNonSiaSeStesso(Utente utente, String emailAdmin) {
        if (utente.getEmail().equals(emailAdmin)) {
            throw new OperazioneNonPermessaException("non puoi modificare il stato del tuo stesso account.");
        }
    }
}
