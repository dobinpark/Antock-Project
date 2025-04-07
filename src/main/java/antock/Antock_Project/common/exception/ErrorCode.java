package antock.Antock_Project.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", " 유효하지 않은 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", " 지원하지 않는 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", " 해당 엔티티를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", " 유효하지 않은 타입 값입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", " 접근 권한이 없습니다."),

    // CSV Related
    CSV_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CSV001", "CSV 파일 다운로드에 실패했습니다."),
    CSV_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CSV002", "CSV 파일 파싱에 실패했습니다."),
    DOWNLOAD_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "CSV003", "파일 다운로드 시간 초과"),
    ELEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CSV004", "웹 페이지 요소를 찾을 수 없습니다."),

    // API Related
    API_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "API001", "외부 API 요청에 실패했습니다."),
    API_RESPONSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "API002", "외부 API 응답 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}