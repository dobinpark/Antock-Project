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

        // 1. 통신판매사업자 등록상세 API 호출 (인터페이스 메소드명 사용 및 Optional 처리)
        Optional<AntockerDetailResponse> detailOptional = antockerApiClient.fetchAntockerDetails(bizRegNum);
        String crpNo = detailOptional
                .map(AntockerDetailResponse::getResponse) // null 체크 포함
                .map(AntockerDetailResponse.Response::getBody)
                .map(AntockerDetailResponse.Body::getItems)
                .map(AntockerDetailResponse.Items::getItem)
                .filter(items -> items.length > 0) // 배열이 비어있지 않은지 확인
                .map(items -> items[0].getCrpno()) // 첫 번째 item의 crpno 가져오기
                .orElse(null); // 값이 없으면 null

        if (crpNo != null) {
            log.info("통신판매사업자 등록상세 API 응답 (Thread: {}): 법인등록번호 = {}", Thread.currentThread().getName(), crpNo); // 스레드
                                                                                                                // 이름 로깅
        } else {
            log.warn("통신판매사업자 등록상세 API 응답 오류 또는 데이터 없음 (Thread: {})", Thread.currentThread().getName()); // 스레드 이름 로깅
        }

        // 2. 공공주소 API 호출 (인터페이스 메소드명 사용 및 Optional 처리)
        Optional<AddressResponse> addressOptional = addressApiClient.fetchAddressInfo(address);
        String admCd = addressOptional
                .map(AddressResponse::getResults) // Results 객체 가져오기
                .map(AddressResponse.Results::getJuso) // Juso 배열 가져오기
                .filter(jusoArray -> jusoArray != null && jusoArray.length > 0) // 배열 null 및 비어있는지 확인
                .map(jusoArray -> jusoArray[0].getAdmCd()) // 첫 번째 Juso 객체의 admCd 가져오기
                .orElse(null); // 값이 없으면 null

        if (admCd != null) {
            log.info("공공주소 API 응답 (Thread: {}): 행정구역코드 = {}", Thread.currentThread().getName(), admCd); // 스레드 이름 로깅
        } else {
            log.warn("공공주소 API 응답 오류 또는 데이터 없음 (Thread: {})", Thread.currentThread().getName()); // 스레드 이름 로깅
        }

        // 3. Antocker 엔티티 생성 및 데이터 매핑 (엔티티 필드명과 일치하도록 수정)
        Antocker antocker = Antocker.builder()
                .companyName(companyName)
                .businessRegistrationNumber(bizRegNum) // 필드명 businessRegistrationNumber
                .corporateRegistrationNumber(crpNo) // 필드명 corporateRegistrationNumber
                .address(address)
                .administrativeCode(admCd) // 필드명 administrativeCode
                .build();

        log.info("데이터 처리 완료 (Thread: {}): 상호 = {}", Thread.currentThread().getName(), companyName); // 스레드 이름 로깅
        return CompletableFuture.completedFuture(antocker); // CompletableFuture 반환
    }
}
