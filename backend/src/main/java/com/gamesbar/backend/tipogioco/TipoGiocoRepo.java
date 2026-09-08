package com.gamesbar.backend.tipogioco;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoGiocoRepo extends JpaRepository<TipoGioco,Long>{
    List<TipoGioco> findByAttivoTrueOrderByNomeAsc(); //perché non ci public o private?
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