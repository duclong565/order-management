package com.example.order_management.exception;

import com.example.order_management.common.BaseResponse;
import com.example.order_management.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<BaseResponse<Void>> handleApplicationException(ApplicationException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), ec.getMessage()));  // KHONG lo ex.getMessage()
    }
}
