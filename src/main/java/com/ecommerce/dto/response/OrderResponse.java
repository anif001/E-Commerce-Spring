package com.ecommerce.dto.response;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {

    private Long id;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String razorpayOrderId;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
}
