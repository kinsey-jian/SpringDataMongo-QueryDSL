package com.kinsey.mongodemo;

import com.kinsey.mongodemo.config.CascadeSaveMongoEventListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@EnableMongoAuditing
@SpringBootApplication
public class MongoDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongoDemoApplication.class, args);
    }

    @Bean
    public CascadeSaveMongoEventListener cascadeControlMongoEventListener() {
        return new CascadeSaveMongoEventListener();
    }
}
