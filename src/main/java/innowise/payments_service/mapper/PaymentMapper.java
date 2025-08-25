package innowise.payments_service.mapper;

import innowise.payments_service.dto.PaymentRequestDto;
import innowise.payments_service.dto.PaymentResponseDto;
import innowise.payments_service.entity.Payment;
import org.bson.types.Decimal128;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.mapstruct.ReportingPolicy.ERROR;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR)
public interface PaymentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "paymentAmount", source = "paymentAmount")
    Payment toPaymentFromRequest(PaymentRequestDto paymentRequestDto);

    @Mapping(target = "paymentAmount", source = "paymentAmount")
    @Mapping(target = "eventType", constant = "CREATE_PAYMENT")
    PaymentResponseDto toPaymentResponseDto(Payment payment);

    static BigDecimal convertPaymentAmountToBigDecimal(Decimal128 decimal128) {
        return decimal128.bigDecimalValue();
    }

    static Decimal128 convertPaymentAmountToDecimal128(BigDecimal decimal128) {
        return Decimal128.parse(decimal128.toPlainString());
    }
}
