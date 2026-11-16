package com.gamesbar.backend.utente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

//test puri dell'Entity: niente Spring, niente database
class UtenteTest {

    private static final String HASH = "$2a$10$hashDiProva";

    private Utente nuovoUtente() {
        return new Utente("Mario", "Rossi", "mario.rossi@example.com", HASH);
    }

    @Test
    void nomeECognomeSonoObbligatori() {
        assertThrows(IllegalArgumentException.class, () -> new Utente(null, "Rossi", "mario@example.com", HASH));
        assertThrows(IllegalArgumentException.class, () -> new Utente("  ", "Rossi", "mario@example.com", HASH));
        assertThrows(IllegalArgumentException.class, () -> new Utente("Mario", null, "mario@example.com", HASH));
        assertThrows(IllegalArgumentException.class, () -> new Utente("Mario", "", "mario@example.com", HASH));
    }

    @Test
    void emailObbligatoria() {
        assertThrows(IllegalArgumentException.class, () -> new Utente("Mario", "Rossi", null, HASH));
        assertThrows(IllegalArgumentException.class, () -> new Utente("Mario", "Rossi", "   ", HASH));
    }

    @Test
    void emailNormalizzataInMinuscoloESenzaSpazi() {
        Utente utente = new Utente("Mario", "Rossi", "  Mario.Rossi@Example.COM  ", HASH);

        assertEquals("mario.rossi@example.com", utente.getEmail());
    }

    @Test
    void hashPasswordObbligatorio() {
        assertThrows(IllegalArgumentException.class, () -> new Utente("Mario", "Rossi", "mario@example.com", null));
        assertThrows(IllegalArgumentException.class, () -> new Utente("Mario", "Rossi", "mario@example.com", " "));
    }

    @Test
    void nuovoUtenteEGiocatoreAttivo() {
        Utente utente = nuovoUtente();

        assertEquals(RuoloUtente.GIOCATORE, utente.getRuolo());
        assertEquals(StatoUtente.ATTIVO, utente.getStato());
        assertTrue(utente.isAttivo());
    }

    @Test
    void cambiDiStato() {
        Utente utente = nuovoUtente();

        utente.sospendi();
        assertEquals(StatoUtente.SOSPESO, utente.getStato());
        assertFalse(utente.isAttivo());

        utente.blocca();
        assertEquals(StatoUtente.BLOCCATO, utente.getStato());
        assertFalse(utente.isAttivo());

        utente.attiva();
        assertEquals(StatoUtente.ATTIVO, utente.getStato());
        assertTrue(utente.isAttivo());
    }

    @Test
    void assegnaRuolo() {
        Utente utente = nuovoUtente();

        utente.assegnaRuolo(RuoloUtente.ADMIN_LOCALE);

        assertEquals(RuoloUtente.ADMIN_LOCALE, utente.getRuolo());
    }

    @Test
    void ruoloNulloRifiutatoERuoloPrecedenteMantenuto() {
        Utente utente = nuovoUtente();

        assertThrows(IllegalArgumentException.class, () -> utente.assegnaRuolo(null));
        assertEquals(RuoloUtente.GIOCATORE, utente.getRuolo());
    }

    @Test
    void registraAccessoSalvaLOraDellUltimoAccesso() {
        Utente utente = nuovoUtente();
        assertNull(utente.getDataUltimoAccesso());

        utente.registraAccesso();

        assertNotNull(utente.getDataUltimoAccesso());
    }
}
