package com.gamesbar.backend.tipogioco.sensore;

//descrive il JSON di un tipo di sensore, con il codice dell'evento che produce
public record TipoSensoreRisposta(
    Long id,
    String codiceTipoGioco,
    String codice,
    String nome,
    String descrizione,
    String codiceEventoProdotto,
    boolean obbligatorio,
    boolean attivo
){
    //il codice dell'evento non è dentro TipoSensore (lì c'è solo l'id): lo passa il Service
    public static TipoSensoreRisposta daSensore(TipoSensore sensore, String codiceEventoProdotto) {
        return new TipoSensoreRisposta(
            sensore.getId(),
            sensore.getCodiceTipoGioco(),
            sensore.getCodice(),
            sensore.getNome(),
            sensore.getDescrizione(),
            codiceEventoProdotto,
            sensore.isObbligatorio(),
            sensore.isAttivo()
        );
    }
}
