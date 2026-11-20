package com.gamesbar.backend.tipogioco;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

import com.gamesbar.backend.utente.RuoloUtente;
import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;
import com.jayway.jsonpath.JsonPath;

//test di INTEGRAZIONE dei tipi di gioco: sicurezza vera + PostgreSQL.
//serve il container PostgreSQL acceso; @Transactional annulla tutto alla fine di ogni test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TipoGiocoIntegrazioneTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String URL_ADMIN = "/api/admin-gioco/tipi-gioco";
    private static final String URL_PUBBLICO = "/api/tipi-gioco/attivi";
    private static final String PASSWORD = "password123";

    //codice inventato solo per i test (in minuscolo: il backend lo trasforma in maiuscolo)
    private static final String JSON_NUOVO_TIPO = """
        { "codice": "test_integrazione", "nome": "Gioco di prova", "descrizione": "solo per i test", "versioneGestore": "0.1" }
        """;

    //crea un utente con un certo ruolo e restituisce il suo token
    private String tokenDi(String email, RuoloUtente ruolo) throws Exception {
        Utente utente = new Utente("Prova", "TipoGioco", email, passwordEncoder.encode(PASSWORD));
        utente.assegnaRuolo(ruolo);
        utenteRepository.saveAndFlush(utente);

        String json = "{ \"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\" }";
        MvcResult risultato = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(risultato.getResponse().getContentAsString(), "$.token");
    }

    //crea il tipo di prova e restituisce il suo id
    private Long creaTipoDiProva(String token) throws Exception {
        MvcResult risultato = mockMvc.perform(post(URL_ADMIN).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(JSON_NUOVO_TIPO))
            .andExpect(status().isCreated())
            .andReturn();
        Number id = JsonPath.read(risultato.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    void listaPubblicaSenzaToken() throws Exception {
        mockMvc.perform(get(URL_PUBBLICO))
            .andExpect(status().isOk());
    }

    @Test
    void senzaTokenAreaAdminGiocoRestituisce401() throws Exception {
        mockMvc.perform(get(URL_ADMIN))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void giocatoreNonPuoGestireITipi() throws Exception {
        String token = tokenDi("giocatore.tipi@test.gamesbar.it", RuoloUtente.GIOCATORE);

        mockMvc.perform(get(URL_ADMIN).header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminGiocoCreaUnTipoNonAttivo() throws Exception {
        String token = tokenDi("admin.gioco@test.gamesbar.it", RuoloUtente.ADMIN_GIOCO);

        mockMvc.perform(post(URL_ADMIN).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(JSON_NUOVO_TIPO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codice").value("TEST_INTEGRAZIONE"))
            .andExpect(jsonPath("$.isAttivo").value(false));

        //non attivo → non compare nella lista pubblica
        mockMvc.perform(get(URL_PUBBLICO))
            .andExpect(content().string(not(containsString("TEST_INTEGRAZIONE"))));
    }

    @Test
    void tipoAttivatoCompareNellaListaPubblica() throws Exception {
        String token = tokenDi("admin.gioco@test.gamesbar.it", RuoloUtente.ADMIN_GIOCO);
        Long id = creaTipoDiProva(token);

        mockMvc.perform(put(URL_ADMIN + "/" + id + "/attiva").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isAttivo").value(true));

        mockMvc.perform(get(URL_PUBBLICO))
            .andExpect(content().string(containsString("TEST_INTEGRAZIONE")));
    }

    @Test
    void tipoDisattivatoSparisceDallaListaPubblica() throws Exception {
        String token = tokenDi("admin.gioco@test.gamesbar.it", RuoloUtente.ADMIN_GIOCO);
        Long id = creaTipoDiProva(token);
        mockMvc.perform(put(URL_ADMIN + "/" + id + "/attiva").header("Authorization", "Bearer " + token));

        mockMvc.perform(put(URL_ADMIN + "/" + id + "/disattiva").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get(URL_PUBBLICO))
            .andExpect(content().string(not(containsString("TEST_INTEGRAZIONE"))));
    }

    @Test
    void adminPiattaformaVedeAncheFreccette() throws Exception {
        String token = tokenDi("admin.piattaforma.tipi@test.gamesbar.it", RuoloUtente.ADMIN_PIATTAFORMA);

        mockMvc.perform(get(URL_ADMIN).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("FRECCETTE")));
    }

    @Test
    void codiceGiaUsatoRestituisce409() throws Exception {
        String token = tokenDi("admin.gioco@test.gamesbar.it", RuoloUtente.ADMIN_GIOCO);
        String json = """
            { "codice": "freccette", "nome": "Freccette doppione" }
            """;

        mockMvc.perform(post(URL_ADMIN).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errore").value("esiste già un tipo di gioco con codice FRECCETTE."));
    }

    @Test
    void codiceNonValidoRestituisce400() throws Exception {
        String token = tokenDi("admin.gioco@test.gamesbar.it", RuoloUtente.ADMIN_GIOCO);
        String json = """
            { "codice": "1-gioco", "nome": "Codice sbagliato" }
            """;

        mockMvc.perform(post(URL_ADMIN).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campi.codice").exists());
    }

    @Test
    void tipoInesistenteRestituisce404() throws Exception {
        String token = tokenDi("admin.gioco@test.gamesbar.it", RuoloUtente.ADMIN_GIOCO);

        mockMvc.perform(put(URL_ADMIN + "/999999999/attiva").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }
}
