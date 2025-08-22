package innowise.payments_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentRequestDto {
    @NotBlank(message = "Order Id is required")
    private Long orderId;
    @NotBlank(message = "User Id is required")
    private Long userId;
    @NotBlank(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment Amount can't be negative or 0")
    @Digits(integer = 10, fraction = 2, message = "Must be ≤ 10 whole digits and exactly 2 decimal places")
    private BigDecimal paymentAmount;
}
