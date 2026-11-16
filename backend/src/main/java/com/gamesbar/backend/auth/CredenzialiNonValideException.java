package com.gamesbar.backend.auth;

//lanciata quando email o password sono sbagliate.
//il messaggio è uguale nei due casi, così non si scopre quali email sono registrate
public class CredenzialiNonValideException extends RuntimeException {

    public CredenzialiNonValideException() {
        super("email o password non corretti.");
    }
}
