package com.saif.JobNet;

import com.mongodb.client.MongoClients;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.saif.JobNet.repositories")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    @NonNull
    protected String getDatabaseName() {
        return "JobNetDatabaseFirstCluster"; // Specify your MongoDB database name
    }

    @Override
    @NonNull
    public com.mongodb.client.MongoClient mongoClient() {
        return MongoClients.create(mongoUri); // Replace with your MongoDB URI
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        System.out.println("mongo uri: "+mongoUri);
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
}
