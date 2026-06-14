package com.ecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOrderConfirmation(String to, String orderId, String status) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Order Confirmation - Order #" + orderId);
            message.setText("Your order #" + orderId + " has been " + status + ".");

            mailSender.send(message);
            log.info("Order confirmation email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    @Async
    public void sendPaymentConfirmation(String to, String orderId, String paymentId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Payment Confirmed - Order #" + orderId);
            message.setText("Payment " + paymentId + " confirmed for order #" + orderId + ".");

            mailSender.send(message);
            log.info("Payment confirmation email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send payment email to: {}", to, e);
        }
    }

    @Async
    public void sendShippingUpdate(String to, String orderId, String status) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Shipping Update - Order #" + orderId);
            message.setText("Your order #" + orderId + " is now " + status + ".");

            mailSender.send(message);
            log.info("Shipping update email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send shipping email to: {}", to, e);
        }
    }
}
