package innowise.payments_service.dto;

import innowise.payments_service.entity.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {
    private String eventType;
    private String id;
    private Status status;
    private LocalDateTime timestamp;
    private BigDecimal paymentAmount;
    private Long userId;
    private Long orderId;
}
