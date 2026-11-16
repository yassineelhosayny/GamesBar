package com.gamesbar.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;
import com.jayway.jsonpath.JsonPath;

//test di INTEGRAZIONE: avvia tutto il backend (sicurezza, servizi, PostgreSQL vero).
//serve il container PostgreSQL acceso.
//@Transactional: alla fine di ogni test tutto viene annullato, nel DB non resta niente
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginIntegrazioneTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtenteRepository utenteRepository;

    private static final String EMAIL = "integrazione@test.gamesbar.it";

    private void registraUtente() throws Exception {
        String json = """
            { "nome": "Anna", "cognome": "Verdi", "email": "integrazione@test.gamesbar.it", "password": "password123" }
            """;
        mockMvc.perform(post("/api/auth/registrazione").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated());
    }

    //fa il login e restituisce il token letto dal JSON di risposta
    private String loginEPrendiToken() throws Exception {
        String json = """
            { "email": "integrazione@test.gamesbar.it", "password": "password123" }
            """;
        MvcResult risultato = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andReturn();

        String rispostaJson = risultato.getResponse().getContentAsString();
        return JsonPath.read(rispostaJson, "$.token");
    }

    @Test
    void registrazioneLoginEProfiloFunzionanoInsieme() throws Exception {
        registraUtente();
        String token = loginEPrendiToken();

        mockMvc.perform(get("/api/utenti/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.ruolo").value("GIOCATORE"));
    }

    @Test
    void profiloSenzaTokenRestituisce401() throws Exception {
        mockMvc.perform(get("/api/utenti/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void profiloConTokenFalsoRestituisce401() throws Exception {
        mockMvc.perform(get("/api/utenti/me").header("Authorization", "Bearer token-inventato"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void utenteSospesoPerdeSubitoLAccesso() throws Exception {
        registraUtente();
        String token = loginEPrendiToken();

        //un amministratore sospende l'utente DOPO il login
        Utente utente = utenteRepository.findByEmail(EMAIL).orElseThrow();
        utente.sospendi();
        utenteRepository.saveAndFlush(utente);

        //lo stesso token di prima non basta più
        mockMvc.perform(get("/api/utenti/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tipiGiocoRestanoPubbliciSenzaToken() throws Exception {
        mockMvc.perform(get("/api/tipi-gioco/attive"))
            .andExpect(status().isOk());
    }
}
