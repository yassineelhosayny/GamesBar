package com.gamesbar.backend.utente;

//valori ammessi dal CHECK utente_stato_valido: non cambiarli senza una nuova migrazione
public enum StatoUtente {
    ATTIVO,
    SOSPESO,
    BLOCCATO
}
