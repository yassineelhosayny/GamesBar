package com.gamesbar.backend.tipogioco.sensore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gamesbar.backend.tipogioco.CodiceGiaUsatoException;
import com.gamesbar.backend.tipogioco.TipoGioco;
import com.gamesbar.backend.tipogioco.TipoGiocoNonTrovatoException;
import com.gamesbar.backend.tipogioco.TipoGiocoRepository;
import com.gamesbar.backend.tipogioco.evento.EventoNonTrovatoException;
import com.gamesbar.backend.tipogioco.evento.TipoEventoGioco;
import com.gamesbar.backend.tipogioco.evento.TipoEventoGiocoRepository;

//sensori di ogni tipo di gioco, sono gestiti dall'admin_giococ
//usa anche gli eventi, perché ogni sensore produce un evento dello stesso gioco
@Service
public class SensoreService {
    private final TipoGiocoRepository tipoGiocoRepo;
    private final TipoEventoGiocoRepository eventoRepository;
    private final TipoSensoreRepository sensoreRepository;

    public SensoreService(TipoGiocoRepository tipoGiocoRepo, TipoEventoGiocoRepository eventoRepository,TipoSensoreRepository sensoreRepository) {
        this.tipoGiocoRepo = tipoGiocoRepo;
        this.eventoRepository = eventoRepository;
        this.sensoreRepository = sensoreRepository;
    }

    public List<TipoSensoreRisposta> trovaSensori(Long idTipoGioco) {
        TipoGioco tipo = trovaTipoGioco(idTipoGioco);
        List<TipoSensore> sensori = sensoreRepository.findByTipoGiocoIdOrderByCodiceAsc(tipo.getId());

        List<TipoSensoreRisposta> risposte = new ArrayList<>();
        for (TipoSensore sensore : sensori) {
            risposte.add(rispostaSensore(sensore));
        }
        return risposte;
    }

    public TipoSensoreRisposta creaSensore(Long idTipoGioco, NuovoTipoSensoreRichiesta richiesta) {
        TipoGioco tipo = trovaTipoGioco(idTipoGioco);
        String codice = richiesta.codice().trim().toUpperCase();

        if (sensoreRepository.existsByTipoGiocoIdAndCodice(tipo.getId(), codice)) {
            throw new CodiceGiaUsatoException("il gioco " + tipo.getCodice() + " ha già un sensore con codice " + codice + ".");
        }

        //l'evento prodotto si cerca solo dentro questo gioco: un evento di un altro gioco non viene trovato
        String codiceEvento = richiesta.codiceEventoProdotto().trim().toUpperCase();
        Optional<TipoEventoGioco> trovato = eventoRepository.findByTipoGiocoIdAndCodice(tipo.getId(), codiceEvento);
        if (trovato.isEmpty()) {
            throw new EventoNonTrovatoException("il gioco " + tipo.getCodice() + " non ha un evento con codice " + codiceEvento + ".");
        }
        TipoEventoGioco evento = trovato.get();

        TipoSensore sensore = new TipoSensore(tipo, codice, richiesta.nome(), richiesta.descrizione(),evento, richiesta.obbligatorio());
        TipoSensore salvato = sensoreRepository.save(sensore);
        return TipoSensoreRisposta.daSensore(salvato, evento.getCodice());
    }

    public TipoSensoreRisposta attivaSensore(Long idSensore) {
        TipoSensore sensore = trovaSensore(idSensore);
        sensore.attiva();
        return rispostaSensore(sensoreRepository.save(sensore));
    }

    public TipoSensoreRisposta disattivaSensore(Long idSensore) {
        TipoSensore sensore = trovaSensore(idSensore);
        sensore.disattiva();
        return rispostaSensore(sensoreRepository.save(sensore));
    }

    //nel sensore c'è solo l'id dell'evento=> cerco l'evento per mettere il suo codice nella risposta
    private TipoSensoreRisposta rispostaSensore(TipoSensore sensore) {
        Optional<TipoEventoGioco> trovato = eventoRepository.findById(sensore.getTipoEventoProdottoId());
        if (trovato.isEmpty()) {
            throw new EventoNonTrovatoException("il tipo di evento con id " + sensore.getTipoEventoProdottoId() + " non esiste.");
        }
        return TipoSensoreRisposta.daSensore(sensore, trovato.get().getCodice());
    }

    private TipoGioco trovaTipoGioco(Long id) {
        Optional<TipoGioco> trovato = tipoGiocoRepo.findById(id);
        if (trovato.isEmpty()) {
            throw new TipoGiocoNonTrovatoException(id);
        }
        return trovato.get();
    }

    private TipoSensore trovaSensore(Long id) {
        Optional<TipoSensore> trovato = sensoreRepository.findById(id);
        if (trovato.isEmpty()) {
            throw new SensoreNonTrovatoException(id);
        }
        return trovato.get();
    }
}
