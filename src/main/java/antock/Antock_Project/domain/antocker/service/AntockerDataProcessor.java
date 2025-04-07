package antock.Antock_Project.domain.antocker.service;

import antock.Antock_Project.domain.antocker.entity.Antocker;
import antock.Antock_Project.external.Antocker.AntockerApiClient;
import antock.Antock_Project.external.Antocker.dto.AntockerDetailResponse;
import antock.Antock_Project.external.address.AddressApiClient;
import antock.Antock_Project.external.address.dto.AddressResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

@Slf4j
@Service
public class AntockerDataProcessor {

    private final AntockerApiClient antockerApiClient;
    private final AddressApiClient addressApiClient;

    public AntockerDataProcessor(AntockerApiClient antockerApiClient, AddressApiClient addressApiClient) {
        this.antockerApiClient = antockerApiClient;
        this.addressApiClient = addressApiClient;
    }

    @Async("antockerDataProcessorExecutor") // 사용할 Executor 빈 이름 지정
    public CompletableFuture<Antocker> processAntockerData(Map<String, String> csvRow) {
        String bizRegNum = csvRow.get("사업자등록번호"); // "사업자등록번호" 컬럼명으로 변경
        String companyName = csvRow.get("상호"); // "상호" 컬럼명으로 변경
        String address = csvRow.get("사업장주소"); // "사업장주소" 컬럼명으로 변경

        log.info("데이터 처리 시작 (Thread: {}): 상호 = {}", Thread.currentThread().getName(), companyName); // 스레드 이름 로깅

        // 1. 통신판매사업자 등록상세 API 호출 (법인등록번호 조회)
        AntockerDetailResponse antockerDetailResponse = antockerApiClient.getAntockerDetail(bizRegNum);
        String crpNo = null;
        if (antockerDetailResponse != null && antockerDetailResponse.getResponse() != null
                && antockerDetailResponse.getResponse().getBody() != null
                && antockerDetailResponse.getResponse().getBody().getItems() != null
                && antockerDetailResponse.getResponse().getBody().getItems().getItem() != null
                && antockerDetailResponse.getResponse().getBody().getItems().getItem().length > 0) {
            crpNo = antockerDetailResponse.getResponse().getBody().getItems().getItem()[0].getCrpno();
            log.info("통신판매사업자 등록상세 API 응답 (Thread: {}): 법인등록번호 = {}", Thread.currentThread().getName(), crpNo); // 스레드
                                                                                                                // 이름 로깅
        } else {
            log.warn("통신판매사업자 등록상세 API 응답 오류 또는 데이터 없음 (Thread: {})", Thread.currentThread().getName()); // 스레드 이름 로깅
        }

        // 2. 공공주소 API 호출 (행정구역코드 조회)
        AddressResponse addressResponse = addressApiClient.getAddressInfo(address);
        String admCd = null;
        if (addressResponse != null && addressResponse.getResults() != null
                && addressResponse.getResults().getJuso() != null
                && addressResponse.getResults().getJuso().length > 0) {
            admCd = addressResponse.getResults().getJuso()[0].getAdmCd();
            log.info("공공주소 API 응답 (Thread: {}): 행정구역코드 = {}", Thread.currentThread().getName(), admCd); // 스레드 이름 로깅
        } else {
            log.warn("공공주소 API 응답 오류 또는 데이터 없음 (Thread: {})", Thread.currentThread().getName()); // 스레드 이름 로깅
        }

        // 3. Antocker 엔티티 생성 및 데이터 매핑
        Antocker antocker = Antocker.builder()
                .companyName(companyName)
                .bizRegNum(bizRegNum)
                .crpRegNum(crpNo)
                .address(address)
                .admCd(admCd)
                .build();

        log.info("데이터 처리 완료 (Thread: {}): 상호 = {}", Thread.currentThread().getName(), companyName); // 스레드 이름 로깅
        return CompletableFuture.completedFuture(antocker); // CompletableFuture 반환
    }

    public Optional<Antocker> processData(Map<String, String> csvData) {
        // Implementation of processData method
        return Optional.empty(); // Placeholder return, actual implementation needed
    }
}
