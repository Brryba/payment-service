package innowise.payments_service.controller;

import innowise.payments_service.dto.error.ErrorDto;
import innowise.payments_service.exception.StatusCodeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class ExceptionControllerAdvice {
    @ExceptionHandler(StatusCodeException.class)
    public ResponseEntity<ErrorDto> handleHttpStatusCodeException(StatusCodeException ex, HttpServletRequest request) {
        ErrorDto errorDto = ErrorDto.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error(ex.getStatusCode().value() + " " + ex.getStatusCode().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .requestType(request.getMethod())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.valueOf(ex.getStatusCode().value()));
    }
}
