package innowise.payments_service.service.kafka;

import innowise.payments_service.dto.payment.PaymentRequestDto;
import innowise.payments_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final PaymentService paymentService;

    @KafkaListener(topics = "orders", groupId = "order-service-group")
    public void listenCreateOrderEvents(PaymentRequestDto payment) {
        log.info("Received {} event from payments topic for {} order.",
                payment.getEventType(), payment.getOrderId());

        paymentService.createPayment(payment);
    }
}