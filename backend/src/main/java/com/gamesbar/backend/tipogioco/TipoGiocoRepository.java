package com.gamesbar.backend.tipogioco;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoGiocoRepository extends JpaRepository<TipoGioco,Long>{
    List<TipoGioco> findByAttivoTrueOrderByNomeAsc(); 
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