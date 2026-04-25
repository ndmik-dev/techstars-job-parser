package io.ndmik.tsparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TechstarsJobParserApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechstarsJobParserApplication.class, args);
    }

}
