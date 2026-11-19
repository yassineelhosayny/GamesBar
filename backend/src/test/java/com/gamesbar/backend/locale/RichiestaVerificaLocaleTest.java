package com.gamesbar.backend.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

//test puri dell'Entity RichiestaVerificaLocale
class RichiestaVerificaLocaleTest {

    private RichiestaVerificaLocale nuovaRichiesta() {
        return new RichiestaVerificaLocale(5L, 10L, "Sono il titolare");
    }

    @Test
    void nuovaRichiestaInAttesaSenzaRevisore() {
        RichiestaVerificaLocale richiesta = nuovaRichiesta();

        assertTrue(richiesta.isInAttesa());
        assertNull(richiesta.getRevisoreId());
        assertNull(richiesta.getDataRevisione());
    }

    @Test
    void approvaSalvaRevisoreEData() {
        RichiestaVerificaLocale richiesta = nuovaRichiesta();

        richiesta.approva(1L);

        assertEquals(StatoRichiestaVerifica.APPROVATA, richiesta.getStato());
        assertEquals(1L, richiesta.getRevisoreId());
        assertNotNull(richiesta.getDataRevisione());
    }

    @Test
    void rifiutaSalvaIlMotivo() {
        RichiestaVerificaLocale richiesta = nuovaRichiesta();

        richiesta.rifiuta(1L, "indirizzo inesistente");

        assertEquals(StatoRichiestaVerifica.RIFIUTATA, richiesta.getStato());
        assertEquals("indirizzo inesistente", richiesta.getMotivoRifiuto());
    }

    @Test
    void rifiutoSenzaMotivoNonPermesso() {
        RichiestaVerificaLocale richiesta = nuovaRichiesta();

        assertThrows(IllegalArgumentException.class, () -> richiesta.rifiuta(1L, " "));
        assertTrue(richiesta.isInAttesa());
    }
}
