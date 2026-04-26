package com.gymtracker.infrastructure.config;

import com.gymtracker.domain.WeightUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityUsersProperties {

    private Map<String, UserDefinition> users = new LinkedHashMap<>();

    public Map<String, UserDefinition> getUsers() {
        return users;
    }

    public void setUsers(Map<String, UserDefinition> users) {
        this.users = users;
    }

    public static class UserDefinition {
        private UUID id;
        private String password;
        private WeightUnit preferredWeightUnit = WeightUnit.KG;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public WeightUnit getPreferredWeightUnit() {
            return preferredWeightUnit;
        }

        public void setPreferredWeightUnit(WeightUnit preferredWeightUnit) {
            this.preferredWeightUnit = preferredWeightUnit;
        }
    }
}

