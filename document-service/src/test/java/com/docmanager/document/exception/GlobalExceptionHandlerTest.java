package com.docmanager.document.exception;

import com.docmanager.document.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Validation-Fehler -> 400 mit zusammengeführten Feldmeldungen")
    void validationError_returns400() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "filename", "darf nicht leer sein"));
        bindingResult.addError(new FieldError("request", "sizeBytes", "muss positiv sein"));
        var ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(
                        GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0),
                bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message())
                .contains("filename: darf nicht leer sein", "sizeBytes: muss positiv sein");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Messaging-Fehler -> 500 mit generischer Meldung")
    void messagingError_returns500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMessaging(new AmqpConnectException(new RuntimeException("geheim")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message())
                .isEqualTo("Messaging-Fehler: Nachricht konnte nicht gesendet werden");
    }

    @Test
    @DisplayName("eigene MessagingException -> 500 mit generischer Meldung")
    void customMessagingException_returns500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMessaging(
                        new MessagingException("Nachricht konnte nicht gesendet werden",
                                new AmqpConnectException(new RuntimeException("geheim"))));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message())
                .isEqualTo("Messaging-Fehler: Nachricht konnte nicht gesendet werden");
    }

    @Test
    @DisplayName("Unbehandelte Exception -> 500 ohne interne Details")
    void unexpectedError_returns500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("interne Details"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Interner Serverfehler");
    }

    @SuppressWarnings("unused")
    private void dummy(String arg) {
    }
}
