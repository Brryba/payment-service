package innowise.payments_service.service;

import innowise.payments_service.dto.PaymentRequestDto;
import innowise.payments_service.dto.PaymentResponseDto;
import innowise.payments_service.entity.Payment;
import innowise.payments_service.entity.Status;
import innowise.payments_service.exception.order.OrderNotFoundException;
import innowise.payments_service.mapper.PaymentMapper;
import innowise.payments_service.repository.PaymentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomNumberAPIClient randomNumberAPIClient;

    public PaymentResponseDto createPayment(@Valid PaymentRequestDto paymentRequest) {
        log.info("Payment response for order {} received", paymentRequest.getOrderId());
        Payment payment = paymentMapper.toPaymentFromRequest(paymentRequest);

        log.info("Receiving random number from external API...");
        try {
            Integer response = randomNumberAPIClient.getRandomNumberAsInteger();
            log.info("Received random number from external API: {}", response);
            payment.setStatus(response % 2 == 0 ? Status.COMPLETED : Status.FAILED);
        } catch (Exception e) {
            log.error("Error getting random number from external API. Payment failed", e);
            payment.setStatus(Status.FAILED);
        }

        payment.setTimestamp(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        log.info("Payment for order {} was saved with id {}", paymentRequest.getOrderId(), payment.getId());

        return paymentMapper.toPaymentResponseDto(payment);
    }

    public List<PaymentResponseDto> getPaymentsByOrderId(Long orderId) {
        log.info("Searching for all payment with order id {}", orderId);

        List<Payment> payments = paymentRepository.findAllByOrderId(orderId);

        if (payments == null || payments.isEmpty()) {
            log.warn("No Payments found for orderId: {}", orderId);
            throw new OrderNotFoundException("There are no payments associated with this order");
        }

        log.info("Found {} payments associated with orderId: {}", payments.size(), orderId);

        return payments.stream().map(paymentMapper::toPaymentResponseDto).toList();
    }

    public List<PaymentResponseDto> getPaymentsByUserId(Long userId) {
        log.info("Searching for all payment with user id {}", userId);

        List<Payment> payments = paymentRepository.findAllByUserId(userId);

        if (payments == null || payments.isEmpty()) {
            log.warn("No Payments found for user with id: {}", userId);
            throw new OrderNotFoundException("There are no payments associated with this user");
        }

        log.info("Found {} payments associated with userId: {}", payments.size(), userId);

        return payments.stream().map(paymentMapper::toPaymentResponseDto).toList();
    }

    public List<PaymentResponseDto> getPaymentsByStatus(Status status) {
        log.info("Searching for all payment with status {}", status);

        List<Payment> payments = paymentRepository.findAllByStatus(status);

        if (payments == null || payments.isEmpty()) {
            log.warn("No Payments found for status: {}", status);
            throw new OrderNotFoundException("There are no payments with the status");
        }

        log.info("Found {} payments associated with status: {}", payments.size(), status);

        return payments.stream().map(paymentMapper::toPaymentResponseDto).toList();
    }

    public BigDecimal countPaymentsSumForDatePeriod(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Receiving the sum of all payments from {} to {}", startDate.toString(), endDate.toString());
        return paymentRepository.countTotalPaymentAmountInDatePeriod(startDate, endDate).bigDecimalValue();
    }
}
