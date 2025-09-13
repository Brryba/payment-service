package innowise.payments_service.controller;

import innowise.payments_service.dto.payment.PaymentPageResponseDto;
import innowise.payments_service.dto.payment.PaymentResponseDto;
import innowise.payments_service.service.PaymentService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping()
    public PaymentPageResponseDto getUserPaymentPage(@RequestHeader("X-User-Id") Long userId,
                                                     @RequestParam(defaultValue = "0") @Min(0) Integer page,
                                                     @RequestParam(defaultValue = "10") @Positive Integer size) {
        return paymentService.getPaymentsByUserId(userId, page, size);
    }

    @GetMapping(params = "orderId")
    public List<PaymentResponseDto> getAllOrderPayments(@RequestHeader("X-User-Id") Long userId,
                                                        @RequestParam() @NotNull Long orderId) {
        return paymentService.getPaymentsByOrderId(userId, orderId);
    }
}
