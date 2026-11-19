package com.gamesbar.backend.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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

//test di INTEGRAZIONE dei locali: tutto il flusso gestore → admin con sicurezza vera e PostgreSQL.
//serve il container PostgreSQL acceso; @Transactional annulla tutto alla fine di ogni test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LocaleIntegrazioneTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private RichiestaVerificaLocaleRepository richiestaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String EMAIL_GESTORE = "gestore.locale@test.gamesbar.it";
    private static final String EMAIL_ALTRO = "altro.utente@test.gamesbar.it";
    private static final String EMAIL_ADMIN = "admin.locale@test.gamesbar.it";
    private static final String PASSWORD = "password123";

    private static final String JSON_LOCALE = """
        {
          "nome": "Bar Sport",
          "descrizione": "Bar con freccette",
          "indirizzo": "Via Roma 1",
          "citta": "Vercelli",
          "codicePostale": "13100",
          "provincia": "vc",
          "numeroTelefono": "0161 123456",
          "messaggio": "Sono il titolare"
        }
        """;

    private void creaUtente(String email, RuoloUtente ruolo) {
        Utente utente = new Utente("Prova", "Locale", email, passwordEncoder.encode(PASSWORD));
        utente.assegnaRuolo(ruolo);
        utenteRepository.saveAndFlush(utente);
    }

    private String login(String email) throws Exception {
        String json = "{ \"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\" }";
        MvcResult risultato = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(risultato.getResponse().getContentAsString(), "$.token");
    }

    //crea il locale con il token del gestore e restituisce il suo id
    private Long creaLocale(String token) throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/locali")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_LOCALE))
            .andExpect(status().isCreated())
            .andReturn();
        Number id = JsonPath.read(risultato.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private Long idRichiestaDelLocale(Long idLocale) {
        List<RichiestaVerificaLocale> richieste = richiestaRepository.findByLocaleIdOrderByIdAsc(idLocale);
        return richieste.get(0).getId();
    }

    @Test
    void senzaTokenNonSiCreaUnLocale() throws Exception {
        mockMvc.perform(post("/api/locali").contentType(MediaType.APPLICATION_JSON).content(JSON_LOCALE))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void giocatoreCreaLocaleInAttesaConLaSuaRichiesta() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        String token = login(EMAIL_GESTORE);

        Long idLocale = creaLocale(token);

        mockMvc.perform(get("/api/locali/miei").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].statoVerifica").value("IN_ATTESA"))
            .andExpect(jsonPath("$[0].provincia").value("VC"));

        List<RichiestaVerificaLocale> richieste = richiestaRepository.findByLocaleIdOrderByIdAsc(idLocale);
        assertEquals(1, richieste.size());
        assertEquals(StatoRichiestaVerifica.IN_ATTESA, richieste.get(0).getStato());
    }

    @Test
    void indirizzoMancanteRestituisce400() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        String token = login(EMAIL_GESTORE);
        String json = """
            { "nome": "Bar Sport", "citta": "Vercelli", "codicePostale": "13100" }
            """;

        mockMvc.perform(post("/api/locali").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campi.indirizzo").value("l'indirizzo è obbligatorio"));
    }

    @Test
    void approvazioneRendeIlGestoreAdminLocale() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        String tokenGestore = login(EMAIL_GESTORE);
        String tokenAdmin = login(EMAIL_ADMIN);
        Long idRichiesta = idRichiestaDelLocale(creaLocale(tokenGestore));

        mockMvc.perform(put("/api/admin/richieste-locali/" + idRichiesta + "/approva")
                .header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stato").value("APPROVATA"))
            .andExpect(jsonPath("$.locale.statoVerifica").value("APPROVATO"));

        //il gestore è diventato ADMIN_LOCALE, senza rifare il login
        mockMvc.perform(get("/api/utenti/me").header("Authorization", "Bearer " + tokenGestore))
            .andExpect(jsonPath("$.ruolo").value("ADMIN_LOCALE"));
    }

    @Test
    void rifiutoDefinitivoBloccaLeModifiche() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        String tokenGestore = login(EMAIL_GESTORE);
        String tokenAdmin = login(EMAIL_ADMIN);
        Long idLocale = creaLocale(tokenGestore);
        Long idRichiesta = idRichiestaDelLocale(idLocale);

        mockMvc.perform(put("/api/admin/richieste-locali/" + idRichiesta + "/rifiuta")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"motivo\": \"indirizzo inesistente\" }"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stato").value("RIFIUTATA"))
            .andExpect(jsonPath("$.motivoRifiuto").value("indirizzo inesistente"));

        //il locale rifiutato non si può più modificare
        mockMvc.perform(put("/api/locali/" + idLocale)
                .header("Authorization", "Bearer " + tokenGestore)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Altro nome\" }"))
            .andExpect(status().isForbidden());

        //e il gestore resta GIOCATORE
        mockMvc.perform(get("/api/utenti/me").header("Authorization", "Bearer " + tokenGestore))
            .andExpect(jsonPath("$.ruolo").value("GIOCATORE"));
    }

    @Test
    void rifiutoSenzaMotivoRestituisce400() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        Long idRichiesta = idRichiestaDelLocale(creaLocale(login(EMAIL_GESTORE)));
        String tokenAdmin = login(EMAIL_ADMIN);

        mockMvc.perform(put("/api/admin/richieste-locali/" + idRichiesta + "/rifiuta")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ }"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void richiestaGiaEsaminataRestituisce403() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        creaUtente(EMAIL_ADMIN, RuoloUtente.ADMIN_PIATTAFORMA);
        Long idRichiesta = idRichiestaDelLocale(creaLocale(login(EMAIL_GESTORE)));
        String tokenAdmin = login(EMAIL_ADMIN);
        String url = "/api/admin/richieste-locali/" + idRichiesta + "/approva";

        mockMvc.perform(put(url).header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk());

        mockMvc.perform(put(url).header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errore").value("la richiesta è già stata esaminata."));
    }

    @Test
    void modificaCambiaSoloICampiPermessi() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        String token = login(EMAIL_GESTORE);
        Long idLocale = creaLocale(token);
        //"indirizzo" viene mandato ma deve essere ignorato
        String json = """
            { "nome": "Bar Sport 2", "descrizione": "Nuova descrizione", "numeroTelefono": "333 1234567",
              "indirizzo": "Via Falsa 99" }
            """;

        mockMvc.perform(put("/api/locali/" + idLocale).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Bar Sport 2"))
            .andExpect(jsonPath("$.numeroTelefono").value("333 1234567"))
            .andExpect(jsonPath("$.indirizzo").value("Via Roma 1"));
    }

    @Test
    void nonSiModificaIlLocaleDiUnAltro() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        creaUtente(EMAIL_ALTRO, RuoloUtente.GIOCATORE);
        Long idLocale = creaLocale(login(EMAIL_GESTORE));
        String tokenAltro = login(EMAIL_ALTRO);

        mockMvc.perform(put("/api/locali/" + idLocale).header("Authorization", "Bearer " + tokenAltro)
                .contentType(MediaType.APPLICATION_JSON).content("{ \"nome\": \"Rubato\" }"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errore").value("puoi modificare solo i tuoi locali."));
    }

    @Test
    void giocatoreNonVedeLeRichiesteDegliAltri() throws Exception {
        creaUtente(EMAIL_GESTORE, RuoloUtente.GIOCATORE);
        String token = login(EMAIL_GESTORE);

        mockMvc.perform(get("/api/admin/richieste-locali").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
