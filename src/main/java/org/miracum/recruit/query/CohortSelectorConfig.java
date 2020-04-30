package org.miracum.recruit.query;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "query.selector")
public class CohortSelectorConfig {

    private final Set<String> list;

    public CohortSelectorConfig() {
        this.list = new HashSet<>();
    }

    public Set<String> getMatchLabels() {
        return this.list;
    }
}
