package innowise.payments_service.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import innowise.payments_service.controller.PaymentController;
import innowise.payments_service.dto.payment.PaymentPageResponseDto;
import innowise.payments_service.dto.payment.PaymentResponseDto;
import innowise.payments_service.exception.payment.PaymentAccessDeniedException;
import innowise.payments_service.exception.payment.PaymentNotFoundException;
import innowise.payments_service.service.PaymentService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
public class PaymentControllerTest {
    @MockitoBean
    PaymentService paymentService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    private PaymentResponseDto paymentResponseDto;
    private PaymentPageResponseDto paymentPageResponseDto;

    private static final String USER_ID_HEADER = "X-User-Id";
    private final int PAGE = 0;
    private final int PAGE_SIZE = 10;
    private final long USER_ID = 1L;
    private final long ORDER_ID = 1L;

    @BeforeAll
    static void setUp() {
        objectMapper.findAndRegisterModules();
    }

    @BeforeEach
    void setUpStubs() throws IOException {
        paymentResponseDto = objectMapper.readValue(
                new ClassPathResource("json/payment-response.json").getFile(),
                PaymentResponseDto.class
        );

        paymentPageResponseDto = objectMapper.readValue(
                new ClassPathResource("json/payment-page-response.json").getFile(),
                PaymentPageResponseDto.class
        );
    }

    @Test
    void testGetAllUserPayments_success_200() throws Exception {
        when(paymentService.getPaymentsByUserId(USER_ID, PAGE, PAGE_SIZE)).thenReturn(paymentPageResponseDto);

        mockMvc.perform(get("/api/payment?page=" + PAGE + "&size=" + PAGE_SIZE)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[0].id").value(paymentResponseDto.getId()))
                .andExpect(jsonPath("$.payments[0].paymentAmount").value(paymentResponseDto.getPaymentAmount()));

        verify(paymentService).getPaymentsByUserId(USER_ID, PAGE, PAGE_SIZE);
    }

    @Test
    void testGetAllUserPayments_whenNoPageAndSizeSet_usesDefaultValues_success_200() throws Exception {
        when(paymentService.getPaymentsByUserId(eq(USER_ID), any(Integer.class), any(Integer.class))).thenReturn(paymentPageResponseDto);

        mockMvc.perform(get("/api/payment?page=" + PAGE + "&size=" + PAGE_SIZE)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk());

        verify(paymentService).getPaymentsByUserId(eq(USER_ID), any(Integer.class), any(Integer.class));
    }

    @Test
    void testGetAllUserPayments_whenNoPaymentsFound_returns404() throws Exception {
        paymentPageResponseDto.setPayments(null);
        when(paymentService.getPaymentsByUserId(USER_ID, PAGE, PAGE_SIZE)).thenThrow(new PaymentNotFoundException(""));

        mockMvc.perform(get("/api/payment")
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(paymentService).getPaymentsByUserId(eq(USER_ID), any(Integer.class), any(Integer.class));
    }

    @Test
    void testGetAllPaymentsByOrderId_whenPaymentsExist_returns200() throws Exception {
        when(paymentService.getPaymentsByOrderId(USER_ID, ORDER_ID)).thenReturn(List.of(paymentResponseDto));

        mockMvc.perform(get("/api/payment?orderId=" + ORDER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(paymentResponseDto.getId()))
                .andExpect(jsonPath("$.[0].paymentAmount").value(paymentResponseDto.getPaymentAmount()));
    }

    @Test
    void testGetAllPaymentsByOrderId_whenUserAccessIsNotAllowed_returns403() throws Exception {
        when(paymentService.getPaymentsByOrderId(USER_ID, ORDER_ID)).thenThrow(new PaymentAccessDeniedException(""));

        mockMvc.perform(get("/api/payment?orderId=" + ORDER_ID)
                        .header(USER_ID_HEADER, USER_ID))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllPaymentsByOrderId_whenUserIdHeaderIsNotSet_returns400() throws Exception {
        when(paymentService.getPaymentsByOrderId(USER_ID, ORDER_ID)).thenThrow(new PaymentAccessDeniedException(""));

        mockMvc.perform(get("/api/payment?orderId=" + ORDER_ID))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
