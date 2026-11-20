package com.gamesbar.backend.tipogioco;

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

import com.gamesbar.backend.utente.RuoloUtente;
import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;
import com.jayway.jsonpath.JsonPath;

//test di INTEGRAZIONE di eventi e sensori: sicurezza vera + PostgreSQL (compresi i vincoli dello schema).
//serve il container PostgreSQL acceso; @Transactional annulla tutto alla fine di ogni test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventiSensoriIntegrazioneTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private TipoGiocoRepository tipoGiocoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "password123";

    private String tokenDi(String email, RuoloUtente ruolo) throws Exception {
        Utente utente = new Utente("Prova", "Eventi", email, passwordEncoder.encode(PASSWORD));
        utente.assegnaRuolo(ruolo);
        utenteRepository.saveAndFlush(utente);

        String json = "{ \"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\" }";
        MvcResult risultato = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(risultato.getResponse().getContentAsString(), "$.token");
    }

    private String tokenAdminGioco() throws Exception {
        return tokenDi("admin.eventi@test.gamesbar.it", RuoloUtente.ADMIN_GIOCO);
    }

    //l'id di FRECCETTE (creato da V3) cambia da un database all'altro: lo cerco
    private Long idFreccette() {
        for (TipoGioco tipo : tipoGiocoRepository.findAllByOrderByNomeAsc()) {
            if (tipo.getCodice().equals("FRECCETTE")) {
                return tipo.getId();
            }
        }
        throw new IllegalStateException("FRECCETTE non trovato: V3 non è stata applicata?");
    }

    //fa una POST e restituisce l'id dell'oggetto creato
    private Long creaEPrendiId(String url, String json, String token) throws Exception {
        MvcResult risultato = mockMvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn();
        Number id = JsonPath.read(risultato.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    //un nuovo tipo di gioco solo per i test, con codice TEST_EVENTI
    private Long creaTipoDiProva(String token) throws Exception {
        return creaEPrendiId("/api/admin-gioco/tipi-gioco",
            "{ \"codice\": \"test_eventi\", \"nome\": \"Gioco di prova eventi\" }", token);
    }

    @Test
    void freccetteHaGliEventiDiV3() throws Exception {
        String token = tokenAdminGioco();

        mockMvc.perform(get("/api/admin-gioco/tipi-gioco/" + idFreccette() + "/eventi").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("TIRO_RILEVATO")))
            .andExpect(content().string(containsString("PARTITA_INIZIATA")));
    }

    @Test
    void freccetteHaIlRilevatoreDiTiro() throws Exception {
        String token = tokenAdminGioco();

        mockMvc.perform(get("/api/admin-gioco/tipi-gioco/" + idFreccette() + "/sensori").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"codice\":\"RILEVATORE_TIRO\"")))
            .andExpect(content().string(containsString("\"codiceEventoProdotto\":\"TIRO_RILEVATO\"")));
    }

    @Test
    void nuovoGiocoConEventoESensore() throws Exception {
        String token = tokenAdminGioco();
        Long idTipo = creaTipoDiProva(token);

        mockMvc.perform(post("/api/admin-gioco/tipi-gioco/" + idTipo + "/eventi").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{ \"codice\": \"gol_segnato\", \"nome\": \"Gol segnato\" }"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codice").value("GOL_SEGNATO"))
            .andExpect(jsonPath("$.codiceTipoGioco").value("TEST_EVENTI"));

        String jsonSensore = """
            { "codice": "sensore_porta", "nome": "Sensore porta", "codiceEventoProdotto": "GOL_SEGNATO", "obbligatorio": true }
            """;
        mockMvc.perform(post("/api/admin-gioco/tipi-gioco/" + idTipo + "/sensori").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(jsonSensore))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codice").value("SENSORE_PORTA"))
            .andExpect(jsonPath("$.codiceEventoProdotto").value("GOL_SEGNATO"))
            .andExpect(jsonPath("$.obbligatorio").value(true));
    }

    @Test
    void sensoreNonPuoProdurreUnEventoDiUnAltroGioco() throws Exception {
        String token = tokenAdminGioco();
        Long idTipo = creaTipoDiProva(token);
        //TIRO_RILEVATO esiste, ma è delle FRECCETTE, non di TEST_EVENTI
        String jsonSensore = """
            { "codice": "sensore_sbagliato", "nome": "Sensore sbagliato", "codiceEventoProdotto": "TIRO_RILEVATO" }
            """;

        mockMvc.perform(post("/api/admin-gioco/tipi-gioco/" + idTipo + "/sensori").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(jsonSensore))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errore").value("il gioco TEST_EVENTI non ha un evento con codice TIRO_RILEVATO."));
    }

    @Test
    void eventoDoppioneRestituisce409() throws Exception {
        String token = tokenAdminGioco();

        mockMvc.perform(post("/api/admin-gioco/tipi-gioco/" + idFreccette() + "/eventi").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{ \"codice\": \"tiro_rilevato\", \"nome\": \"Doppione\" }"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errore").value("il gioco FRECCETTE ha già un evento con codice TIRO_RILEVATO."));
    }

    @Test
    void disabilitareUnEventoCambiaLaRegolaDiRegistrazione() throws Exception {
        String token = tokenAdminGioco();
        Long idTipo = creaTipoDiProva(token);
        Long idEvento = creaEPrendiId("/api/admin-gioco/tipi-gioco/" + idTipo + "/eventi",
            "{ \"codice\": \"gol_segnato\", \"nome\": \"Gol segnato\" }", token);

        mockMvc.perform(put("/api/admin-gioco/eventi/" + idEvento + "/disabilita").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.abilitata").value(false));

        mockMvc.perform(put("/api/admin-gioco/eventi/" + idEvento + "/abilita").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.abilitata").value(true));
    }

    @Test
    void disattivareUnSensore() throws Exception {
        String token = tokenAdminGioco();
        Long idTipo = creaTipoDiProva(token);
        creaEPrendiId("/api/admin-gioco/tipi-gioco/" + idTipo + "/eventi",
            "{ \"codice\": \"gol_segnato\", \"nome\": \"Gol segnato\" }", token);
        Long idSensore = creaEPrendiId("/api/admin-gioco/tipi-gioco/" + idTipo + "/sensori",
            "{ \"codice\": \"sensore_porta\", \"nome\": \"Sensore porta\", \"codiceEventoProdotto\": \"GOL_SEGNATO\" }", token);

        mockMvc.perform(put("/api/admin-gioco/sensori/" + idSensore + "/disattiva").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attivo").value(false));
    }

    @Test
    void giocatoreNonPuoGestireGliEventi() throws Exception {
        String token = tokenDi("giocatore.eventi@test.gamesbar.it", RuoloUtente.GIOCATORE);

        mockMvc.perform(get("/api/admin-gioco/tipi-gioco/" + idFreccette() + "/eventi").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void tipoGiocoInesistenteRestituisce404() throws Exception {
        String token = tokenAdminGioco();

        mockMvc.perform(get("/api/admin-gioco/tipi-gioco/999999999/eventi").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }
}
