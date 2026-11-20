package com.gamesbar.backend.tipogioco.evento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gamesbar.backend.tipogioco.TipoGioco;

//test puri dell'Entity TipoEventoGioco
class TipoEventoGiocoTest {

    private final TipoGioco freccette = new TipoGioco(null, "FRECCETTE", "Freccette", null, null);

    @Test
    void nuovoEventoAbilitatoConIlCodiceDelGioco() {
        TipoEventoGioco evento = new TipoEventoGioco(freccette, "tiro_rilevato", "Tiro rilevato", null);

        assertTrue(evento.isAbilitata());
        assertEquals("TIRO_RILEVATO", evento.getCodice());
        assertEquals("FRECCETTE", evento.getCodiceTipoGioco());
    }

    @Test
    void codiceENomeObbligatori() {
        assertThrows(IllegalArgumentException.class, () -> new TipoEventoGioco(freccette, " ", "Tiro rilevato", null));
        assertThrows(IllegalArgumentException.class, () -> new TipoEventoGioco(freccette, "TIRO_RILEVATO", null, null));
    }

    @Test
    void disabilitaEAbilita() {
        TipoEventoGioco evento = new TipoEventoGioco(freccette, "TIRO_RILEVATO", "Tiro rilevato", null);

        evento.disabilita();
        assertFalse(evento.isAbilitata());

        evento.abilita();
        assertTrue(evento.isAbilitata());
    }
}
