package com.gamesbar.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gamesbar.backend.utente.Utente;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

//crea e controlla i token JWT del login
@Service
public class JwtService {
    private final SecretKey chiave;
    private final long durataMillisecondi;

    //@Value legge i valori da application.properties (jwt.segreto arriva dal file .env)
    public JwtService(@Value("${jwt.segreto}") String segreto,@Value("${jwt.durata-minuti}") long durataMinuti) {
        //la chiave deve essere lunga almeno 32 caratteri, altrimenti jjwt dà errore
        this.chiave = Keys.hmacShaKeyFor(segreto.getBytes(StandardCharsets.UTF_8));
        this.durataMillisecondi = durataMinuti * 60 * 1000;
    }

    //crea un token firmato che contiene l'email dell'utente e la scadenza
    public String creaToken(Utente utente) {
        Date adesso = new Date();
        Date scadenza = new Date(adesso.getTime() + durataMillisecondi);

        return Jwts.builder()
            .subject(utente.getEmail())     //"di chi è" il token
            .issuedAt(adesso)               //quando è stato creato
            .expiration(scadenza)           //quando scade
            .signWith(chiave)               //firma: senza la chiave nessuno può falsificarlo
            .compact();                     //trasforma tutto in una stringa
    }

    //restituisce l'email scritta nel token,
    //oppure null se il token è falso, modificato o scaduto
    public String leggiEmail(String token) {
        try {
            Claims dati = Jwts.parser()
                .verifyWith(chiave)         //controlla la firma con la nostra chiave
                .build()
                .parseSignedClaims(token)   //controlla anche la scadenza
                .getPayload();
            return dati.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
