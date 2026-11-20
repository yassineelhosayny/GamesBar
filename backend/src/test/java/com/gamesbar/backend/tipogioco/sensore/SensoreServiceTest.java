package com.gamesbar.backend.tipogioco.sensore;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gamesbar.backend.tipogioco.TipoGioco;
import com.gamesbar.backend.tipogioco.TipoGiocoRepository;
import com.gamesbar.backend.tipogioco.evento.EventoNonTrovatoException;
import com.gamesbar.backend.tipogioco.evento.TipoEventoGioco;
import com.gamesbar.backend.tipogioco.evento.TipoEventoGiocoRepository;

@ExtendWith(MockitoExtension.class)
class SensoreServiceTest {

    @Mock
    private TipoGiocoRepository tipoGiocoRepo;
    @Mock
    private TipoEventoGiocoRepository eventoRepository;
    @Mock
    private TipoSensoreRepository sensoreRepository;
    @InjectMocks
    private SensoreService sensoreService;

    //il tipo di gioco con id 1 è FRECCETTE (finto, perché l'id non ha setter)
    private TipoGioco freccette() {
        TipoGioco tipo = mock(TipoGioco.class);
        when(tipo.getId()).thenReturn(1L);
        when(tipo.getCodice()).thenReturn("FRECCETTE");
        when(tipoGiocoRepo.findById(1L)).thenReturn(Optional.of(tipo));
        return tipo;
    }

    @Test
    void creaSensoreCollegatoAUnEventoDelloStessoGioco() {
        freccette();
        TipoEventoGioco tiroRilevato = mock(TipoEventoGioco.class);
        when(tiroRilevato.getId()).thenReturn(2L);
        when(tiroRilevato.getTipoGiocoId()).thenReturn(1L);
        when(tiroRilevato.getCodice()).thenReturn("TIRO_RILEVATO");
        when(sensoreRepository.existsByTipoGiocoIdAndCodice(1L, "SECONDO_RILEVATORE")).thenReturn(false);
        when(eventoRepository.findByTipoGiocoIdAndCodice(1L, "TIRO_RILEVATO")).thenReturn(Optional.of(tiroRilevato));
        when(sensoreRepository.save(any(TipoSensore.class))).thenAnswer(invocazione -> invocazione.getArgument(0));

        TipoSensoreRisposta risposta = sensoreService.creaSensore(1L,
            new NuovoTipoSensoreRichiesta("secondo_rilevatore", "Secondo rilevatore", null, "tiro_rilevato", true));

        assertEquals("TIRO_RILEVATO", risposta.codiceEventoProdotto());
        assertTrue(risposta.obbligatorio());

        ArgumentCaptor<TipoSensore> captor = ArgumentCaptor.forClass(TipoSensore.class);
        verify(sensoreRepository).save(captor.capture());
        assertEquals(2L, captor.getValue().getTipoEventoProdottoId());
    }

    @Test
    void eventoCheIlGiocoNonHaNonVieneTrovato() {
        freccette();
        when(sensoreRepository.existsByTipoGiocoIdAndCodice(1L, "SENSORE_PORTA")).thenReturn(false);
        when(eventoRepository.findByTipoGiocoIdAndCodice(1L, "GOL_SEGNATO")).thenReturn(Optional.empty());

        assertThrows(EventoNonTrovatoException.class, () -> sensoreService.creaSensore(1L,
            new NuovoTipoSensoreRichiesta("sensore_porta", "Sensore porta", null, "gol_segnato", false)));

        verify(sensoreRepository, never()).save(any(TipoSensore.class));
    }
}
