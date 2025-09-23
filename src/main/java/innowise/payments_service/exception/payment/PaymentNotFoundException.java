package innowise.payments_service.exception.payment;

import innowise.payments_service.exception.StatusCodeException;
import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends StatusCodeException {
    public PaymentNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}