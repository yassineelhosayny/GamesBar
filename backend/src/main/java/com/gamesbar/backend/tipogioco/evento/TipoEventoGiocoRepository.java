package com.gamesbar.backend.tipogioco.evento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoEventoGiocoRepository extends JpaRepository<TipoEventoGioco, Long> {

    //gli eventi di un tipo di gioco
    List<TipoEventoGioco> findByTipoGiocoIdOrderByCodiceAsc(Long tipoGiocoId);

    //cerca un evento DENTRO un gioco (es. TIRO_RILEVATO delle FRECCETTE)
    Optional<TipoEventoGioco> findByTipoGiocoIdAndCodice(Long tipoGiocoId, String codice);

    //il codice è UNIQUE dentro lo stesso gioco
    boolean existsByTipoGiocoIdAndCodice(Long tipoGiocoId, String codice);
}
