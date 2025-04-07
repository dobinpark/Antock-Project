package antock.Antock_Project.domain.antocker.service;

import antock.Antock_Project.domain.antocker.entity.Antocker;
import antock.Antock_Project.external.Antocker.AntockerApiClient;
// import antock.Antock_Project.external.Antocker.dto.AntockerDetailResponse; // 이전 경로 주석 처리
import antock.Antock_Project.external.Antocker.AntockerDetailResponse; // 올바른 경로로 수정
import antock.Antock_Project.external.address.AddressApiClient;
// import antock.Antock_Project.external.address.dto.AddressResponse; // 이전 경로 주석 처리
import antock.Antock_Project.external.address.AddressResponse; // 올바른 경로로 수정
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class) // JUnit 5에서 Mockito 사용 설정
class AntockerDataProcessorTest {

    @InjectMocks // 테스트 대상 클래스. Mock 객체들이 자동으로 주입됨.
    private AntockerDataProcessor antockerDataProcessor;

    @Mock // Mock 객체 생성
    private AntockerApiClient antockerApiClient;

    @Mock // Mock 객체 생성
    private AddressApiClient addressApiClient;

    private Map<String, String> sampleCsvData;
    private AntockerDetailResponse sampleDetailResponse;
    private AddressResponse sampleAddressResponse;

    @BeforeEach // 각 테스트 메소드 실행 전에 호출됨
    void setUp() {
        // 테스트용 샘플 데이터 설정
        sampleCsvData = new HashMap<>();
        sampleCsvData.put("사업자등록번호", "1234567890");
        sampleCsvData.put("상호명", "테스트상점");
        sampleCsvData.put("사업장소재지", "서울특별시 강남구 테스트로 123");
        // ... 기타 CSV 필드 추가

        sampleDetailResponse = new AntockerDetailResponse();
        sampleDetailResponse.setCorporateRegistrationNumber("111111-1111111"); // 법인번호 설정
        sampleDetailResponse.setTradeRegistrationNumber("1234567890");
        sampleDetailResponse.setCompanyName("테스트상점");

        sampleAddressResponse = new AddressResponse();
        sampleAddressResponse.setAdministrativeCode("1168010100"); // 행정구역 코드 설정
        sampleAddressResponse.setRoadAddress("서울특별시 강남구 테스트로 123");
    }

    @Test
    @DisplayName("정상적인 CSV 데이터와 API 응답으로 Antocker 엔티티 생성 성공")
    void processData_Success() {
        // given - Mock 설정
        when(antockerApiClient.fetchAntockerDetails("1234567890"))
                .thenReturn(Optional.of(sampleDetailResponse));
        when(addressApiClient.fetchAddressInfo("서울특별시 강남구 테스트로 123"))
                .thenReturn(Optional.of(sampleAddressResponse));

        // when - 테스트 대상 메소드 호출
        Optional<Antocker> resultOpt = antockerDataProcessor.processData(sampleCsvData);

        // then - 결과 검증
        assertThat(resultOpt).isPresent(); // 결과가 존재하는지 확인
        Antocker result = resultOpt.get();

        assertThat(result.getBusinessRegistrationNumber()).isEqualTo("1234567890");
        assertThat(result.getCompanyName()).isEqualTo("테스트상점");
        assertThat(result.getAddress()).isEqualTo("서울특별시 강남구 테스트로 123");
        assertThat(result.getCorporateRegistrationNumber()).isEqualTo("111111-1111111"); // Mock 응답 값 확인
        assertThat(result.getAdministrativeCode()).isEqualTo("1168010100"); // Mock 응답 값 확인
        // ... 기타 필드 검증

        // API 클라이언트 메소드가 예상대로 호출되었는지 검증
        verify(antockerApiClient).fetchAntockerDetails("1234567890");
        verify(addressApiClient).fetchAddressInfo("서울특별시 강남구 테스트로 123");
    }

    @Test
    @DisplayName("사업자등록번호 누락 시 Optional.empty() 반환")
    void processData_MissingBusinessNumber() {
        // given
        sampleCsvData.remove("사업자등록번호"); // 사업자등록번호 제거

        // when
        Optional<Antocker> resultOpt = antockerDataProcessor.processData(sampleCsvData);

        // then
        assertThat(resultOpt).isNotPresent(); // 결과가 없는지 확인

        // API 클라이언트가 호출되지 않았는지 검증
        verify(antockerApiClient, never()).fetchAntockerDetails(anyString());
        verify(addressApiClient, never()).fetchAddressInfo(anyString());
    }

    @Test
    @DisplayName("주소 정보 누락 시 주소 API 호출 없이 처리")
    void processData_MissingAddress() {
        // given
        sampleCsvData.remove("사업장소재지"); // 주소 제거
        when(antockerApiClient.fetchAntockerDetails("1234567890"))
                .thenReturn(Optional.of(sampleDetailResponse)); // 상세 정보 API는 정상 응답 가정

        // when
        Optional<Antocker> resultOpt = antockerDataProcessor.processData(sampleCsvData);

        // then
        assertThat(resultOpt).isPresent();
        Antocker result = resultOpt.get();

        assertThat(result.getAddress()).isNull(); // 주소 필드 null 확인
        assertThat(result.getAdministrativeCode()).isNull(); // 행정구역 코드 null 확인
        assertThat(result.getCorporateRegistrationNumber()).isEqualTo("111111-1111111"); // 다른 API 결과는 정상 반영 확인

        // 주소 API는 호출되지 않았는지 검증
        verify(addressApiClient, never()).fetchAddressInfo(anyString());
        // 상세 정보 API는 호출되었는지 검증
        verify(antockerApiClient).fetchAntockerDetails("1234567890");
    }

    @Test
    @DisplayName("외부 API 응답이 없을 경우 해당 필드는 null로 처리")
    void processData_ApiReturnsEmpty() {
        // given - API 클라이언트가 빈 Optional 반환하도록 설정
        when(antockerApiClient.fetchAntockerDetails("1234567890"))
                .thenReturn(Optional.empty());
        when(addressApiClient.fetchAddressInfo("서울특별시 강남구 테스트로 123"))
                .thenReturn(Optional.empty());

        // when
        Optional<Antocker> resultOpt = antockerDataProcessor.processData(sampleCsvData);

        // then
        assertThat(resultOpt).isPresent();
        Antocker result = resultOpt.get();

        assertThat(result.getBusinessRegistrationNumber()).isEqualTo("1234567890"); // CSV 데이터는 정상 반영
        assertThat(result.getCompanyName()).isEqualTo("테스트상점");
        assertThat(result.getAddress()).isEqualTo("서울특별시 강남구 테스트로 123");
        assertThat(result.getCorporateRegistrationNumber()).isNull(); // API 결과 없으므로 null
        assertThat(result.getAdministrativeCode()).isNull(); // API 결과 없으므로 null

        verify(antockerApiClient).fetchAntockerDetails("1234567890");
        verify(addressApiClient).fetchAddressInfo("서울특별시 강남구 테스트로 123");
    }
}
