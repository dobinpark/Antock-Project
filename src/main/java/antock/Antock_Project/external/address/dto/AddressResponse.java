package antock.Antock_Project.external.address.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AddressResponse {
    private Results results;

    @Data
    public static class Results {
        private Common common;
        private Juso[] juso;
    }

    @Data
    public static class Common {
        private String errorCode;
        private String errorMessage;
    }

    @Data
    public static class Juso {
        @JsonProperty("admCd")
        private String admCd; // 행정구역코드
    }
}
