package com.gamesbar.backend.utente;

//descrive il JSON di un utente restituito dall'API.
//non contiene mai la password né il suo hash
public record UtenteRisposta(
    Long id,
    String nome,
    String cognome,
    String email,
    RuoloUtente ruolo,
    StatoUtente stato
){
    //trasforma l'Entity in risposta: usato da registrazione, login, profilo e amministrazione
    public static UtenteRisposta daUtente(Utente utente) {
        return new UtenteRisposta(
            utente.getId(),
            utente.getNome(),
            utente.getCognome(),
            utente.getEmail(),
            utente.getRuolo(),
            utente.getStato()
        );
    }
}
