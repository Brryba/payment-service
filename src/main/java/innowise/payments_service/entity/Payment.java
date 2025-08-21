package innowise.payments_service.entity;

import lombok.Builder;
import lombok.Data;
import org.bson.BsonTimestamp;
import org.bson.types.Decimal128;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "payments")
@Data
@Builder
public class Payment {
    @Id
    private String id;

    @Field(name = "user_id")
    private Long userId;

    @Field(name = "order_id")
    private Long orderId;

    @Field(name = "payment_amount")
    private Decimal128 paymentAmount;

    @Field(name = "status")
    private Status status;

    @Field(name = "timestamp")
    private BsonTimestamp timestamp;
}
