package com.gamesbar.backend.utente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

//test sul PostgreSQL reale (container Docker): serve il database avviato.
//@DataJpaTest esegue ogni test in una transazione annullata alla fine,
//quindi nel database non resta nessun dato di prova
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   //non sostituire PostgreSQL con un DB in memoria
class UtenteRepositoryTest {

    private static final String EMAIL = "mario.rossi@test.gamesbar.it";

    @Autowired
    private UtenteRepository utenteRepository;

    //serve per svuotare la cache di Hibernate e costringerlo a rileggere dal DB
    @Autowired
    private TestEntityManager entityManager;

    private Utente nuovoUtente(String email) {
        return new Utente("Mario", "Rossi", email, "$2a$10$hashDiProva");
    }

    @Test
    void salvaERitrovaPerEmail() {
        Utente salvato = utenteRepository.saveAndFlush(nuovoUtente(EMAIL));
        entityManager.clear();

        Optional<Utente> trovato = utenteRepository.findByEmail(EMAIL);

        assertTrue(trovato.isPresent());
        assertNotNull(salvato.getId());
        assertEquals(salvato.getId(), trovato.get().getId());
        assertEquals("Mario", trovato.get().getNome());
        assertEquals(RuoloUtente.GIOCATORE, trovato.get().getRuolo());
        assertEquals(StatoUtente.ATTIVO, trovato.get().getStato());
    }

    @Test
    void findByEmailVuotoSeNonEsiste() {
        assertTrue(utenteRepository.findByEmail("nessuno@test.gamesbar.it").isEmpty());
    }

    @Test
    void existsByEmail() {
        assertFalse(utenteRepository.existsByEmail(EMAIL));

        utenteRepository.saveAndFlush(nuovoUtente(EMAIL));

        assertTrue(utenteRepository.existsByEmail(EMAIL));
    }

    @Test
    void emailDuplicataRifiutataDalDatabase() {
        utenteRepository.saveAndFlush(nuovoUtente(EMAIL));

        //la stessa email scritta diversamente viene normalizzata e viola utente_email_univoca
        assertThrows(DataIntegrityViolationException.class,
                () -> utenteRepository.saveAndFlush(nuovoUtente("  MARIO.ROSSI@test.gamesbar.it ")));
    }

    @Test
    void dateValorizzateDalDatabase() {
        Long id = utenteRepository.saveAndFlush(nuovoUtente(EMAIL)).getId();
        entityManager.clear();

        Utente caricato = utenteRepository.findById(id).orElseThrow();

        assertNotNull(caricato.getDataCreazione());
        assertNotNull(caricato.getDataAggiornamento());
    }

    @Test
    void dataAggiornamentoModificataDalTriggerDopoUpdate() {
        Long id = utenteRepository.saveAndFlush(nuovoUtente(EMAIL)).getId();
        entityManager.clear();

        Utente caricato = utenteRepository.findById(id).orElseThrow();
        Instant creazione = caricato.getDataCreazione();
        Instant aggiornamentoIniziale = caricato.getDataAggiornamento();

        caricato.sospendi();
        utenteRepository.saveAndFlush(caricato);   //UPDATE → il trigger usa clock_timestamp()
        entityManager.clear();

        Utente ricaricato = utenteRepository.findById(id).orElseThrow();

        assertEquals(StatoUtente.SOSPESO, ricaricato.getStato());
        assertTrue(ricaricato.getDataAggiornamento().isAfter(aggiornamentoIniziale));
        assertEquals(creazione, ricaricato.getDataCreazione());
    }
}
