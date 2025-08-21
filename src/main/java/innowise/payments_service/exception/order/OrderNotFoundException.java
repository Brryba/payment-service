package innowise.payments_service.exception.order;

import innowise.payments_service.exception.StatusCodeException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends StatusCodeException {
    public OrderNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
