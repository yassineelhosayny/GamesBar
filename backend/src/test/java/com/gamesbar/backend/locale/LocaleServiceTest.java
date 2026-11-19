package com.gamesbar.backend.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

import com.gamesbar.backend.utente.OperazioneNonPermessaException;
import com.gamesbar.backend.utente.RuoloUtente;
import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;

@ExtendWith(MockitoExtension.class)
class LocaleServiceTest {

    @Mock
    private LocaleRepository localeRepository;
    @Mock
    private RichiestaVerificaLocaleRepository richiestaRepository;
    @Mock
    private UtenteRepository utenteRepository;
    @InjectMocks
    private LocaleService localeService;

    private static final String EMAIL_GESTORE = "mario.rossi@example.com";
    private static final String EMAIL_ADMIN = "admin@example.com";

    //Utente non ha un setter per l'id (lo decide il DB): nei test uso un utente finto con l'id che serve
    private Utente utenteFinto(Long id) {
        Utente utente = mock(Utente.class);
        when(utente.getId()).thenReturn(id);
        return utente;
    }

    private Locale localeDi(Long idGestore) {
        return new Locale(idGestore, "Bar Sport", null, "Via Roma 1", "Vercelli", "13100", "VC", null);
    }

    // ---------------- GESTORE ----------------

    @Test
    void creaLocaleSalvaAncheLaRichiestaDelGestore() {
        Utente gestore = utenteFinto(10L);
        when(utenteRepository.findByEmail(EMAIL_GESTORE)).thenReturn(Optional.of(gestore));
        when(localeRepository.save(any(Locale.class))).thenAnswer(invocazione -> invocazione.getArgument(0));
        NuovoLocaleRichiesta richiesta = new NuovoLocaleRichiesta("Bar Sport", null, "Via Roma 1",
            "Vercelli", "13100", "vc", null, "Sono il titolare");

        LocaleRisposta risposta = localeService.creaLocale(richiesta, EMAIL_GESTORE);

        assertEquals(10L, risposta.amministratoreId());
        assertEquals(StatoVerificaLocale.IN_ATTESA, risposta.statoVerifica());

        ArgumentCaptor<RichiestaVerificaLocale> captor = ArgumentCaptor.forClass(RichiestaVerificaLocale.class);
        verify(richiestaRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getRichiedenteId());
        assertEquals("Sono il titolare", captor.getValue().getMessaggioRichiedente());
    }

    @Test
    void nonSiModificaIlLocaleDiUnAltro() {
        Utente utente = utenteFinto(10L);
        when(utenteRepository.findByEmail(EMAIL_GESTORE)).thenReturn(Optional.of(utente));
        when(localeRepository.findById(5L)).thenReturn(Optional.of(localeDi(99L)));
        ModificaLocaleRichiesta modifica = new ModificaLocaleRichiesta("Nuovo nome", null, null);

        assertThrows(OperazioneNonPermessaException.class,
            () -> localeService.modificaLocale(5L, modifica, EMAIL_GESTORE));

        verify(localeRepository, never()).save(any(Locale.class));
    }

    @Test
    void localeRifiutatoNonSiModifica() {
        Utente utente = utenteFinto(10L);
        Locale locale = localeDi(10L);
        locale.rifiuta();
        when(utenteRepository.findByEmail(EMAIL_GESTORE)).thenReturn(Optional.of(utente));
        when(localeRepository.findById(5L)).thenReturn(Optional.of(locale));
        ModificaLocaleRichiesta modifica = new ModificaLocaleRichiesta("Nuovo nome", null, null);

        assertThrows(OperazioneNonPermessaException.class,
            () -> localeService.modificaLocale(5L, modifica, EMAIL_GESTORE));
    }

    // ---------------- ADMIN ----------------

    //prepara una richiesta (id 1) per il locale 5 del gestore 10, con l'admin che ha id 1
    private void preparaRichiesta(RichiestaVerificaLocale richiesta, Locale locale, Utente gestore) {
        Utente admin = utenteFinto(1L);
        when(richiestaRepository.findById(1L)).thenReturn(Optional.of(richiesta));
        when(utenteRepository.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin));
        when(localeRepository.findById(5L)).thenReturn(Optional.of(locale));
        if (gestore != null) {
            when(utenteRepository.findById(10L)).thenReturn(Optional.of(gestore));
        }
    }

    @Test
    void approvaPromuoveIlGiocatoreAdAdminLocale() {
        Utente gestore = mock(Utente.class);
        when(gestore.getRuolo()).thenReturn(RuoloUtente.GIOCATORE);
        preparaRichiesta(new RichiestaVerificaLocale(5L, 10L, null), localeDi(10L), gestore);

        RichiestaLocaleRisposta risposta = localeService.approvaRichiesta(1L, EMAIL_ADMIN);

        assertEquals(StatoRichiestaVerifica.APPROVATA, risposta.stato());
        assertEquals(StatoVerificaLocale.APPROVATO, risposta.locale().statoVerifica());
        verify(gestore).assegnaRuolo(RuoloUtente.ADMIN_LOCALE);
    }

    @Test
    void approvaNonAbbassaUnRuoloPiuAlto() {
        Utente gestore = mock(Utente.class);
        when(gestore.getRuolo()).thenReturn(RuoloUtente.ADMIN_PIATTAFORMA);
        preparaRichiesta(new RichiestaVerificaLocale(5L, 10L, null), localeDi(10L), gestore);

        localeService.approvaRichiesta(1L, EMAIL_ADMIN);

        verify(gestore, never()).assegnaRuolo(any(RuoloUtente.class));
    }

    @Test
    void rifiutaSalvaIlMotivoERifiutaIlLocale() {
        preparaRichiesta(new RichiestaVerificaLocale(5L, 10L, null), localeDi(10L), null);

        RichiestaLocaleRisposta risposta = localeService.rifiutaRichiesta(1L, "indirizzo inesistente", EMAIL_ADMIN);

        assertEquals(StatoRichiestaVerifica.RIFIUTATA, risposta.stato());
        assertEquals("indirizzo inesistente", risposta.motivoRifiuto());
        assertEquals(StatoVerificaLocale.RIFIUTATO, risposta.locale().statoVerifica());
    }

    @Test
    void richiestaGiaEsaminataNonSiRiesamina() {
        RichiestaVerificaLocale richiesta = new RichiestaVerificaLocale(5L, 10L, null);
        richiesta.approva(1L);
        when(richiestaRepository.findById(1L)).thenReturn(Optional.of(richiesta));

        assertThrows(OperazioneNonPermessaException.class, () -> localeService.approvaRichiesta(1L, EMAIL_ADMIN));
    }

    @Test
    void richiestaInesistenteLanciaNonTrovata() {
        when(richiestaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RichiestaNonTrovataException.class, () -> localeService.approvaRichiesta(99L, EMAIL_ADMIN));
    }
}
