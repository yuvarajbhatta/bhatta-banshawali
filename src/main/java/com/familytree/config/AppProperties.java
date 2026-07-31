package com.familytree.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Admin admin = new Admin();
    private final DevUser devUser = new DevUser();
    private final Lineage lineage = new Lineage();
    private final Names names = new Names();
    private final Email email = new Email();
    private String frontendBaseUrl = "";

    public Admin getAdmin() {
        return admin;
    }

    public Lineage getLineage() {
        return lineage;
    }

    public DevUser getDevUser() {
        return devUser;
    }

    public Names getNames() {
        return names;
    }

    public Email getEmail() {
        return email;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
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

    public static class Names {
        private boolean backfillMissingNepaliOnStartup = false;

        public boolean isBackfillMissingNepaliOnStartup() {
            return backfillMissingNepaliOnStartup;
        }

        public void setBackfillMissingNepaliOnStartup(boolean backfillMissingNepaliOnStartup) {
            this.backfillMissingNepaliOnStartup = backfillMissingNepaliOnStartup;
        }
    }

    public static class Email {
        private boolean enabled = false;
        private String fromAddress = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }
    }
}
