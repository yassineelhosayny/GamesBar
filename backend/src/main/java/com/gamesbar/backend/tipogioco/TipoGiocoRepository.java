package com.gamesbar.backend.tipogioco;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoGiocoRepository extends JpaRepository<TipoGioco,Long>{
    List<TipoGioco> findByAttivoTrueOrderByNomeAsc();

    //tutti i tipi, anche quelli non attivi (per l'amministratore del gioco)
    List<TipoGioco> findAllByOrderByNomeAsc();

    //il codice è UNIQUE nel DB: lo controllo prima per dare un errore chiaro (409)
    boolean existsByCodice(String codice);
}

/*
findBy          → cerca le righe dove…
AttivaTrue      → proprietà attiva = true
OrderBy         → ordina per…
NomeAsc         → proprietà nome, ordine crescente */

/*

jpa ti da questi metodi;
findAll();
findById(id);
save(categoria);
deleteById(id);
existsById(id);
*/