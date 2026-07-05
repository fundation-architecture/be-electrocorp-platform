package com.electrocorp.electrocorpplatform.shared.interfaces.rest.errors;

import com.electrocorp.electrocorpplatform.shared.interfaces.rest.resources.ErrorResource;
import com.electrocorp.electrocorpplatform.shared.application.result.ApplicationError;
import com.electrocorp.electrocorpplatform.shared.domain.exceptions.BusinessRuleException;
import com.electrocorp.electrocorpplatform.shared.domain.exceptions.BusinessRuleValidationException;
import com.electrocorp.electrocorpplatform.shared.domain.exceptions.ConflictException;
import com.electrocorp.electrocorpplatform.shared.domain.exceptions.DomainException;
import com.electrocorp.electrocorpplatform.shared.domain.exceptions.InvalidValueObjectException;
import com.electrocorp.electrocorpplatform.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResource handleResourceNotFoundException(ResourceNotFoundException exception) {
        return error(ApplicationError.notFound(exception.getMessage()));
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictException.class)
    public ErrorResource handleConflictException(ConflictException exception) {
        return error(ApplicationError.conflict(exception.getMessage()));
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler({
            BusinessRuleException.class,
            BusinessRuleValidationException.class,
            InvalidValueObjectException.class,
            DomainException.class
    })
    public ErrorResource handleDomainException(RuntimeException exception) {
        return error(ApplicationError.businessRuleViolation(exception.getMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResource handleIllegalArgumentException(IllegalArgumentException exception) {
        return error(ApplicationError.badRequest(exception.getMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResource handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Validation failed");
        return error(new ApplicationError("VALIDATION_ERROR", message));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResource handleGeneralException(Exception exception) {
        return error(ApplicationError.unexpected(exception.getMessage()));
    }

    private ErrorResource error(ApplicationError error) {
        return ErrorResponseAssembler.toResourceFromError(
                new ApplicationError(error.code(), resolveMessage(error.message()))
        );
    }

    private String resolveMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        return messageSource.getMessage(message, null, message, LocaleContextHolder.getLocale());
    }
}
