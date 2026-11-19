package com.gamesbar.backend.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

//test puri dell'Entity Locale: niente Spring, niente database
class LocaleTest {

    private Locale nuovoLocale() {
        return new Locale(10L, "Bar Sport", "Bar con freccette", "Via Roma 1", "Vercelli", "13100", "vc", "0161 123456");
    }

    @Test
    void nuovoLocaleInAttesaEAttivo() {
        Locale locale = nuovoLocale();

        assertEquals(StatoVerificaLocale.IN_ATTESA, locale.getStatoVerifica());
        assertTrue(locale.isAttivo());
        assertEquals(10L, locale.getAmministratoreId());
    }

    @Test
    void provinciaSalvataInMaiuscolo() {
        Locale locale = nuovoLocale();

        assertEquals("VC", locale.getProvincia());
    }

    @Test
    void provinciaVuotaDiventaNull() {
        Locale locale = new Locale(10L, "Casa Rossi", null, "Via Verdi 3", "Roma", "00100", "  ", null);

        assertNull(locale.getProvincia());
    }

    @Test
    void nomeEIndirizzoObbligatori() {
        assertThrows(IllegalArgumentException.class,
            () -> new Locale(10L, " ", null, "Via Roma 1", "Vercelli", "13100", null, null));
        assertThrows(IllegalArgumentException.class,
            () -> new Locale(10L, "Bar Sport", null, null, "Vercelli", "13100", null, null));
    }

    @Test
    void modificaDatiCambiaSoloNomeDescrizioneETelefono() {
        Locale locale = nuovoLocale();

        locale.modificaDati("Bar Sport Nuovo", "Ora anche biliardo", "0161 999999");

        assertEquals("Bar Sport Nuovo", locale.getNome());
        assertEquals("Ora anche biliardo", locale.getDescrizione());
        assertEquals("0161 999999", locale.getNumeroTelefono());
        assertEquals("Via Roma 1", locale.getIndirizzo());
        assertEquals("Vercelli", locale.getCitta());
    }

    @Test
    void approvaERifiutaCambianoLoStato() {
        Locale approvato = nuovoLocale();
        approvato.approva();
        assertEquals(StatoVerificaLocale.APPROVATO, approvato.getStatoVerifica());

        Locale rifiutato = nuovoLocale();
        rifiutato.rifiuta();
        assertTrue(rifiutato.isRifiutato());
    }
}
