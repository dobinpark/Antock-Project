package antock.Antock_Project.external.Antocker;

import antock.Antock_Project.external.Antocker.dto.AntockerDetailResponse;

public interface AntockerApiClient {
    /**
     * 사업자등록번호를 기반으로 통신판매사업자 상세 정보를 조회합니다.
     * @param bizRegNum 사업자등록번호 (10자리)
     * @return 통신판매사업자 상세 정보 응답
     */
    AntockerDetailResponse getAntockerDetail(String bizRegNum);
}
