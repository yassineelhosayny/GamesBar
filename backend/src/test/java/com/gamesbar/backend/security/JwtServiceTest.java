package com.gamesbar.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.gamesbar.backend.utente.Utente;

//test puri del JwtService: niente Spring, la chiave la passiamo noi
class JwtServiceTest {

    private static final String CHIAVE_DI_TEST = "chiave-di-test-lunga-almeno-32-caratteri-123456";

    private final Utente utente = new Utente("Mario", "Rossi", "mario.rossi@example.com", "HASH_FINTO");

    @Test
    void tokenValidoContieneLEmail() {
        JwtService jwtService = new JwtService(CHIAVE_DI_TEST, 60);

        String token = jwtService.creaToken(utente);

        assertEquals("mario.rossi@example.com", jwtService.leggiEmail(token));
    }

    @Test
    void tokenModificatoNonValido() {
        JwtService jwtService = new JwtService(CHIAVE_DI_TEST, 60);
        String token = jwtService.creaToken(utente);

        //aggiungo un carattere: la firma non corrisponde più
        assertNull(jwtService.leggiEmail(token + "x"));
    }

    @Test
    void tokenFirmatoConUnAltraChiaveNonValido() {
        JwtService altroServer = new JwtService("una-chiave-diversa-lunga-almeno-32-caratteri-99", 60);
        String tokenFalso = altroServer.creaToken(utente);

        JwtService jwtService = new JwtService(CHIAVE_DI_TEST, 60);

        assertNull(jwtService.leggiEmail(tokenFalso));
    }

    @Test
    void tokenScadutoNonValido() {
        //durata -1 minuti: il token è già scaduto quando viene creato
        JwtService jwtService = new JwtService(CHIAVE_DI_TEST, -1);
        String token = jwtService.creaToken(utente);

        assertNull(jwtService.leggiEmail(token));
    }

    @Test
    void testoCasualeNonValido() {
        JwtService jwtService = new JwtService(CHIAVE_DI_TEST, 60);

        assertNull(jwtService.leggiEmail("questo-non-e-un-token"));
    }
}
