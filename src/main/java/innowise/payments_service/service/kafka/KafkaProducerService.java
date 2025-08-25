package innowise.payments_service.service.kafka;

import innowise.payments_service.dto.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, PaymentResponseDto> kafkaTemplate;

    private static final String TOPIC = "payments";

    public void sendCreatePaymentEvent(PaymentResponseDto payment) {
        log.info("Sending CREATE_PAYMENT event to {} for order {}", TOPIC, payment.getOrderId());

        try {
            kafkaTemplate.send(TOPIC, payment);
            log.info("Successfully sent CREATE_PAYMENT event to {} for order {}", TOPIC, payment.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to send CREATE_PAYMENT event to {} for order {}", TOPIC, payment.getOrderId());
        }
    }
}