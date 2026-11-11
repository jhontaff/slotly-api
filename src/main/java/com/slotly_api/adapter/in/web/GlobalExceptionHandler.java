package com.slotly_api.adapter.in.web;

import com.slotly_api.domain.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(StartNotInFutureException.class)
    ProblemDetail handleStartNotInFuture(StartNotInFutureException ex) {
        return badRequest(ex, "START_NOT_IN_FUTURE");
    }

    @ExceptionHandler(InvalidTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidTransitionException ex) {
        return conflict(ex, "INVALID_TRANSITION");
    }

    @ExceptionHandler(BookingAlreadyStartedException.class)
    ProblemDetail handleBookingAlreadyStarted(BookingAlreadyStartedException ex) {
        return conflict(ex, "BOOKING_ALREADY_STARTED");
    }

    @ExceptionHandler(BookingNotStartedException.class)
    ProblemDetail handleBookingNotStarted(BookingNotStartedException ex) {
        return badRequest(ex, "BOOKING_NOT_STARTED");
    }

    @ExceptionHandler(SlotUnavailableException.class)
    ProblemDetail handleSlotUnavailable(SlotUnavailableException ex) {
        return conflict(ex, "SLOT_UNAVAILABLE");
    }

    @ExceptionHandler(ProviderNotFoundException.class)
    ProblemDetail handleProviderNotFound(ProviderNotFoundException ex) {
        return badRequest(ex, "PROVIDER_NOT_FOUND");
    }

    @ExceptionHandler(BookingNotFoundException.class)
    ProblemDetail handleBookingNotFound(BookingNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex, "BOOKING_NOT_FOUND");
    }

    private ProblemDetail conflict(RuntimeException ex, String code) {
        return problem(HttpStatus.CONFLICT, ex, code);
    }

    private ProblemDetail badRequest(RuntimeException ex, String code) {
        return problem(HttpStatus.BAD_REQUEST, ex, code);
    }

    private ProblemDetail problem(HttpStatus status, RuntimeException ex, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setProperty("code", code);
        return problem;
    }

}

