package com.gamesbar.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

import com.gamesbar.backend.tipogioco.CodiceGiaUsatoException;
import com.gamesbar.backend.tipogioco.NuovoTipoGiocoRichiesta;
import com.gamesbar.backend.tipogioco.TipoGioco;
import com.gamesbar.backend.tipogioco.TipoGiocoNonTrovatoException;
import com.gamesbar.backend.tipogioco.TipoGiocoRepository;
import com.gamesbar.backend.tipogioco.TipoGiocoRisposta;
import com.gamesbar.backend.tipogioco.TipoGiocoService;
import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;

@ExtendWith(MockitoExtension.class)
public class TipoGiocoServiceTest {

    @Mock
    private TipoGiocoRepository tipoGiocoRepo;
    @Mock
    private UtenteRepository utenteRepository;
    @InjectMocks
    private TipoGiocoService tipoGiocoService;

    @Test
    void ControllaGiochiAttiviDisponibili(){
        //nuovo costruttore: (creatore, codice, nome, descrizione, versione); il tipo nasce NON attivo
        final TipoGioco g1 = new TipoGioco(null, "FRECCETTE", "Freccette", "Gioco delle freccette", "0.1");
        g1.attiva();
        //final TipoGioco g2 = new TipoGioco(null, "BALILLA", "Balilla", "Gioco Balilla", "1.0");

        //prparazione del repositary
        when(tipoGiocoRepo.findByAttivoTrueOrderByNomeAsc()).thenReturn(List.of(g1));

        List<TipoGiocoRisposta> res = tipoGiocoService.trovaTipiGiochiAttivi();
        assertEquals(1,res.size());
        assertEquals("FRECCETTE",res.get(0).codice());
        assertEquals("Freccette",res.get(0).nome());
        assertEquals(true,res.get(0).isAttivo());
    }

    // ---------------- AMMINISTRATORE DEL GIOCO ----------------

    @Test
    void creaTipoNonAttivoConIlCreatoreDelToken() {
        Utente admin = mock(Utente.class);
        when(admin.getId()).thenReturn(7L);
        when(utenteRepository.findByEmail("admin.gioco@example.com")).thenReturn(Optional.of(admin));
        when(tipoGiocoRepo.existsByCodice("CALCIO_BALILLA")).thenReturn(false);
        when(tipoGiocoRepo.save(any(TipoGioco.class))).thenAnswer(invocazione -> invocazione.getArgument(0));
        NuovoTipoGiocoRichiesta richiesta = new NuovoTipoGiocoRichiesta("calcio_balilla", "Calcio balilla", null, "1.0");

        TipoGiocoRisposta risposta = tipoGiocoService.creaTipo(richiesta, "admin.gioco@example.com");

        assertEquals("CALCIO_BALILLA", risposta.codice());
        assertFalse(risposta.isAttivo());
    }

    @Test
    void codiceGiaUsatoNonSiCrea() {
        when(tipoGiocoRepo.existsByCodice("FRECCETTE")).thenReturn(true);
        NuovoTipoGiocoRichiesta richiesta = new NuovoTipoGiocoRichiesta("freccette", "Altre freccette", null, null);

        assertThrows(CodiceGiaUsatoException.class,
            () -> tipoGiocoService.creaTipo(richiesta, "admin.gioco@example.com"));

        verify(tipoGiocoRepo, never()).save(any(TipoGioco.class));
    }

    @Test
    void attivaRendeIlTipoAttivo() {
        TipoGioco tipo = new TipoGioco(null, "FRECCETTE", "Freccette", null, null);
        when(tipoGiocoRepo.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoGiocoRepo.save(tipo)).thenReturn(tipo);

        TipoGiocoRisposta risposta = tipoGiocoService.attiva(1L);

        assertTrue(risposta.isAttivo());
    }

    @Test
    void disattivaRendeIlTipoNonAttivo() {
        TipoGioco tipo = new TipoGioco(null, "FRECCETTE", "Freccette", null, null);
        tipo.attiva();
        when(tipoGiocoRepo.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoGiocoRepo.save(tipo)).thenReturn(tipo);

        TipoGiocoRisposta risposta = tipoGiocoService.disattiva(1L);

        assertFalse(risposta.isAttivo());
    }

    @Test
    void tipoInesistenteLanciaNonTrovato() {
        when(tipoGiocoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TipoGiocoNonTrovatoException.class, () -> tipoGiocoService.attiva(99L));
    }
}
