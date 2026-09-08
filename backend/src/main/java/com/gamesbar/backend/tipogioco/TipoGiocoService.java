package com.gamesbar.backend.tipogioco;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TipoGiocoService {
    private final TipoGiocoRepo tipoGiocoRepo;

    public TipoGiocoService(TipoGiocoRepo tipoGiocoRepo) {
        this.tipoGiocoRepo = tipoGiocoRepo;
    }
   
    public List<TipoGiocoRisposta> trovaTipiGiochiAttivi(){
        return tipoGiocoRepo.findByAttivoTrueOrderByNomeAsc().stream()
                .map(tipo_gioco -> new TipoGiocoRisposta(
                    tipo_gioco.getId(),
                    tipo_gioco.getCodice(),
                    tipo_gioco.getNome(),
                    tipo_gioco.getDescrizione(),
                    tipo_gioco.isAttivo()
                )).toList();
    }
}
