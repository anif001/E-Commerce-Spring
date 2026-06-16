package com.ecommerce.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendOrderConfirmation_ShouldSendEmail() {
        emailService.sendOrderConfirmation("test@example.com", "123", "CONFIRMED");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPaymentConfirmation_ShouldSendEmail() {
        emailService.sendPaymentConfirmation("test@example.com", "123", "pay_123");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendShippingUpdate_ShouldSendEmail() {
        emailService.sendShippingUpdate("test@example.com", "123", "SHIPPED");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderConfirmation_ShouldNotThrow_WhenMailFails() {
        doThrow(new RuntimeException("Mail server down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendOrderConfirmation("test@example.com", "123", "CONFIRMED");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
