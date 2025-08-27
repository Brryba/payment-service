package innowise.payments_service.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import innowise.payments_service.dto.PaymentRequestDto;
import innowise.payments_service.dto.PaymentResponseDto;
import innowise.payments_service.entity.Payment;
import innowise.payments_service.entity.Status;
import innowise.payments_service.repository.PaymentRepository;
import innowise.payments_service.service.PaymentService;
import innowise.payments_service.service.kafka.KafkaConsumerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PaymentServiceIntegrationTest {
    @Value("${external-api.random-number.url}")
    private String externalApiUrl;

    @Container
    static GenericContainer<?> mongoDBContainer = new GenericContainer<>(DockerImageName
            .parse("mongo:6.0.8"))
            .withExposedPorts(27017)
            .withEnv("MONGO_INITDB_DATABASE", "payments")
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "user")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", "password");

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka"));

    static WireMockServer wireMockServer;

    @Autowired
    private KafkaTemplate<String, PaymentRequestDto> kafkaRequestTemplate;

    @MockitoSpyBean
    private KafkaConsumerService kafkaConsumerService;

    @MockitoBean
    private KafkaTemplate<String, PaymentResponseDto> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    private PaymentRequestDto paymentRequestDto;
    private final int SUCCESS_API_RESPONSE = 2;
    private final int FAIL_API_RESPONSE = 3;

    @DynamicPropertySource
    static void mongodbContainerProperties(DynamicPropertyRegistry registry) {
        mongoDBContainer.start();
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", mongoDBContainer::getFirstMappedPort);
        String mongoDbUrl = "mongodb://" + mongoDBContainer.getHost() + ":" + mongoDBContainer.getFirstMappedPort()
                + "/payments";
        registry.add("spring.liquibase.url", () -> mongoDbUrl);
    }

    @DynamicPropertySource
    static void kafkaContainerProperties(DynamicPropertyRegistry registry) {
        kafkaContainer.start();
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8081));
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @BeforeEach
    void setUp() throws IOException {
        paymentRequestDto = objectMapper.readValue(
                new ClassPathResource("json/payment-request.json").getFile(),
                PaymentRequestDto.class
        );
    }

    @AfterEach
    void cleanUpDatabase() {
        paymentRepository.deleteAll();
        WireMock.reset();
    }

    @AfterAll
    static void stopContainers() {
        mongoDBContainer.stop();
        kafkaContainer.stop();
    }

    void configureWireMockResponse(Integer neededValue) {
        stubFor(WireMock.get(externalApiUrl).willReturn(ok()
                .withBody(neededValue.toString())
        ));
    }

    @Test
    void checkPaymentPresentInDatabaseAfterSave() {
        configureWireMockResponse(SUCCESS_API_RESPONSE);
        PaymentResponseDto response = paymentService.createPayment(paymentRequestDto);

        Payment savedPayment = paymentRepository.findById(response.getId()).orElse(null);

        assertNotNull(savedPayment);
        assertAll(() -> {
            assertEquals(Status.COMPLETED, savedPayment.getStatus());
            assertEquals(response.getStatus(), savedPayment.getStatus());
        });
    }

    @Test
    void testPaymentsSavesAsFailed_ifExternalAPIFails() {
        stubFor(WireMock.get(externalApiUrl).willReturn(serverError()));
        PaymentResponseDto response = paymentService.createPayment(paymentRequestDto);

        Payment savedPayment = paymentRepository.findById(response.getId()).orElse(null);

        assertNotNull(savedPayment);
        assertAll(() -> {
            assertEquals(Status.FAILED, savedPayment.getStatus());
            assertEquals(response.getStatus(), savedPayment.getStatus());
        });
    }

    @Test
    void checkFindPaymentsByStatus() {
        configureWireMockResponse(SUCCESS_API_RESPONSE);
        PaymentResponseDto completedPayment = paymentService.createPayment(paymentRequestDto);
        configureWireMockResponse(FAIL_API_RESPONSE);
        PaymentResponseDto failedPayment = paymentService.createPayment(paymentRequestDto);

        List<PaymentResponseDto> completedPayments = paymentService.getPaymentsByStatus(Status.COMPLETED);
        assertAll(() -> {
            assertNotNull(completedPayments);
            assertEquals(1, completedPayments.size());
            assertEquals(completedPayment.getId(), completedPayments.getFirst().getId());
            assertNotEquals(failedPayment.getId(), completedPayments.getFirst().getId());
        });
    }

    @Test
    void checkFindPaymentsByOrderId() {
        configureWireMockResponse(SUCCESS_API_RESPONSE);
        paymentService.createPayment(paymentRequestDto);
        paymentRequestDto.setOrderId(999L);
        paymentService.createPayment(paymentRequestDto);

        List<PaymentResponseDto> completedPayments = paymentService.getPaymentsByOrderId(999L);
        assertAll(() -> {
            assertNotNull(completedPayments);
            assertEquals(1, completedPayments.size());
            assertEquals(999L, completedPayments.getFirst().getOrderId());
        });
    }

    @Test
    void checkFindPaymentsByUserId() {
        configureWireMockResponse(SUCCESS_API_RESPONSE);
        paymentService.createPayment(paymentRequestDto);
        paymentRequestDto.setUserId(999L);
        paymentService.createPayment(paymentRequestDto);

        List<PaymentResponseDto> completedPayments = paymentService.getPaymentsByUserId(999L);
        assertAll(() -> {
            assertNotNull(completedPayments);
            assertEquals(1, completedPayments.size());
            assertEquals(999L, completedPayments.getFirst().getUserId());
        });
    }

    @Test
    void checkCountPaymentsSumForDatePeriod() {
        configureWireMockResponse(SUCCESS_API_RESPONSE);
        PaymentResponseDto responseDto1 = paymentService.createPayment(paymentRequestDto);
        PaymentResponseDto responseDto2 = paymentService.createPayment(paymentRequestDto);
        Payment payment1 = paymentRepository.findById(responseDto1.getId()).orElse(null);

        assertNotNull(payment1);
        payment1.setTimestamp(LocalDateTime.now().minusDays(30));
        paymentRepository.save(payment1);

        BigDecimal sum = paymentService.countPaymentsSumForDatePeriod(LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));

        assertNotNull(sum);
        assertEquals(responseDto2.getPaymentAmount(), sum);
    }

    @Test
    void testKafkaConsumerReceivesEvent_andSavesToDatabase() {
        configureWireMockResponse(SUCCESS_API_RESPONSE);
        kafkaRequestTemplate.send("orders", paymentRequestDto);

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(kafkaConsumerService, atLeastOnce())
                            .listenCreateOrderEvents(argThat(paymentRequestDto1 ->
                                    paymentRequestDto1.getUserId().equals(paymentRequestDto.getUserId())));
                    verify(paymentService, atLeastOnce())
                            .createPayment(argThat(paymentRequestDto1 ->
                                    paymentRequestDto1.getUserId().equals(paymentRequestDto.getUserId())));

                    List<Payment> payments = paymentRepository.findAll();
                    assertEquals(1, payments.size());
                });

    }

    @Test
    void testKafkaProducerSendsEvent() {
        configureWireMockResponse(SUCCESS_API_RESPONSE);
        paymentService.createPayment(paymentRequestDto);

        verify(kafkaTemplate, times(1)).send(eq("payments"), any(PaymentResponseDto.class));
    }
}
