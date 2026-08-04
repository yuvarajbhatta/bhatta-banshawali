package com.familytree.services.email;

public interface EmailService {

    void sendVerificationOtpEmail(String toEmail, String code, String preferredLanguage);

    void sendPasswordResetEmail(String toEmail, String rawToken, String preferredLanguage);

    void sendAdminAccessOtpEmail(String toEmail, String code, String preferredLanguage);
}
