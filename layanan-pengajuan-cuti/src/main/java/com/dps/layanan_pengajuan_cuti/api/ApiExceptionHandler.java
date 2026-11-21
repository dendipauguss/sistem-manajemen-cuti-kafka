package com.dps.layanan_pengajuan_cuti.api;

import com.dps.layanan_pengajuan_cuti.service.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(EntityNotFoundException.class)
	ProblemDetail notFound(EntityNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(MessagingException.class)
	ProblemDetail messagingFailure(MessagingException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	ProblemDetail conflict(IllegalStateException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail badRequest(IllegalArgumentException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}
}
