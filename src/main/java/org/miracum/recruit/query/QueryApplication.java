package org.miracum.recruit.query;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class QueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryApplication.class, args);
    }

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
