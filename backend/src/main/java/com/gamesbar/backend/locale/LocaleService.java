package com.gamesbar.backend.locale;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamesbar.backend.utente.OperazioneNonPermessaException;
import com.gamesbar.backend.utente.RuoloUtente;
import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;

@Service
public class LocaleService {
    private final LocaleRepository localeRepository;
    private final RichiestaVerificaLocaleRepository richiestaRepository;
    private final UtenteRepository utenteRepository;

    public LocaleService(LocaleRepository localeRepository, RichiestaVerificaLocaleRepository richiestaRepository,
                         UtenteRepository utenteRepository) {
        this.localeRepository = localeRepository;
        this.richiestaRepository = richiestaRepository;
        this.utenteRepository = utenteRepository;
    }

    //Locale

    //crea il locale e la sua richiesta di verifica.
    @Transactional //o tutto va bene oppure torna indetro
    public LocaleRisposta creaLocale(NuovoLocaleRichiesta richiesta, String emailUtente) {
        Utente gestore = trovaUtentePerEmail(emailUtente);

        Locale locale = new Locale(gestore.getId(), richiesta.nome(), richiesta.descrizione(),
            richiesta.indirizzo(), richiesta.citta(), richiesta.codicePostale(),
            richiesta.provincia(), richiesta.numeroTelefono());
        Locale salvato = localeRepository.save(locale);

        RichiestaVerificaLocale verifica = new RichiestaVerificaLocale(salvato.getId(), gestore.getId(), richiesta.messaggio());
        richiestaRepository.save(verifica);

        return LocaleRisposta.daLocale(salvato);
    }

    public List<LocaleRisposta> trovaMieiLocali(String emailUtente) {
        Utente gestore = trovaUtentePerEmail(emailUtente);
        List<Locale> locali = localeRepository.findByAmministratoreIdOrderByIdAsc(gestore.getId());

        List<LocaleRisposta> risposte = new ArrayList<>();
        for (Locale locale : locali) {
            risposte.add(LocaleRisposta.daLocale(locale));
        }
        return risposte;
    }

    //modifica solo nome, descrizione, telefono
    public LocaleRisposta modificaLocale(Long id, ModificaLocaleRichiesta richiesta, String emailUtente) {
        Utente utente = trovaUtentePerEmail(emailUtente);
        Locale locale = trovaLocale(id);

        //solo il gestore del locale può modificarlo
        if (!locale.getAmministratoreId().equals(utente.getId())) {
            throw new OperazioneNonPermessaException("puoi modificare solo i tuoi locali.");
        }
        //il rifiuto è definitivo: il locale resta com'era (anche per le statistiche)
        if (locale.isRifiutato()) {
            throw new OperazioneNonPermessaException("il locale è stato rifiutato e non può più essere modificato.");
        }

        locale.modificaDati(richiesta.nome(), richiesta.descrizione(), richiesta.numeroTelefono());
        return LocaleRisposta.daLocale(localeRepository.save(locale));
    }

    // Admin_Piattaforma

    //le richieste in_attsa
    public List<RichiestaLocaleRisposta> trovaRichiesteInAttesa() {
        List<RichiestaVerificaLocale> richieste =
            richiestaRepository.findByStatoOrderByDataRichiestaAsc(StatoRichiestaVerifica.IN_ATTESA);

        List<RichiestaLocaleRisposta> risposte = new ArrayList<>();
        for (RichiestaVerificaLocale richiesta : richieste) {
            Locale locale = trovaLocale(richiesta.getLocaleId());
            risposte.add(RichiestaLocaleRisposta.daRichiesta(richiesta, locale));
        }
        return risposte;
    }

    //accetta richiesta
    @Transactional
    public RichiestaLocaleRisposta approvaRichiesta(Long idRichiesta, String emailAdmin) {
        RichiestaVerificaLocale richiesta = trovaRichiestaInAttesa(idRichiesta);
        Utente admin = trovaUtentePerEmail(emailAdmin);
        Locale locale = trovaLocale(richiesta.getLocaleId());

        richiesta.approva(admin.getId());
        locale.approva();

        //modifica il ruolo di un giocatore a un admin locale
        Utente gestore = trovaUtentePerId(locale.getAmministratoreId());
        if (gestore.getRuolo() == RuoloUtente.GIOCATORE) {
            gestore.assegnaRuolo(RuoloUtente.ADMIN_LOCALE);
            utenteRepository.save(gestore);
        }

        richiestaRepository.save(richiesta);
        localeRepository.save(locale);
        return RichiestaLocaleRisposta.daRichiesta(richiesta, locale);
    }

    //rifiutaRichiesta. 
    @Transactional
    public RichiestaLocaleRisposta rifiutaRichiesta(Long idRichiesta, String motivo, String emailAdmin) {
        RichiestaVerificaLocale richiesta = trovaRichiestaInAttesa(idRichiesta);
        Utente admin = trovaUtentePerEmail(emailAdmin);
        Locale locale = trovaLocale(richiesta.getLocaleId());

        richiesta.rifiuta(admin.getId(), motivo);
        locale.rifiuta();

        richiestaRepository.save(richiesta);
        localeRepository.save(locale);
        return RichiestaLocaleRisposta.daRichiesta(richiesta, locale);
    }

    //cerca la richiesta (404 se non c'è) e controlla che non sia già stata decisa
    private RichiestaVerificaLocale trovaRichiestaInAttesa(Long id) {
        Optional<RichiestaVerificaLocale> trovata = richiestaRepository.findById(id);
        if (trovata.isEmpty()) {
            throw new RichiestaNonTrovataException(id);
        }
        RichiestaVerificaLocale richiesta = trovata.get();
        if (!richiesta.isInAttesa()) {
            throw new OperazioneNonPermessaException("la richiesta è già stata esaminata.");
        }
        return richiesta;
    }

    private Locale trovaLocale(Long id) {
        Optional<Locale> trovato = localeRepository.findById(id);
        if (trovato.isEmpty()) {
            throw new LocaleNonTrovatoException(id);
        }
        return trovato.get();
    }

    private Utente trovaUtentePerEmail(String email) {
        Optional<Utente> trovato = utenteRepository.findByEmail(email);
        if (trovato.isEmpty()) {
            throw new IllegalStateException("utente non trovato: " + email);
        }
        return trovato.get();
    }

    private Utente trovaUtentePerId(Long id) {
        Optional<Utente> trovato = utenteRepository.findById(id);
        if (trovato.isEmpty()) {
            throw new IllegalStateException("utente non trovato: " + id);
        }
        return trovato.get();
    }
}
