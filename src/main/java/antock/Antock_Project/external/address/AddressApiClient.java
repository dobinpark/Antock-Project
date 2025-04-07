package antock.Antock_Project.external.address;

import java.util.Optional;
import antock.Antock_Project.external.address.dto.AddressResponse;

/**
 * 외부 주소 정보 API 클라이언트 인터페이스
 */
public interface AddressApiClient {

    /**
     * 주소를 이용하여 관련 정보 (예: 행정구역 코드)를 조회합니다.
     *
     * @param address 조회할 주소 문자열
     * @return 조회된 주소 정보 (Optional)
     */
    Optional<AddressResponse> fetchAddressInfo(String address);
}
