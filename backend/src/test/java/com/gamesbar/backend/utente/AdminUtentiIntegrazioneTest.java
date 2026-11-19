package com.gamesbar.backend.utente;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

//test di INTEGRAZIONE dell'area amministratore: sicurezza vera + PostgreSQL.
//serve il container PostgreSQL acceso; @Transactional annulla tutto alla fine di ogni test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUtentiIntegrazioneTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String EMAIL_ADMIN = "admin.integrazione@test.gamesbar.it";
    private static final String EMAIL_GIOCATORE = "giocatore.integrazione@test.gamesbar.it";
    private static final String PASSWORD = "password123";

    //crea un utente direttamente nel DB (per l'admin è come l'UPDATE fatto a mano in pgAdmin)
    private Utente creaUtente(String email, RuoloUtente ruolo) {
        Utente utente = new Utente("Prova", "Integrazione", email, passwordEncoder.encode(PASSWORD));
        utente.assegnaRuolo(ruolo);
        return utenteRepository.saveAndFlush(utente);
    }

    //fa il login e restituisce il token
    private String login(String email) throws Exception {
        String json = "{ \"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\" }";
        MvcResult risultato = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(risultato.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void senzaTokenAreaAdminRestituisce401() throws Exception {
        mockMvc.perform(get("/api/admin/utenti"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void giocatoreNonPuoUsareLAreaAdmin() throws Exception {
        creaUtente(EMAIL_GIOCATORE, RuoloUtente.GIOCATORE);
        String tokenGiocatore = login(EMAIL_GIOCATORE);

        mockMvc.perform(get("/api/admin/utenti").header("Authorization", "Bearer " + tokenGiocatore))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminVedeLElencoDegliUtenti() throws Exception {
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        creaUtente(EMAIL_GIOCATORE, RuoloUtente.GIOCATORE);
        String tokenAdmin = login(EMAIL_ADMIN);

        mockMvc.perform(get("/api/admin/utenti").header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(EMAIL_GIOCATORE)));
    }

    @Test
    void utenteSospesoDallAdminPerdeSubitoLAccesso() throws Exception {
        Utente giocatore = creaUtente(EMAIL_GIOCATORE, RuoloUtente.GIOCATORE);
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        String tokenGiocatore = login(EMAIL_GIOCATORE);
        String tokenAdmin = login(EMAIL_ADMIN);

        mockMvc.perform(put("/api/admin/utenti/" + giocatore.getId() + "/sospendi").header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stato").value("SOSPESO"));

        //il giocatore ha ancora il suo token, ma non basta più
        mockMvc.perform(get("/api/utenti/me").header("Authorization", "Bearer " + tokenGiocatore))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void nuovoRuoloValeSubitoSenzaRifareIlLogin() throws Exception {
        Utente giocatore = creaUtente(EMAIL_GIOCATORE, RuoloUtente.GIOCATORE);
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        String tokenGiocatore = login(EMAIL_GIOCATORE);
        String tokenAdmin = login(EMAIL_ADMIN);

        mockMvc.perform(put("/api/admin/utenti/" + giocatore.getId() + "/ruolo")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"ruolo\": \"ADMIN_LOCALE\" }"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ruolo").value("ADMIN_LOCALE"));

        mockMvc.perform(get("/api/utenti/me").header("Authorization", "Bearer " + tokenGiocatore))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ruolo").value("ADMIN_LOCALE"));
    }

    @Test
    void adminNonPuoSospendereSeStesso() throws Exception {
        Utente admin = creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        String tokenAdmin = login(EMAIL_ADMIN);

        mockMvc.perform(put("/api/admin/utenti/" + admin.getId() + "/sospendi").header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errore").value("non puoi modificare il stato del tuo stesso account."));
    }

    @Test
    void ruoloInesistenteRestituisce400() throws Exception {
        Utente giocatore = creaUtente(EMAIL_GIOCATORE, RuoloUtente.GIOCATORE);
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        String tokenAdmin = login(EMAIL_ADMIN);

        mockMvc.perform(put("/api/admin/utenti/" + giocatore.getId() + "/ruolo")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"ruolo\": \"PIPPO\" }"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void utenteInesistenteRestituisce404() throws Exception {
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        String tokenAdmin = login(EMAIL_ADMIN);

        mockMvc.perform(put("/api/admin/utenti/999999999/sospendi").header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isNotFound());
    }
}
