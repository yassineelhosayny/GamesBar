package com.gamesbar.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.gamesbar.backend.utente.UtenteRepository;

@Configuration  //spring lo legge come una configurazione nel avvio
public class SecurityConfig {

    //Spring crea il Bean passwordEncoder e lo salva in application context
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //la class fornisce "match() e .encode()" che servono per il controllo del password.
    }

    //regole di sicurezza di tutte le richieste HTTP
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,UtenteRepository utenteRepository) throws Exception {
        JwtAuthenticationFilter filtroJwt = new JwtAuthenticationFilter(jwtService, utenteRepository);

        //CSRF serve per i cookie di sessione: noi usiamo il token, quindi lo spegniamo
        http.csrf(csrf -> csrf.disable());

        //STATELESS: il server non salva sessioni, ogni richiesta porta il suo token
        http.sessionManagement(sessione -> sessione.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        //chi può chiamare cosa (le regole vengono lette dall'alto in basso)
        http.authorizeHttpRequests(regole -> regole
            .requestMatchers(HttpMethod.POST, "/api/auth/registrazione", "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/tipi-gioco/**").permitAll()
            .requestMatchers("/error").permitAll()      //pagina errori interna di Spring
            .anyRequest().authenticated()               //tutto il resto: serve il token
        );

        //senza token valido su un endpoint protetto → 401 Unauthorized
        http.exceptionHandling(errori -> errori.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        //il nostro filtro JWT viene eseguito prima di quello standard di Spring
        http.addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
