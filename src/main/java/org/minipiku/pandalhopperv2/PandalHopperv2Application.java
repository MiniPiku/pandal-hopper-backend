package org.minipiku.pandalhopperv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PandalHopperv2Application {

    public static void main(String[] args) {
        SpringApplication.run(PandalHopperv2Application.class, args);
    }

}
