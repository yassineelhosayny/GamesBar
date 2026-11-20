package com.gamesbar.backend.tipogioco;

public record TipoGiocoRisposta(  //descrive il JSON restituito dall’API.
    Long id,
    String codice,
    String nome,
    String descrizione,
    String versioneGestore,
    boolean isAttivo
){
    //trasforma l'Entity in risposta (usato dalla lista pubblica e da quella dell'admin)
    public static TipoGiocoRisposta daTipoGioco(TipoGioco tipoGioco) {
        return new TipoGiocoRisposta(
            tipoGioco.getId(),
            tipoGioco.getCodice(),
            tipoGioco.getNome(),
            tipoGioco.getDescrizione(),
            tipoGioco.getVersioneGestore(),
            tipoGioco.isAttivo()
        );
    }
}
