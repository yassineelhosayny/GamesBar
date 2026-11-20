package com.gamesbar.backend.tipogioco.sensore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.gamesbar.backend.tipogioco.TipoGioco;
import com.gamesbar.backend.tipogioco.evento.TipoEventoGioco;

//test puri dell'Entity TipoSensore
class TipoSensoreTest {

    //gli id li decide il DB e non hanno setter: per avere id diversi uso oggetti finti
    private TipoGioco giocoConId(Long id) {
        TipoGioco gioco = mock(TipoGioco.class);
        when(gioco.getId()).thenReturn(id);
        when(gioco.getCodice()).thenReturn("FRECCETTE");
        return gioco;
    }

    private TipoEventoGioco eventoDelGioco(Long idGioco) {
        TipoEventoGioco evento = mock(TipoEventoGioco.class);
        when(evento.getTipoGiocoId()).thenReturn(idGioco);
        return evento;
    }

    @Test
    void nuovoSensoreAttivoConIDatiDelGioco() {
        TipoSensore sensore = new TipoSensore(giocoConId(1L), "rilevatore_tiro", "Rilevatore di tiro", null,
            eventoDelGioco(1L), true);

        assertTrue(sensore.isAttivo());
        assertTrue(sensore.isObbligatorio());
        assertEquals("RILEVATORE_TIRO", sensore.getCodice());
        assertEquals("FRECCETTE", sensore.getCodiceTipoGioco());
    }

    @Test
    void eventoDiUnAltroGiocoNonPermesso() {
        TipoGioco gioco = mock(TipoGioco.class);
        when(gioco.getId()).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
            () -> new TipoSensore(gioco, "SENSORE", "Sensore", null, eventoDelGioco(2L), false));
    }

    @Test
    void disattivaEAttiva() {
        TipoSensore sensore = new TipoSensore(giocoConId(1L), "SENSORE", "Sensore", null, eventoDelGioco(1L), false);

        sensore.disattiva();
        assertFalse(sensore.isAttivo());

        sensore.attiva();
        assertTrue(sensore.isAttivo());
    }
}
