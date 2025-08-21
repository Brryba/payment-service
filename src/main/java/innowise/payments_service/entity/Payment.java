package innowise.payments_service.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Data
public class Payment {
    @Id
    private String id;

    @Field(name = "user_id")
    private Long userId;

    @Field(name = "order_id")
    private Long orderId;

    @Field(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Field(name = "status")
    private Status status;

    @Field(name = "timestamp")
    private LocalDateTime timestamp;
}
