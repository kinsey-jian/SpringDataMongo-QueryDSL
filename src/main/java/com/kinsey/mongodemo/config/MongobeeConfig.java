package com.kinsey.mongodemo.config;

import com.github.mongobee.Mongobee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by zj on 2018/8/22
 */
@Configuration
@EnableConfigurationProperties(MongoProperties.class)
public class MongobeeConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoProperties properties;

    @Bean
    public Mongobee mongobee() {
        System.err.println(properties.getUri());
        Mongobee mongobee = new Mongobee(properties.getUri());
        mongobee.setChangeLogsScanPackage("com.kinsey.mongodemo.shell");
        mongobee.setMongoTemplate(mongoTemplate);
        return mongobee;
    }
}
