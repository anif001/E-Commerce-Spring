package com.ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItemResponse {

    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;
}
