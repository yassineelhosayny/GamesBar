package com.gamesbar.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gamesbar.backend.security.JwtService;
import com.gamesbar.backend.utente.RuoloUtente;
import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;
import com.gamesbar.backend.utente.UtenteRisposta;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UtenteRepository utenteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    private RegistrazioneRichiesta richiestaDiProva() {
        return new RegistrazioneRichiesta("Mario", "Rossi", "mario.rossi@example.com", "password123");
    }

    //prepara i mock per una registrazione che va a buon fine
    private void preparaRegistrazioneValida() {
        when(utenteRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASH_FINTO");
        //come il vero save(): restituisce l'utente che ha ricevuto
        when(utenteRepository.save(any(Utente.class))).thenAnswer(invocazione -> invocazione.getArgument(0));
    }

    @Test
    void registraSalvaSoloLHashDellaPassword() {
        preparaRegistrazioneValida();

        authService.registra(richiestaDiProva());

        //il captor "cattura" l'utente passato a save() per poterlo controllare
        ArgumentCaptor<Utente> captor = ArgumentCaptor.forClass(Utente.class);
        verify(utenteRepository).save(captor.capture());
        assertEquals("HASH_FINTO", captor.getValue().getHashPassword());
    }

    @Test
    void rispostaContieneDatiUtenteERuoloGiocatore() {
        preparaRegistrazioneValida();

        UtenteRisposta risposta = authService.registra(richiestaDiProva());

        assertEquals("Mario", risposta.nome());
        assertEquals("Rossi", risposta.cognome());
        assertEquals("mario.rossi@example.com", risposta.email());
        assertEquals(RuoloUtente.GIOCATORE, risposta.ruolo());
    }

    @Test
    void emailNormalizzataPrimaDelControlloDuplicati() {
        preparaRegistrazioneValida();
        RegistrazioneRichiesta richiesta =
            new RegistrazioneRichiesta("Mario", "Rossi", "  Mario.Rossi@Example.COM ", "password123");

        authService.registra(richiesta);

        verify(utenteRepository).existsByEmail("mario.rossi@example.com");
    }

    @Test
    void emailGiaRegistrataRifiutataSenzaSalvare() {
        when(utenteRepository.existsByEmail("mario.rossi@example.com")).thenReturn(true);

        assertThrows(EmailGiaRegistrataException.class, () -> authService.registra(richiestaDiProva()));

        verify(passwordEncoder, never()).encode(anyString());
        verify(utenteRepository, never()).save(any(Utente.class));
    }

    // ---------------- LOGIN ----------------

    private LoginRichiesta loginDiProva() {
        return new LoginRichiesta("mario.rossi@example.com", "password123");
    }

    private Utente utenteSalvato() {
        return new Utente("Mario", "Rossi", "mario.rossi@example.com", "HASH_FINTO");
    }

    @Test
    void loginCorrettoRestituisceTokenEUtente() {
        Utente utente = utenteSalvato();
        when(utenteRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("password123", "HASH_FINTO")).thenReturn(true);
        when(jwtService.creaToken(utente)).thenReturn("TOKEN_FINTO");

        AuthRisposta risposta = authService.login(loginDiProva());

        assertEquals("TOKEN_FINTO", risposta.token());
        assertEquals("mario.rossi@example.com", risposta.utente().email());
    }

    @Test
    void loginCorrettoSalvaLUltimoAccesso() {
        Utente utente = utenteSalvato();
        when(utenteRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("password123", "HASH_FINTO")).thenReturn(true);

        authService.login(loginDiProva());

        assertNotNull(utente.getDataUltimoAccesso());
        verify(utenteRepository).save(utente);
    }

    @Test
    void emailSconosciutaRifiutata() {
        when(utenteRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.empty());

        assertThrows(CredenzialiNonValideException.class, () -> authService.login(loginDiProva()));
    }

    @Test
    void passwordSbagliataRifiutataSenzaToken() {
        when(utenteRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(utenteSalvato()));
        when(passwordEncoder.matches("password123", "HASH_FINTO")).thenReturn(false);

        assertThrows(CredenzialiNonValideException.class, () -> authService.login(loginDiProva()));

        verify(jwtService, never()).creaToken(any(Utente.class));
    }

    @Test
    void utenteSospesoNonPuoEntrare() {
        Utente utente = utenteSalvato();
        utente.sospendi();
        when(utenteRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(utente));
        when(passwordEncoder.matches("password123", "HASH_FINTO")).thenReturn(true);

        assertThrows(UtenteNonAttivoException.class, () -> authService.login(loginDiProva()));

        verify(jwtService, never()).creaToken(any(Utente.class));
    }

    @Test
    void emailDelLoginNormalizzata() {
        when(utenteRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.empty());

        assertThrows(CredenzialiNonValideException.class,
            () -> authService.login(new LoginRichiesta("Mario.Rossi@Example.COM", "password123")));

        verify(utenteRepository).findByEmail("mario.rossi@example.com");
    }
}
