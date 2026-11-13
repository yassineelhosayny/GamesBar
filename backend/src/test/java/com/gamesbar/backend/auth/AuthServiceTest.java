package com.gamesbar.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
