package innowise.payments_service.repository;

import innowise.payments_service.entity.Payment;
import innowise.payments_service.entity.Status;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findAllByOrderId(Long orderId);

    Page<Payment> findAllByUserId(Long userId, Pageable pageable);

    List<Payment> findAllByStatus(Status status);

    @Aggregation(pipeline = {
            "{ $match: { 'timestamp' : { $gte: ?0, $lte: ?1 }, 'status' : 'COMPLETED' } }",
            "{ $group: { _id: null, sum: { $sum: '$payment_amount' } } }"
    })
    Decimal128 countTotalPaymentAmountInDatePeriod(LocalDateTime startDate, LocalDateTime endDate);
}
