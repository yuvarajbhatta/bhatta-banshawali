package com.familytree.services.email;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String rawToken, String preferredLanguage);

    void sendPasswordResetEmail(String toEmail, String rawToken, String preferredLanguage);
}
