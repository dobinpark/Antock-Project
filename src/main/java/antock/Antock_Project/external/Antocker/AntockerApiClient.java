package antock.Antock_Project.external.Antocker;

import java.util.Optional;
import antock.Antock_Project.external.Antocker.dto.AntockerDetailResponse;

/**
 * 외부 통신판매사업자 정보 API 클라이언트 인터페이스
 */
public interface AntockerApiClient {

    /**
     * 사업자등록번호를 이용하여 통신판매사업자 상세 정보를 조회합니다.
     *
     * @param businessRegistrationNumber 조회할 사업자등록번호
     * @return 조회된 사업자 상세 정보 (Optional)
     */
    Optional<AntockerDetailResponse> fetchAntockerDetails(String businessRegistrationNumber);
}
