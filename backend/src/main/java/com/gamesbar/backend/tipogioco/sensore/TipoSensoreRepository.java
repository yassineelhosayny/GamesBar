package com.gamesbar.backend.tipogioco.sensore;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoSensoreRepository extends JpaRepository<TipoSensore, Long> {

    //i sensori di un tipo di gioco
    List<TipoSensore> findByTipoGiocoIdOrderByCodiceAsc(Long tipoGiocoId);

    //il codice è UNIQUE dentro lo stesso gioco
    boolean existsByTipoGiocoIdAndCodice(Long tipoGiocoId, String codice);
}
