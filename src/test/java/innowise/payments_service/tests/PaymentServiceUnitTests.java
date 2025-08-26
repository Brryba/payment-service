package innowise.payments_service.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import innowise.payments_service.dto.PaymentRequestDto;
import innowise.payments_service.dto.PaymentResponseDto;
import innowise.payments_service.entity.Payment;
import innowise.payments_service.entity.Status;
import innowise.payments_service.exception.order.OrderNotFoundException;
import innowise.payments_service.mapper.PaymentMapper;
import innowise.payments_service.repository.PaymentRepository;
import innowise.payments_service.service.PaymentService;
import innowise.payments_service.service.RandomNumberAPIClient;
import innowise.payments_service.service.kafka.KafkaProducerService;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTests {
    @Mock
    private PaymentRepository paymentRepository;

    @Spy
    private PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);

    @Mock
    private RandomNumberAPIClient randomNumberAPIClient;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private PaymentService paymentService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentRequestDto paymentRequestDto;
    private PaymentResponseDto paymentResponseDto;
    private Payment payment;

    @BeforeAll
    static void setUp() {
        objectMapper.findAndRegisterModules();
    }

    @BeforeEach
    void setUpStubs() throws IOException {
        paymentRequestDto = objectMapper.readValue(
                new ClassPathResource("json/payment-request.json").getFile(),
                PaymentRequestDto.class
        );

        paymentResponseDto = objectMapper.readValue(
                new ClassPathResource("json/payment-response.json").getFile(),
                PaymentResponseDto.class
        );

        payment = paymentMapper.toPaymentFromRequest(paymentRequestDto);
        payment.setTimestamp(paymentResponseDto.getTimestamp());
        payment.setId(paymentResponseDto.getId());
        payment.setStatus(paymentResponseDto.getStatus());
    }

    @Test
    void testCreatePayment_paymentCompleted() {
        when(randomNumberAPIClient.getRandomNumberAsInteger()).thenReturn(30);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

        assertAll(() -> {
            assertNotNull(responseDto);
            assertEquals(paymentRequestDto.getOrderId(), responseDto.getOrderId());
            assertEquals(Status.COMPLETED, responseDto.getStatus());
        });

        verify(kafkaProducerService).sendCreatePaymentEvent(any(PaymentResponseDto.class));
    }

    @Test
    void testCreatePayment_paymentFailed() {
        when(randomNumberAPIClient.getRandomNumberAsInteger()).thenReturn(25);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

        assertAll(() -> {
            assertNotNull(responseDto);
            assertEquals(paymentRequestDto.getOrderId(), responseDto.getOrderId());
            assertEquals(Status.FAILED, responseDto.getStatus());
        });

        verify(kafkaProducerService).sendCreatePaymentEvent(any(PaymentResponseDto.class));
    }

    @Test
    void testCreatePayment_ApiNotResponding() {
        doThrow(FeignException.FeignServerException.class).when(randomNumberAPIClient).getRandomNumberAsInteger();
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

        assertAll(() -> {
            assertNotNull(responseDto);
            assertEquals(paymentRequestDto.getOrderId(), responseDto.getOrderId());
            assertEquals(Status.FAILED, responseDto.getStatus());
        });

        verify(kafkaProducerService).sendCreatePaymentEvent(any(PaymentResponseDto.class));
    }

    @Test
    void testGetPaymentsByOrderId() {
        when(paymentRepository.findAllByOrderId(1L)).thenReturn(List.of(payment));

        List<PaymentResponseDto> responseDtos = paymentService.getPaymentsByOrderId(1L);

        assertAll(() -> {
            assertNotNull(responseDtos);
            assertEquals(1, responseDtos.size());
            assertEquals(payment.getId(), responseDtos.getFirst().getId());
            assertEquals(1L, responseDtos.getFirst().getOrderId());
        });
    }

    @Test
    void testGetPaymentsByOrderId_NoPaymentsFound() {
        when(paymentRepository.findAllByOrderId(1L)).thenReturn(List.of());

        assertThrows(OrderNotFoundException.class, () -> paymentService.getPaymentsByOrderId(1L));
    }

    @Test
    void testGetPaymentsByStatus() {
        when(paymentRepository.findAllByStatus(Status.COMPLETED)).thenReturn(List.of(payment));

        List<PaymentResponseDto> responseDtos = paymentService.getPaymentsByStatus(Status.COMPLETED);

        assertAll(() -> {
            assertNotNull(responseDtos);
            assertEquals(1, responseDtos.size());
            assertEquals(payment.getId(), responseDtos.getFirst().getId());
            assertEquals(1L, responseDtos.getFirst().getOrderId());
            assertEquals(Status.COMPLETED, responseDtos.getFirst().getStatus());
        });
    }

    @Test
    void testGetPaymentsStatus_NoPaymentsFound() {
        when(paymentRepository.findAllByStatus(Status.FAILED)).thenReturn(List.of());

        assertThrows(OrderNotFoundException.class, () -> paymentService.getPaymentsByStatus(Status.FAILED));
    }

    @Test
    void testGetPaymentsByUserId() {
        when(paymentRepository.findAllByUserId(1L)).thenReturn(List.of(payment));

        List<PaymentResponseDto> responseDtos = paymentService.getPaymentsByUserId(1L);

        assertAll(() -> {
            assertNotNull(responseDtos);
            assertEquals(1, responseDtos.size());
            assertEquals(payment.getId(), responseDtos.getFirst().getId());
            assertEquals(1L, responseDtos.getFirst().getUserId());
        });
    }

    @Test
    void testGetPaymentsByUserId_NoOrdersFound() {
        when(paymentRepository.findAllByUserId(1L)).thenReturn(List.of());

        assertThrows(OrderNotFoundException.class, () -> paymentService.getPaymentsByUserId(1L));
    }

    @Test
    void testGetPaymentSumForDatePeriod() {
        LocalDateTime startDate = LocalDate.of(2024, 1, 1).atStartOfDay(),
                endDate = LocalDate.of(2024, 12, 31).atStartOfDay();
        BigDecimal sum = BigDecimal.valueOf(1234567.89);
        when(paymentRepository.countTotalPaymentAmountInDatePeriod(startDate, endDate))
                .thenReturn(new Decimal128(sum));

        assertEquals(sum, paymentService.countPaymentsSumForDatePeriod(startDate, endDate));
    }
}
