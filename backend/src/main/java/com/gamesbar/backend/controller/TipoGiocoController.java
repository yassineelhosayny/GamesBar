package com.gamesbar.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gamesbar.backend.dto.TipoGiocoRisposta;
import com.gamesbar.backend.service.TipoGiocoService;

@RestController
@RequestMapping("/api/tipi-gioco")
public class TipoGiocoController {
    private final TipoGiocoService tipoGiocoService;

    public TipoGiocoController(TipoGiocoService tipiGiocoService){
        this.tipoGiocoService = tipiGiocoService;
    }

    @GetMapping("/attive")
    public List<TipoGiocoRisposta> trovaTipiGiochiAttivi(){
        return tipoGiocoService.trovaTipiGiochiAttivi();
    }
    
}
