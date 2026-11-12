package com.gamesbar.backend.utente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UtenteRepository extends JpaRepository<Utente, Long> {

    //Optional perché l'email potrebbe non appartenere a nessun utente;
    //l'email va passata già normalizzata (trim + minuscolo), come la salva Utente
    Optional<Utente> findByEmail(String email);

    //SELECT di esistenza, senza caricare l'intero utente
    boolean existsByEmail(String email);
}
