package antock.Antock_Project.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailMessage; // 추가적인 상세 메시지

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = errorCode.getMessage();
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage); // 상세 메시지를 예외 메시지로 사용
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    // 필요에 따라 원인(cause)을 받는 생성자 등을 추가할 수 있습니다.
}
