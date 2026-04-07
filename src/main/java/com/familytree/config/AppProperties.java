package com.familytree.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Admin admin = new Admin();
    private final DevUser devUser = new DevUser();
    private final Lineage lineage = new Lineage();

    public Admin getAdmin() {
        return admin;
    }

    public Lineage getLineage() {
        return lineage;
    }

    public DevUser getDevUser() {
        return devUser;
    }

    public static class Admin {
        private String username = "";
        private String password = "";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Lineage {
        private String defaultLastName = "";
        private String defaultGender = "";

        public String getDefaultLastName() {
            return defaultLastName;
        }

        public void setDefaultLastName(String defaultLastName) {
            this.defaultLastName = defaultLastName;
        }

        public String getDefaultGender() {
            return defaultGender;
        }

        public void setDefaultGender(String defaultGender) {
            this.defaultGender = defaultGender;
        }
    }

    public static class DevUser {
        private String username = "";
        private String password = "";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
