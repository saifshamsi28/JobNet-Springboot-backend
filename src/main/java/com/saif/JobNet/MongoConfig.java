package com.saif.JobNet;

import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableMongoRepositories(basePackages = "com.saif.JobNet.repositories")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        System.out.println("mongo uri: "+mongoUri);
        return "JobNetDatabaseFirstCluster"; // Specify your MongoDB database name
    }

    @Override
    public com.mongodb.client.MongoClient mongoClient() {
        System.out.println("mongo uri: "+mongoUri);
        return MongoClients.create(mongoUri); // Replace with your MongoDB URI
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        System.out.println("mongo uri: "+mongoUri);
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
}
