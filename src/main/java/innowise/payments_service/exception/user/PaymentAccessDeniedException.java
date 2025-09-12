package innowise.payments_service.exception.user;

import innowise.payments_service.exception.StatusCodeException;
import org.springframework.http.HttpStatus;

public class PaymentAccessDeniedException extends StatusCodeException {
    public PaymentAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
