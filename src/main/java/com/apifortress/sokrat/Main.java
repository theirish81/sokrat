package com.apifortress.sokrat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * © 2018 Simone Pezzano
 *
 * @author Simone Pezzano
 **/
@ComponentScan
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args){
        log.info("Application booting");
        new AnnotationConfigApplicationContext(Main.class);
    }
}
