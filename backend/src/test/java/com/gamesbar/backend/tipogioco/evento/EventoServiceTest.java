package com.gamesbar.backend.tipogioco.evento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gamesbar.backend.tipogioco.CodiceGiaUsatoException;
import com.gamesbar.backend.tipogioco.TipoGioco;
import com.gamesbar.backend.tipogioco.TipoGiocoNonTrovatoException;
import com.gamesbar.backend.tipogioco.TipoGiocoRepository;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private TipoGiocoRepository tipoGiocoRepo;
    @Mock
    private TipoEventoGiocoRepository eventoRepository;
    @InjectMocks
    private EventoService eventoService;

    //il tipo di gioco con id 1 è FRECCETTE (finto, perché l'id non ha setter)
    private TipoGioco freccette() {
        TipoGioco tipo = mock(TipoGioco.class);
        when(tipo.getId()).thenReturn(1L);
        when(tipo.getCodice()).thenReturn("FRECCETTE");
        when(tipoGiocoRepo.findById(1L)).thenReturn(Optional.of(tipo));
        return tipo;
    }

    @Test
    void creaEventoCopiaIlCodiceDelGioco() {
        freccette();
        when(eventoRepository.existsByTipoGiocoIdAndCodice(1L, "PARTITA_SOSPESA")).thenReturn(false);
        when(eventoRepository.save(any(TipoEventoGioco.class))).thenAnswer(invocazione -> invocazione.getArgument(0));

        TipoEventoRisposta risposta = eventoService.creaEvento(1L,
            new NuovoTipoEventoRichiesta("partita_sospesa", "Partita sospesa", null));

        assertEquals("PARTITA_SOSPESA", risposta.codice());
        assertEquals("FRECCETTE", risposta.codiceTipoGioco());
        assertTrue(risposta.abilitata());
    }

    @Test
    void eventoDoppioneNelloStessoGiocoRifiutato() {
        freccette();
        when(eventoRepository.existsByTipoGiocoIdAndCodice(1L, "TIRO_RILEVATO")).thenReturn(true);

        assertThrows(CodiceGiaUsatoException.class, () -> eventoService.creaEvento(1L,
            new NuovoTipoEventoRichiesta("tiro_rilevato", "Doppione", null)));

        verify(eventoRepository, never()).save(any(TipoEventoGioco.class));
    }

    @Test
    void disabilitaEventoCambiaLaRegola() {
        TipoGioco gioco = new TipoGioco(null, "FRECCETTE", "Freccette", null, null);
        TipoEventoGioco evento = new TipoEventoGioco(gioco, "TIRO_RILEVATO", "Tiro rilevato", null);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(evento)).thenReturn(evento);

        TipoEventoRisposta risposta = eventoService.disabilitaEvento(5L);

        assertFalse(risposta.abilitata());
    }

    @Test
    void tipoGiocoInesistenteLanciaNonTrovato() {
        when(tipoGiocoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TipoGiocoNonTrovatoException.class, () -> eventoService.trovaEventi(99L));
    }
}
