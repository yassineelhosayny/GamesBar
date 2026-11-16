package com.gamesbar.backend.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gamesbar.backend.auth.CredenzialiNonValideException;
import com.gamesbar.backend.auth.EmailGiaRegistrataException;
import com.gamesbar.backend.auth.UtenteNonAttivoException;

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
            String nomeCampo = errore.getField();            //es. "email"
            String messaggio = errore.getDefaultMessage();   //es. "l'email non è valida"
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
}
