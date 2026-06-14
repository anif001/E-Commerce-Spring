package com.ecommerce.controller;

import com.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment integration endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order/{orderId}")
    @Operation(summary = "Create Razorpay order for checkout")
    public ResponseEntity<Map<String, String>> createRazorpayOrder(@PathVariable Long orderId) {
        String razorpayOrderId = paymentService.createRazorpayOrder(orderId);
        return ResponseEntity.ok(Map.of(
                "razorpayOrderId", razorpayOrderId,
                "status", "created"
        ));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature")
    public ResponseEntity<Map<String, String>> verifyPayment(
            @RequestBody Map<String, String> paymentData) {
        paymentService.verifyPayment(
                paymentData.get("razorpayOrderId"),
                paymentData.get("razorpayPaymentId"),
                paymentData.get("signature")
        );
        return ResponseEntity.ok(Map.of("status", "verified"));
    }
}
