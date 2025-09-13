package innowise.payments_service.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import innowise.payments_service.dto.payment.PaymentKafkaResponseDto;
import innowise.payments_service.dto.payment.PaymentRequestDto;
import innowise.payments_service.dto.payment.PaymentResponseDto;
import innowise.payments_service.entity.Payment;
import innowise.payments_service.entity.Status;
import innowise.payments_service.exception.order.OrderNotFoundException;
import innowise.payments_service.exception.payment.PaymentNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
import static org.mockito.ArgumentMatchers.eq;
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

    private final int PAGE = 0;
    private final int PAGE_SIZE = 10;
    private final long USER_ID = 1L;
    private final long ORDER_ID = 999L;

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

        PaymentKafkaResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

        assertAll(() -> {
            assertNotNull(responseDto);
            assertEquals(paymentRequestDto.getOrderId(), responseDto.getOrderId());
            assertEquals(Status.COMPLETED, responseDto.getStatus());
        });

        verify(kafkaProducerService).sendCreatePaymentEvent(any(PaymentKafkaResponseDto.class));
    }

    @Test
    void testCreatePayment_paymentFailed() {
        when(randomNumberAPIClient.getRandomNumberAsInteger()).thenReturn(25);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentKafkaResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

        assertAll(() -> {
            assertNotNull(responseDto);
            assertEquals(paymentRequestDto.getOrderId(), responseDto.getOrderId());
            assertEquals(Status.FAILED, responseDto.getStatus());
        });

        verify(kafkaProducerService).sendCreatePaymentEvent(any(PaymentKafkaResponseDto.class));
    }

    @Test
    void testCreatePayment_ApiNotResponding() {
        doThrow(FeignException.FeignServerException.class).when(randomNumberAPIClient).getRandomNumberAsInteger();
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentKafkaResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

        assertAll(() -> {
            assertNotNull(responseDto);
            assertEquals(paymentRequestDto.getOrderId(), responseDto.getOrderId());
            assertEquals(Status.FAILED, responseDto.getStatus());
        });

        verify(kafkaProducerService).sendCreatePaymentEvent(any(PaymentKafkaResponseDto.class));
    }

    @Test
    void testGetPaymentsByOrderId() {
        when(paymentRepository.findAllByOrderId(ORDER_ID)).thenReturn(List.of(payment));

        List<PaymentResponseDto> responseDtos = paymentService.getPaymentsByOrderId(USER_ID, ORDER_ID);

        assertAll(() -> {
            assertNotNull(responseDtos);
            assertEquals(1, responseDtos.size());
            assertEquals(paymentResponseDto, responseDtos.getFirst());
        });
    }

    @Test
    void testGetPaymentsByOrderId_NoPaymentsFound() {
        when(paymentRepository.findAllByOrderId(ORDER_ID)).thenReturn(List.of());

        assertThrows(OrderNotFoundException.class, () -> paymentService.getPaymentsByOrderId(USER_ID, ORDER_ID));
    }

    @Test
    void testGetPaymentsByStatus() {
        when(paymentRepository.findAllByStatus(Status.COMPLETED)).thenReturn(List.of(payment));

        List<PaymentResponseDto> responseDtos = paymentService.getPaymentsByStatus(Status.COMPLETED);

        assertAll(() -> {
            assertNotNull(responseDtos);
            assertEquals(1, responseDtos.size());
            assertEquals(paymentResponseDto, responseDtos.getFirst());
        });
    }

    @Test
    void testGetPaymentsStatus_NoPaymentsFound() {
        when(paymentRepository.findAllByStatus(Status.FAILED)).thenReturn(List.of());

        assertThrows(OrderNotFoundException.class, () -> paymentService.getPaymentsByStatus(Status.FAILED));
    }

    @Test
    void testGetPaymentsByUserId() {
        when(paymentRepository.findAllByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(payment)));

        List<PaymentResponseDto> responseDtos = paymentService.getPaymentsByUserId(USER_ID, PAGE, PAGE_SIZE).getPayments();

        assertAll(() -> {
            assertNotNull(responseDtos);
            assertEquals(1, responseDtos.size());
            assertEquals(paymentResponseDto, responseDtos.getFirst());
        });
    }

    @Test
    void testGetPaymentsByUserId_NoOrdersFound() {
        when(paymentRepository.findAllByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(Page.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentsByUserId(USER_ID, PAGE, PAGE_SIZE));
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
