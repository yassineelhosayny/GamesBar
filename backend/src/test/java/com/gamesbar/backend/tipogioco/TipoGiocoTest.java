package com.gamesbar.backend.tipogioco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

//test puri dell'Entity TipoGioco: niente Spring, niente database
class TipoGiocoTest {

    @Test
    void nuovoTipoNasceNonAttivo() {
        TipoGioco tipo = new TipoGioco(7L, "CALCIO_BALILLA", "Calcio balilla", null, "1.0");

        assertFalse(tipo.isAttivo());
        assertEquals(7L, tipo.getAmministratore_id());
    }

    @Test
    void codiceSalvatoInMaiuscoloSenzaSpazi() {
        TipoGioco tipo = new TipoGioco(7L, "  calcio_balilla ", "Calcio balilla", null, null);

        assertEquals("CALCIO_BALILLA", tipo.getCodice());
    }

    @Test
    void codiceENomeObbligatori() {
        assertThrows(IllegalArgumentException.class, () -> new TipoGioco(7L, " ", "Calcio balilla", null, null));
        assertThrows(IllegalArgumentException.class, () -> new TipoGioco(7L, "CALCIO_BALILLA", null, null, null));
    }

    @Test
    void attivaEDisattiva() {
        TipoGioco tipo = new TipoGioco(7L, "CALCIO_BALILLA", "Calcio balilla", null, null);

        tipo.attiva();
        assertTrue(tipo.isAttivo());

        tipo.disattiva();
        assertFalse(tipo.isAttivo());
    }
}
