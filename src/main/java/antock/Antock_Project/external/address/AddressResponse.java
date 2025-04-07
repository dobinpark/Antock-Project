package antock.Antock_Project.external.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AddressResponse {
    // 주소 API 응답 필드와 매핑 (실제 API 응답 스펙에 맞게 수정 필요)

    @JsonProperty("adm_cd") // 행정구역 코드
    private String administrativeCode;

    @JsonProperty("road_addr") // 도로명 주소
    private String roadAddress;

    @JsonProperty("jibun_addr") // 지번 주소
    private String jibunAddress;

    // ... 기타 필요한 필드 추가
}
