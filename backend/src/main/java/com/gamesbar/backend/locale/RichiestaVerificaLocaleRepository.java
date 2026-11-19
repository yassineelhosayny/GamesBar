package com.gamesbar.backend.locale;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RichiestaVerificaLocaleRepository extends JpaRepository<RichiestaVerificaLocale, Long> {

    //le richieste con un certo stato, dalla più vecchia
    List<RichiestaVerificaLocale> findByStatoOrderByDataRichiestaAsc(StatoRichiestaVerifica stato);

    //tutte le richieste di un locale, anche quelle rifuitati<mi serve per il storico dopo>
    List<RichiestaVerificaLocale> findByLocaleIdOrderByIdAsc(Long localeId);
}
