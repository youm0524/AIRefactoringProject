package com.careerpick.controller;

import com.careerpick.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = CoverLetterController.class)
public class CoverLetterExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CoverLetterExceptionHandler.class);
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> status(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String code = switch (status) {
            case 400 -> "INVALID_REQUEST";
            case 401 -> "AUTHENTICATION_REQUIRED";
            case 404 -> exception instanceof com.careerpick.service.CoverLetterNotFoundException ? "COVER_LETTER_NOT_FOUND" : "JOB_NOT_FOUND";
            default -> "INTERNAL_ERROR";
        };
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, status < 500 ? exception.getReason() : "요청을 처리하지 못했습니다."));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("INVALID_REQUEST", "입력값을 확인해 주세요."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception exception) {
        log.error("자기소개서 요청 실패: {}", exception.getClass().getName());
        return ResponseEntity.internalServerError().body(new ApiErrorResponse("INTERNAL_ERROR", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }
}
