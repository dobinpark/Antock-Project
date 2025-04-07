package antock.Antock_Project.external.Antocker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AntockerDetailResponse {
    // API 응답 필드와 매핑 (실제 API 응답 스펙에 맞게 수정 필요)

    @JsonProperty("corp_reg_num") // 예시: JSON 응답 필드 이름이 다른 경우
    private String corporateRegistrationNumber; // 법인등록번호

    @JsonProperty("trd_reg_num")
    private String tradeRegistrationNumber; // 사업자등록번호 (확인용)

    @JsonProperty("corp_nm")
    private String companyName; // 상호명

    // ... 기타 필요한 필드 추가
}
