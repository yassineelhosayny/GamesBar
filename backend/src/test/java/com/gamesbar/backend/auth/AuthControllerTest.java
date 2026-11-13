package com.gamesbar.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gamesbar.backend.utente.RuoloUtente;
import com.gamesbar.backend.utente.UtenteRisposta;

//avvia solo la parte web (controller + GestoreErrori), senza server e senza database
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    //simula le richieste HTTP, come farebbe Postman
    @Autowired
    private MockMvc mockMvc;

    //il vero AuthService non viene creato: Spring mette al suo posto un mock
    @MockitoBean
    private AuthService authService;

    private static final String URL = "/api/auth/registrazione";

    private static final String JSON_VALIDO = """
        {
          "nome": "Mario",
          "cognome": "Rossi",
          "email": "mario.rossi@example.com",
          "password": "password123"
        }
        """;

    @Test
    void registrazioneValidaRestituisce201ConUtente() throws Exception {
        UtenteRisposta risposta = new UtenteRisposta(1L, "Mario", "Rossi", "mario.rossi@example.com", RuoloUtente.GIOCATORE);
        when(authService.registra(any(RegistrazioneRichiesta.class))).thenReturn(risposta);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("mario.rossi@example.com"))
            .andExpect(jsonPath("$.ruolo").value("GIOCATORE"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.hashPassword").doesNotExist());
    }

    @Test
    void emailConSpaziAccettataEPassataSenzaSpazi() throws Exception {
        String json = """
            { "nome": "Mario", "cognome": "Rossi", "email": "  mario.rossi@example.com ", "password": "password123" }
            """;
        UtenteRisposta risposta = new UtenteRisposta(1L, "Mario", "Rossi", "mario.rossi@example.com", RuoloUtente.GIOCATORE);
        when(authService.registra(any(RegistrazioneRichiesta.class))).thenReturn(risposta);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated());

        //controlla cosa è arrivato al Service: l'email senza spazi
        ArgumentCaptor<RegistrazioneRichiesta> captor = ArgumentCaptor.forClass(RegistrazioneRichiesta.class);
        verify(authService).registra(captor.capture());
        assertEquals("mario.rossi@example.com", captor.getValue().email());
    }

    @Test
    void emailNonValidaRestituisce400SenzaChiamareIlService() throws Exception {
        String json = """
            { "nome": "Mario", "cognome": "Rossi", "email": "non-una-email", "password": "password123" }
            """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errore").value("dati non validi"))
            .andExpect(jsonPath("$.campi.email").exists());

        verify(authService, never()).registra(any(RegistrazioneRichiesta.class));
    }

    @Test
    void passwordCortaRestituisce400() throws Exception {
        String json = """
            { "nome": "Mario", "cognome": "Rossi", "email": "mario.rossi@example.com", "password": "corta" }
            """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campi.password").value("la password deve avere tra 8 e 72 caratteri"));
    }

    @Test
    void nomeMancanteRestituisce400() throws Exception {
        String json = """
            { "cognome": "Rossi", "email": "mario.rossi@example.com", "password": "password123" }
            """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campi.nome").value("il nome è obbligatorio"));
    }

    @Test
    void emailGiaRegistrataRestituisce409() throws Exception {
        when(authService.registra(any(RegistrazioneRichiesta.class)))
            .thenThrow(new EmailGiaRegistrataException("mario.rossi@example.com"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errore").value("l'email mario.rossi@example.com è già registrata."));
    }
}
