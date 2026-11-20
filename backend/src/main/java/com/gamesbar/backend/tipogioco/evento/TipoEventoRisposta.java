package com.gamesbar.backend.tipogioco.evento;

//descrive il JSON di un tipo di evento
public record TipoEventoRisposta(
    Long id,
    String codiceTipoGioco,
    String codice,
    String nome,
    String descrizione,
    boolean abilitata
){
    public static TipoEventoRisposta daEvento(TipoEventoGioco evento) {
        return new TipoEventoRisposta(
            evento.getId(),
            evento.getCodiceTipoGioco(),
            evento.getCodice(),
            evento.getNome(),
            evento.getDescrizione(),
            evento.isAbilitata()
        );
    }
}
