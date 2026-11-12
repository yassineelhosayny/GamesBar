package com.gamesbar.backend.tipogioco;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TipoGiocoService {
    private final TipoGiocoRepository tipoGiocoRepo;

    public TipoGiocoService(TipoGiocoRepository tipoGiocoRepo) {
        this.tipoGiocoRepo = tipoGiocoRepo;
    }
   
    public List<TipoGiocoRisposta> trovaTipiGiochiAttivi(){
        return tipoGiocoRepo.findByAttivoTrueOrderByNomeAsc().stream()  //stream() crea un flusso per elaborare gli elementi della lista uno alla volta
                .map(tipo_gioco -> new TipoGiocoRisposta( //map transforma tabella a class TipoGiocoRisposta
                    tipo_gioco.getId(),
                    tipo_gioco.getCodice(),
                    tipo_gioco.getNome(),
                    tipo_gioco.getDescrizione(),
                    tipo_gioco.isAttivo()
                )).toList();
    }
}
