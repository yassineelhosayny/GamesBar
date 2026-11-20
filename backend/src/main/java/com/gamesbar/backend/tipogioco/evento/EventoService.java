package com.gamesbar.backend.tipogioco.evento;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gamesbar.backend.tipogioco.CodiceGiaUsatoException;
import com.gamesbar.backend.tipogioco.TipoGioco;
import com.gamesbar.backend.tipogioco.TipoGiocoNonTrovatoException;
import com.gamesbar.backend.tipogioco.TipoGiocoRepository;

//regole di registrazione eventi di ogni tipo di gioco, sono gestiti dall'admin del gioco
@Service
public class EventoService {
    private final TipoGiocoRepository tipoGiocoRepo;
    private final TipoEventoGiocoRepository eventoRepository;

    public EventoService(TipoGiocoRepository tipoGiocoRepo, TipoEventoGiocoRepository eventoRepository) {
        this.tipoGiocoRepo = tipoGiocoRepo;
        this.eventoRepository = eventoRepository;
    }

    public List<TipoEventoRisposta> trovaEventi(Long idTipoGioco) {
        TipoGioco tipo = trovaTipoGioco(idTipoGioco);
        List<TipoEventoGioco> eventi = eventoRepository.findByTipoGiocoIdOrderByCodiceAsc(tipo.getId());

        List<TipoEventoRisposta> risposte = new ArrayList<>();

        for (TipoEventoGioco evento : eventi) {
            risposte.add(TipoEventoRisposta.daEvento(evento));
        }
        return risposte;
    }

    public TipoEventoRisposta creaEvento(Long idTipoGioco, NuovoTipoEventoRichiesta richiesta) {
        TipoGioco tipo = trovaTipoGioco(idTipoGioco);
        String codice = richiesta.codice().trim().toUpperCase();

        //lo stesso codice può esistere in giochi diversi, ma non due volte nello stesso gioco
        if (eventoRepository.existsByTipoGiocoIdAndCodice(tipo.getId(), codice)) {
            throw new CodiceGiaUsatoException("il gioco " + tipo.getCodice() + " ha già un evento con codice " + codice + ".");
        }

        TipoEventoGioco evento = new TipoEventoGioco(tipo, codice, richiesta.nome(), richiesta.descrizione());
        return TipoEventoRisposta.daEvento(eventoRepository.save(evento));
    }

    //evento verrà registrato durante le partite
    public TipoEventoRisposta abilitaEvento(Long idEvento) {
        TipoEventoGioco evento = trovaEvento(idEvento);
        evento.abilita();
        return TipoEventoRisposta.daEvento(eventoRepository.save(evento));
    }

    //evento non verrà più registrato
    public TipoEventoRisposta disabilitaEvento(Long idEvento) {
        TipoEventoGioco evento = trovaEvento(idEvento);
        evento.disabilita();
        return TipoEventoRisposta.daEvento(eventoRepository.save(evento));
    }

    private TipoGioco trovaTipoGioco(Long id) {
        Optional<TipoGioco> trovato = tipoGiocoRepo.findById(id);
        if (trovato.isEmpty()) {
            throw new TipoGiocoNonTrovatoException(id);
        }
        return trovato.get();
    }

    private TipoEventoGioco trovaEvento(Long id) {
        Optional<TipoEventoGioco> trovato = eventoRepository.findById(id);
        if (trovato.isEmpty()) {
            throw new EventoNonTrovatoException("il tipo di evento con id " + id + " non esiste.");
        }
        return trovato.get();
    }
}
