package com.gamesbar.backend.utente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UtenteServiceTest {

    @Mock
    private UtenteRepository utenteRepository;
    @InjectMocks
    private UtenteService utenteService;

    private static final String EMAIL_ADMIN = "admin@example.com";

    private Utente nuovoGiocatore() {
        return new Utente("Mario", "Rossi", "mario.rossi@example.com", "HASH_FINTO");
    }

    //l'utente con id 5 esiste e save() lo restituisce, come il vero repository
    private void preparaUtente(Utente utente) {
        when(utenteRepository.findById(5L)).thenReturn(Optional.of(utente));
        when(utenteRepository.save(utente)).thenReturn(utente);
    }

    @Test
    void sospendiCambiaLoStatoInSospeso() {
        Utente utente = nuovoGiocatore();
        preparaUtente(utente);

        UtenteRisposta risposta = utenteService.sospendi(5L, EMAIL_ADMIN);

        assertEquals(StatoUtente.SOSPESO, risposta.stato());
    }

    @Test
    void bloccaCambiaLoStatoInBloccato() {
        Utente utente = nuovoGiocatore();
        preparaUtente(utente);

        UtenteRisposta risposta = utenteService.blocca(5L, EMAIL_ADMIN);

        assertEquals(StatoUtente.BLOCCATO, risposta.stato());
    }

    @Test
    void attivaRiattivaUnUtenteSospeso() {
        Utente utente = nuovoGiocatore();
        utente.sospendi();
        preparaUtente(utente);

        UtenteRisposta risposta = utenteService.attiva(5L, EMAIL_ADMIN);

        assertEquals(StatoUtente.ATTIVO, risposta.stato());
    }

    @Test
    void cambiaRuoloAssegnaIlNuovoRuolo() {
        Utente utente = nuovoGiocatore();
        preparaUtente(utente);

        UtenteRisposta risposta = utenteService.cambiaRuolo(5L, RuoloUtente.ADMIN_LOCALE, EMAIL_ADMIN);

        assertEquals(RuoloUtente.ADMIN_LOCALE, risposta.ruolo());
    }

    @Test
    void utenteInesistenteLanciaNonTrovato() {
        when(utenteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UtenteNonTrovatoException.class, () -> utenteService.sospendi(99L, EMAIL_ADMIN));
    }

    @Test
    void adminNonPuoModificareSeStesso() {
        Utente admin = new Utente("Admin", "Piattaforma", EMAIL_ADMIN, "HASH_FINTO");
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(OperazioneNonPermessaException.class, () -> utenteService.sospendi(1L, EMAIL_ADMIN));

        assertEquals(StatoUtente.ATTIVO, admin.getStato());
        verify(utenteRepository, never()).save(any(Utente.class));
    }

    @Test
    void trovaTuttiRestituisceTuttiGliUtenti() {
        Utente primo = nuovoGiocatore();
        Utente secondo = new Utente("Anna", "Verdi", "anna.verdi@example.com", "HASH_FINTO");
        when(utenteRepository.findAllByOrderByIdAsc()).thenReturn(List.of(primo, secondo));

        List<UtenteRisposta> risposte = utenteService.trovaTutti();

        assertEquals(2, risposte.size());
        assertEquals("anna.verdi@example.com", risposte.get(1).email());
    }
}
