package com.gymtracker.infrastructure.config;

import com.gymtracker.domain.WeightUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityUsersProperties {

    private Users users = new Users();

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public Map<String, UserDefinition> definitions() {
        Map<String, UserDefinition> definitions = new LinkedHashMap<>();
        definitions.put("user1", withDefaults(users.getUser1(), UUID.fromString("11111111-1111-1111-1111-111111111111"), "password1", WeightUnit.KG));
        definitions.put("user2", withDefaults(users.getUser2(), UUID.fromString("22222222-2222-2222-2222-222222222222"), "password2", WeightUnit.LBS));
        return definitions;
    }

    private UserDefinition withDefaults(UserDefinition definition, UUID id, String password, WeightUnit weightUnit) {
        UserDefinition resolved = definition == null ? new UserDefinition() : definition;
        if (resolved.getId() == null) {
            resolved.setId(id);
        }
        if (resolved.getPassword() == null || resolved.getPassword().isBlank()) {
            resolved.setPassword(password);
        }
        if (resolved.getPreferredWeightUnit() == null) {
            resolved.setPreferredWeightUnit(weightUnit);
        }
        return resolved;
    }

    public static class Users {
        private UserDefinition user1 = new UserDefinition();
        private UserDefinition user2 = new UserDefinition();

        public UserDefinition getUser1() {
            return user1;
        }

        public void setUser1(UserDefinition user1) {
            this.user1 = user1;
        }

        public UserDefinition getUser2() {
            return user2;
        }

        public void setUser2(UserDefinition user2) {
            this.user2 = user2;
        }
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



