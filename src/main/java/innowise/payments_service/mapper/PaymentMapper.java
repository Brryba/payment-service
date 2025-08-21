package innowise.payments_service.mapper;

import innowise.payments_service.dto.PaymentResponseDto;
import innowise.payments_service.entity.Payment;
import org.bson.BsonTimestamp;
import org.bson.types.Decimal128;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.mapstruct.ReportingPolicy.ERROR;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR)
public interface PaymentMapper {
    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "convertTimestamp")
    @Mapping(target = "paymentAmount", source = "paymentAmount", qualifiedByName = "convertPaymentAmount")
    PaymentResponseDto toPaymentResponseDto(Payment payment);

    @Named("convertTimestamp")
    static LocalDateTime convertTimestamp(BsonTimestamp bsonTimestamp) {
        Instant instant = Instant.ofEpochSecond(bsonTimestamp.getTime());

        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    @Named("convertPaymentAmount")
    static BigDecimal convertPaymentAmount(Decimal128 decimal128) {
        return decimal128.bigDecimalValue();
    }
}
