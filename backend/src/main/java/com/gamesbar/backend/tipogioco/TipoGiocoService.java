package com.gamesbar.backend.tipogioco;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;

@Service
public class TipoGiocoService {
    private final TipoGiocoRepository tipoGiocoRepo;
    private final UtenteRepository utenteRepository;

    public TipoGiocoService(TipoGiocoRepository tipoGiocoRepo, UtenteRepository utenteRepository) {
        this.tipoGiocoRepo = tipoGiocoRepo;
        this.utenteRepository = utenteRepository;
    }

    public List<TipoGiocoRisposta> trovaTipiGiochiAttivi(){
        return tipoGiocoRepo.findByAttivoTrueOrderByNomeAsc().stream()  //stream() crea un flusso per elaborare gli elementi della lista uno alla volta
                .map(tipo_gioco -> TipoGiocoRisposta.daTipoGioco(tipo_gioco)) //map transforma tabella a class TipoGiocoRisposta
                .toList();
    }

    //Admin
    //tutti i tipi di gioco, anche quelli non attivi
    public List<TipoGiocoRisposta> trovaTutti() {
        List<TipoGioco> tipi = tipoGiocoRepo.findAllByOrderByNomeAsc();

        List<TipoGiocoRisposta> risposte = new ArrayList<>();
        for (TipoGioco tipo : tipi) {
            risposte.add(TipoGiocoRisposta.daTipoGioco(tipo));
        }
        return risposte;
    }

    //crea un nuovo tipo di gioco (NON attivo)
    public TipoGiocoRisposta creaTipo(NuovoTipoGiocoRichiesta richiesta, String emailAdmin) {
        String codice = richiesta.codice().trim().toUpperCase();
        if (tipoGiocoRepo.existsByCodice(codice)) {
            throw new CodiceGiaUsatoException("esiste già un tipo di gioco con codice " + codice + ".");
        }

        Utente admin = trovaUtentePerEmail(emailAdmin);
        TipoGioco tipo = new TipoGioco(admin.getId(), codice, richiesta.nome(),
            richiesta.descrizione(), richiesta.versioneGestore());

        return TipoGiocoRisposta.daTipoGioco(tipoGiocoRepo.save(tipo));
    }

    //un tipo attivo compare nella lista pubblica e si potrà installare nei locali
    public TipoGiocoRisposta attiva(Long id) {
        TipoGioco tipo = trovaPerId(id);
        tipo.attiva();
        return TipoGiocoRisposta.daTipoGioco(tipoGiocoRepo.save(tipo));
    }

    public TipoGiocoRisposta disattiva(Long id) {
        TipoGioco tipo = trovaPerId(id);
        tipo.disattiva();
        return TipoGiocoRisposta.daTipoGioco(tipoGiocoRepo.save(tipo));
    }

    private TipoGioco trovaPerId(Long id) {
        Optional<TipoGioco> trovato = tipoGiocoRepo.findById(id);
        if (trovato.isEmpty()) {
            throw new TipoGiocoNonTrovatoException(id);
        }
        return trovato.get();
    }

    //l'utente del token esiste sempre: lo ha appena letto il filtro JWT
    private Utente trovaUtentePerEmail(String email) {
        Optional<Utente> trovato = utenteRepository.findByEmail(email);
        if (trovato.isEmpty()) {
            throw new IllegalStateException("utente non trovato: " + email);
        }
        return trovato.get();
    }
}
