package innowise.payments_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PaymentPageResponseDto {
    private int currentPage;
    private int pageSize;
    private int lastPage;
    private List<PaymentResponseDto> payments;
}
