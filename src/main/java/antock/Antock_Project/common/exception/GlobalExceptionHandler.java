package antock.Antock_Project.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        log.warn("Validation Exception: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getBindingResult().getAllErrors().get(0).getDefaultMessage(), // 첫 번째 에러 메시지만 반환
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business Exception: {} (Code: {})", ex.getMessage(), ex.getErrorCode()); // 에러 코드도 로깅
        ErrorCode errorCode = ex.getErrorCode(); // ErrorCode 가져오기
        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getStatus().value(), // ErrorCode에서 status 가져오기
                errorCode.getStatus().getReasonPhrase(), // ErrorCode에서 status 가져오기
                ex.getDetailMessage(), // 상세 메시지 사용
                request.getRequestURI());
        return ResponseEntity.status(errorCode.getStatus()).body(errorResponse); // ErrorCode에서 status 사용
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected Exception: {}", ex.getMessage(), ex); // 스택 트레이스 로깅
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Unexpected error occurred",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
