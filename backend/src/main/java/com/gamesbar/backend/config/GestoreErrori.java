package com.gamesbar.backend.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gamesbar.backend.auth.CredenzialiNonValideException;
import com.gamesbar.backend.auth.EmailGiaRegistrataException;
import com.gamesbar.backend.auth.UtenteNonAttivoException;
import com.gamesbar.backend.locale.LocaleNonTrovatoException;
import com.gamesbar.backend.locale.RichiestaNonTrovataException;
import com.gamesbar.backend.utente.OperazioneNonPermessaException;
import com.gamesbar.backend.utente.UtenteNonTrovatoException;

//intercetta le eccezioni lanciate da tutti i controller
//e le trasforma in una risposta HTTP con un JSON ErroreRisposta
@RestControllerAdvice
public class GestoreErrori {

    //@Valid ha trovato uno o più campi non validi → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroreRisposta datiNonValidi(MethodArgumentNotValidException eccezione) {
        //risultato del controllo fatto da @Valid
        BindingResult risultato = eccezione.getBindingResult();

        //prendo la lista degli errori
        List<FieldError> listaErrori = risultato.getFieldErrors();

        //salvo nome del campo , messaggio scritto nel DTO
        Map<String, String> campi = new HashMap<>();
        for (FieldError errore : listaErrori) {
            String nomeCampo = errore.getField();            //esempio emai"
            String messaggio = errore.getDefaultMessage();   //=l'email non è valida
            campi.put(nomeCampo, messaggio);
        }

        return new ErroreRisposta("dati non validi", campi);
    }

    //l'email esiste già → 409
    @ExceptionHandler(EmailGiaRegistrataException.class)
     @ResponseStatus(HttpStatus.CONFLICT)
    public ErroreRisposta emailGiaRegistrata(EmailGiaRegistrataException eccezione) {
        return new ErroreRisposta(eccezione.getMessage(), new HashMap<>());
    }

    //login con email o password sbagliate → 401
    @ExceptionHandler(CredenzialiNonValideException.class)
     @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroreRisposta credenzialiNonValide(CredenzialiNonValideException eccezione) {
        return new ErroreRisposta(eccezione.getMessage(), new HashMap<>());
    }

    //password giusta ma utente sospeso o bloccato → 403
    @ExceptionHandler(UtenteNonAttivoException.class)
@ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroreRisposta utenteNonAttivo(UtenteNonAttivoException eccezione) {
        return new ErroreRisposta(eccezione.getMessage(), new HashMap<>());
    }
    //"admin"
    //l'id richiesto non esiste → 404 
    @ExceptionHandler(UtenteNonTrovatoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroreRisposta utenteNonTrovato(UtenteNonTrovatoException eccezione) {
        return new ErroreRisposta(eccezione.getMessage(), new HashMap<>());
    }

    //"locali"
    //il locale richiesto non esiste → 404
    @ExceptionHandler(LocaleNonTrovatoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroreRisposta localeNonTrovato(LocaleNonTrovatoException eccezione) {
        return new ErroreRisposta(eccezione.getMessage(), new HashMap<>());
    }

    //la richiesta di verifica non esiste → 404
    @ExceptionHandler(RichiestaNonTrovataException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroreRisposta richiestaNonTrovata(RichiestaNonTrovataException eccezione) {
        return new ErroreRisposta(eccezione.getMessage(), new HashMap<>());
    }

    //operazione vietata da una regola esempio admin non pò modificare se stesso → 403
    @ExceptionHandler(OperazioneNonPermessaException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroreRisposta operazioneNonPermessa(OperazioneNonPermessaException eccezione) {
        return new ErroreRisposta(eccezione.getMessage(), new HashMap<>());
    }

    //JSON scritto male o con un valore non ammesso (es. "ruolo": "bviibi") → 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroreRisposta jsonNonLeggibile(HttpMessageNotReadableException eccezione) {
        return new ErroreRisposta("JSON non valido o con un valore non ammesso.", new HashMap<>());
    }
}
