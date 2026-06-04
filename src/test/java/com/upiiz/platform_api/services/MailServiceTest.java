package com.upiiz.platform_api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void passwordResetEmailDoesNotUseSmtpWhenCredentialsAreMissing() {
        MailService service = new MailService(mailSender, "", "", "https://app.example/");

        service.sendPasswordResetEmail("user@example.com", "token-123");

        verifyNoInteractions(mailSender);
    }

    @Test
    void passwordResetEmailUsesConfiguredSender() {
        MailService service = new MailService(
                mailSender,
                "no-reply@example.com",
                "secret",
                "https://app.example/"
        );

        service.sendPasswordResetEmail("user@example.com", "token-123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@example.com", message.getFrom());
        assertEquals("user@example.com", message.getTo()[0]);
        assertEquals("Recuperacion de contrasena", message.getSubject());
        assertTrue(message.getText().contains("https://app.example/reset-password?token=token-123"));
    }
}
