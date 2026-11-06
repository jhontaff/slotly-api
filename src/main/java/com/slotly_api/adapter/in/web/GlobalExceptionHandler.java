package com.slotly_api.adapter.in.web;

import com.slotly_api.domain.exceptions.BookingAlreadyStartedException;
import com.slotly_api.domain.exceptions.BookingNotStartedException;
import com.slotly_api.domain.exceptions.InvalidTransitionException;
import com.slotly_api.domain.exceptions.StartNotInFutureException;
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

    private ProblemDetail conflict(RuntimeException ex, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("code", code);
        return problem;
    }

    private ProblemDetail badRequest(RuntimeException ex, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("code", code);
        return problem;
    }


}

