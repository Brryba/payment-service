package innowise.payments_service.dto.payment;

import innowise.payments_service.entity.Status;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(exclude = "timestamp")
public class PaymentResponseDto {
    private String id;
    private Status status;
    private LocalDateTime timestamp;
    private BigDecimal paymentAmount;
    private Long userId;
    private Long orderId;
}