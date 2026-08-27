package com.gamesbar.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gamesbar.backend.dto.TipoGiocoRisposta;
import com.gamesbar.backend.entity.TipoGioco;
import com.gamesbar.backend.repository.TipoGiocoRepo;

@ExtendWith(MockitoExtension.class)
public class TipoGiocoServiceTest {

    @Mock
    private TipoGiocoRepo tipoGiocoRepo;
    @InjectMocks
    private TipoGiocoService tipoGiocoService;

    @Test
    void ControllaGiochiAttiviDisponibili(){
        final TipoGioco g1 = new TipoGioco("FRECCETTE","Freccette","Gioco delle freccette","0.1",true);
        //final TipoGioco g2 = new TipoGioco("BALILLA","balilla","Gioco Balilla","1.0",false);

        //prparazione del repositary
        when(tipoGiocoRepo.findByAttivoTrueOrderByNomeAsc()).thenReturn(List.of(g1));

        List<TipoGiocoRisposta> res = tipoGiocoService.trovaTipiGiochiAttivi();
        assertEquals(1,res.size());
        assertEquals("FRECCETTE",res.get(0).nome());
    }
}
