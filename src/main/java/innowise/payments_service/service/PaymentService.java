package innowise.payments_service.service;

import innowise.payments_service.dto.payment.PaymentKafkaResponseDto;
import innowise.payments_service.dto.payment.PaymentPageResponseDto;
import innowise.payments_service.dto.payment.PaymentRequestDto;
import innowise.payments_service.dto.payment.PaymentResponseDto;
import innowise.payments_service.entity.Payment;
import innowise.payments_service.entity.Status;
import innowise.payments_service.exception.order.OrderNotFoundException;
import innowise.payments_service.exception.payment.PaymentNotFoundException;
import innowise.payments_service.exception.payment.PaymentAccessDeniedException;
import innowise.payments_service.mapper.PaymentMapper;
import innowise.payments_service.repository.PaymentRepository;
import innowise.payments_service.service.kafka.KafkaProducerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final KafkaProducerService kafkaProducerService;
    private final EmailSenderService emailSenderService;

    public PaymentKafkaResponseDto createPayment(@Valid PaymentRequestDto paymentRequest) {
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

        emailSenderService.sendEmail(paymentRequest.getUserId(), payment);

        PaymentKafkaResponseDto paymentResponseDto = paymentMapper.toPaymentKafkaResponseDto(payment);
        kafkaProducerService.sendCreatePaymentEvent(paymentResponseDto);
        return paymentResponseDto;
    }

    public List<PaymentResponseDto> getPaymentsByOrderId(Long userId, Long orderId) {
        log.info("Searching for all payment with order id {}", orderId);

        List<Payment> payments = paymentRepository.findAllByOrderId(orderId);

        if (payments.isEmpty()) {
            log.warn("No Payments found for orderId: {}", orderId);
            throw new OrderNotFoundException("There are no payments associated with this order");
        }
        log.info("Found {} payments associated with orderId: {}", payments.size(), orderId);

        payments.forEach(payment -> validatePaymentOwnership(payment, userId));
        return payments.stream().map(paymentMapper::toPaymentResponseDto).toList();
    }

    public PaymentPageResponseDto getPaymentsByUserId(Long userId, int page, int pageSize) {
        log.info("Searching for all payment with user id {}", userId);

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("timestamp").descending());
        Page<Payment> paymentPage = paymentRepository.findAllByUserId(userId, pageable);

        if (paymentPage.getContent().isEmpty()) {
            log.warn("No Payments found for user with id: {}", userId);
            if (paymentPage.getTotalElements() == 0) {
                throw new PaymentNotFoundException("You haven't done any payments yet");
            } else {
                throw new PaymentNotFoundException("Page " + page + " does not exist. " +
                        "The last page for " + pageSize + " size is " + (paymentPage.getTotalPages() - 1));
            }
        }

        log.info("Found {} payments associated with userId: {}", paymentPage.getContent().size(), userId);

        return PaymentPageResponseDto.builder()
                .payments(paymentPage.stream().map(paymentMapper::toPaymentResponseDto).toList())
                .currentPage(paymentPage.getNumber())
                .pageSize(paymentPage.getSize())
                .lastPage(paymentPage.getTotalPages() - 1)
                .build();
    }

    public List<PaymentResponseDto> getPaymentsByStatus(Status status) {
        log.info("Searching for all payment with status {}", status);

        List<Payment> payments = paymentRepository.findAllByStatus(status);

        if (payments.isEmpty()) {
            log.warn("No Payments found for status: {}", status);
            throw new PaymentNotFoundException("There are no payments with the status");
        }

        log.info("Found {} payments associated with status: {}", payments.size(), status);

        return payments.stream().map(paymentMapper::toPaymentResponseDto).toList();
    }

    public BigDecimal countPaymentsSumForDatePeriod(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Receiving the sum of all payments from {} to {}", startDate.toString(), endDate.toString());
        return paymentRepository.countTotalPaymentAmountInDatePeriod(startDate, endDate).bigDecimalValue();
    }

    private void validatePaymentOwnership(Payment payment, Long userId) {
        if (!payment.getUserId().equals(userId)) {
            log.warn("Access to payment {} was forbidden for user {}", payment.getId(), userId);
            throw new PaymentAccessDeniedException("You are not allowed to see other users payments");
        }
    }
}