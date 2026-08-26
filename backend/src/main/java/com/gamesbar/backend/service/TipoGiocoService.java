package com.gamesbar.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gamesbar.backend.dto.TipoGiocoRisposta;
import com.gamesbar.backend.repository.TipoGiocoRepo;

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
            tipo_gioco.getDescrizione()
        )).toList();
    }
}
