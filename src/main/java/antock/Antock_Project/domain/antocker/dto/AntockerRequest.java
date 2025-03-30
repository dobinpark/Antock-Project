package antock.Antock_Project.domain.antocker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AntockerRequest {

    @NotBlank(message = "시/도 (city)는 필수 입력값입니다.")
    private String city;

    @NotBlank(message = "구/군 (district)는 필수 입력값입니다.")
    private String district;

}
