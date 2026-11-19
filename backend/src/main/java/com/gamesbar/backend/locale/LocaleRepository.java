package com.gamesbar.backend.locale;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocaleRepository extends JpaRepository<Locale, Long> {

    //tutti i locali gestiti dal utente
    List<Locale> findByAmministratoreIdOrderByIdAsc(Long amministratoreId);
}
