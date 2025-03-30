package antock.Antock_Project.external.Antocker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AntockerDetailResponse {

    private Response response;

    @Data
    public static class Response {
        private Body body;
    }

    @Data
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data
    public static class Body {
        private Items items;
    }

    @Data
    public static class Items {
        private Item[] item;
    }

    @Data
    public static class Item {
        @JsonProperty("tospsn_no") // JSON 필드명과 매핑
        private String tospSnNo; // 통신판매번호

        @JsonProperty("entrps_nm")
        private String entrpsNm; // 상호

        @JsonProperty("bizrno")
        private String bizrno; // 사업자등록번호

        @JsonProperty("crpno")
        private String crpno; // 법인등록번호
    }
}
