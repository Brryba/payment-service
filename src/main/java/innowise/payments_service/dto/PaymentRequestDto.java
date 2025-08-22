package innowise.payments_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDto {
    @NotBla
    private Long orderId;
    private Long paymentId;
    private BigDecimal paymentAmount;
}
