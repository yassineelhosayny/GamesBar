package com.gamesbar.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration  //spring lo legge come una configurazione nel avvio
public class SecurityConfig {

    //Spring crea il Bean passwordEncoder e lo salva in application context
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //la class fornisce "match() e .encode()" che servono per il controllo del password.
    }
}
