package com.gamesbar.backend.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gamesbar.backend.utente.Utente;
import com.gamesbar.backend.utente.UtenteRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//filtro eseguito una volta per ogni richiesta HTTP, PRIMA del controller.
//se trova un token valido dice a Spring Security "questa richiesta è di questo utente"
//(non è un @Component: lo crea SecurityConfig, così Spring non lo registra due volte)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UtenteRepository utenteRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UtenteRepository utenteRepository) {
        this.jwtService = jwtService;
        this.utenteRepository = utenteRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,FilterChain filterChain) throws ServletException, IOException {

        //1. cerco l'intestazione "Authorization: Bearer <token>"
        String intestazione = request.getHeader("Authorization");

        if (intestazione != null && intestazione.startsWith("Bearer ")) {
            String token = intestazione.substring(7);   //tolgo "Bearer "

            //2. controllo il token: null se falso o scaduto
            String email = jwtService.leggiEmail(token);

            if (email != null) {
                //3. rileggo l'utente dal DB: se nel frattempo è stato bloccato, non entra
                Optional<Utente> trovato = utenteRepository.findByEmail(email);

                if (trovato.isPresent() && trovato.get().isAttivo()) {
                    Utente utente = trovato.get();

                    //4. il ruolo diventa una "authority" di Spring (es. ROLE_GIOCATORE)
                    List<SimpleGrantedAuthority> ruoli = new ArrayList<>();
                    ruoli.add(new SimpleGrantedAuthority("ROLE_" + utente.getRuolo().name()));

                    //5. dico a Spring Security chi è l'utente di questa richiesta
                    UsernamePasswordAuthenticationToken autenticazione =
                        new UsernamePasswordAuthenticationToken(utente.getEmail(), null, ruoli);
                    SecurityContextHolder.getContext().setAuthentication(autenticazione);
                }
            }
        }

        //6. in ogni caso la richiesta continua: se l'utente non è autenticato
        //sarà Spring Security a rispondere 401 sugli endpoint protetti
        filterChain.doFilter(request, response);
    }
}
