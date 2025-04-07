package antock.Antock_Project.domain.antocker.service;

import antock.Antock_Project.domain.antocker.entity.Antocker;
import antock.Antock_Project.external.Antocker.AntockerApiClient;
// import antock.Antock_Project.external.Antocker.AntockerDetailResponse; // 이전 타입 제거
import antock.Antock_Project.external.Antocker.dto.AntockerDetailResponse; // DTO 타입 임포트
import antock.Antock_Project.external.address.AddressApiClient;
// import antock.Antock_Project.external.address.AddressResponse; // 이전 타입 제거
import antock.Antock_Project.external.address.dto.AddressResponse; // DTO 타입 임포트
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AntockerDataProcessorTest {

    @Mock
    private AntockerApiClient antockerApiClient;

    @Mock
    private AddressApiClient addressApiClient;

    @InjectMocks
    private AntockerDataProcessor antockerDataProcessor;

    private Map<String, String> sampleCsvData;
    private AntockerDetailResponse sampleDetailResponse; // DTO 타입으로 변경
    private AddressResponse sampleAddressResponse; // DTO 타입으로 변경

    @BeforeEach
    void setUp() {
        // 샘플 CSV 데이터 설정
        sampleCsvData = new HashMap<>();
        sampleCsvData.put("사업자등록번호", "1234567890");
        sampleCsvData.put("상호", "테스트상점");
        sampleCsvData.put("사업장주소", "서울시 강남구 테스트로 123");

        // 샘플 AntockerDetailResponse (DTO) 설정
        sampleDetailResponse = new AntockerDetailResponse();
        AntockerDetailResponse.Response response = new AntockerDetailResponse.Response();
        AntockerDetailResponse.Body body = new AntockerDetailResponse.Body();
        AntockerDetailResponse.Items items = new AntockerDetailResponse.Items();
        AntockerDetailResponse.Item item = new AntockerDetailResponse.Item();
        item.setCrpno("1111112222222"); // 법인등록번호 설정
        items.setItem(new AntockerDetailResponse.Item[]{item});
        body.setItems(items);
        response.setBody(body);
        sampleDetailResponse.setResponse(response);

        // 샘플 AddressResponse (DTO) 설정
        sampleAddressResponse = new AddressResponse();
        AddressResponse.Results results = new AddressResponse.Results();
        AddressResponse.Juso juso = new AddressResponse.Juso();
        juso.setAdmCd("1168010100"); // 행정구역코드 설정
        results.setJuso(new AddressResponse.Juso[]{juso});
        sampleAddressResponse.setResults(results);
    }

    @Test
    @DisplayName("정상적인 CSV 데이터 처리 테스트")
    void processAntockerData_Success() throws ExecutionException, InterruptedException {
        // given
        // Mock 설정 (DTO 타입 사용)
        when(antockerApiClient.fetchAntockerDetails(anyString())).thenReturn(Optional.of(sampleDetailResponse));
        when(addressApiClient.fetchAddressInfo(anyString())).thenReturn(Optional.of(sampleAddressResponse));

        // when
        // processAntockerData 호출 및 CompletableFuture 결과 확인
        CompletableFuture<Antocker> resultFuture = antockerDataProcessor.processAntockerData(sampleCsvData);
        Antocker resultAntocker = resultFuture.get(); // 결과 가져오기

        // then
        assertNotNull(resultAntocker);
        assertEquals("테스트상점", resultAntocker.getCompanyName());
        assertEquals("1234567890", resultAntocker.getBusinessRegistrationNumber());
        assertEquals("1111112222222", resultAntocker.getCorporateRegistrationNumber());
        assertEquals("서울시 강남구 테스트로 123", resultAntocker.getAddress());
        assertEquals("1168010100", resultAntocker.getAdministrativeCode());
    }

    @Test
    @DisplayName("통신판매사업자 API 응답 없을 때 처리 테스트")
    void processAntockerData_NoAntockerDetail() throws ExecutionException, InterruptedException {
        // given
        when(antockerApiClient.fetchAntockerDetails(anyString())).thenReturn(Optional.empty()); // API 응답 없음
        when(addressApiClient.fetchAddressInfo(anyString())).thenReturn(Optional.of(sampleAddressResponse));

        // when
        CompletableFuture<Antocker> resultFuture = antockerDataProcessor.processAntockerData(sampleCsvData);
        Antocker resultAntocker = resultFuture.get();

        // then
        assertNotNull(resultAntocker);
        assertNull(resultAntocker.getCorporateRegistrationNumber()); // 법인번호는 null
        assertEquals("1168010100", resultAntocker.getAdministrativeCode()); // 주소 코드는 정상
    }

    @Test
    @DisplayName("주소 API 응답 없을 때 처리 테스트")
    void processAntockerData_NoAddressInfo() throws ExecutionException, InterruptedException {
        // given
        when(antockerApiClient.fetchAntockerDetails(anyString())).thenReturn(Optional.of(sampleDetailResponse));
        when(addressApiClient.fetchAddressInfo(anyString())).thenReturn(Optional.empty()); // 주소 API 응답 없음

        // when
        CompletableFuture<Antocker> resultFuture = antockerDataProcessor.processAntockerData(sampleCsvData);
        Antocker resultAntocker = resultFuture.get();

        // then
        assertNotNull(resultAntocker);
        assertEquals("1111112222222", resultAntocker.getCorporateRegistrationNumber()); // 법인번호는 정상
        assertNull(resultAntocker.getAdministrativeCode()); // 주소 코드는 null
    }

    @Test
    @DisplayName("두 API 모두 응답 없을 때 처리 테스트")
    void processAntockerData_NoApiResponse() throws ExecutionException, InterruptedException {
        // given
        when(antockerApiClient.fetchAntockerDetails(anyString())).thenReturn(Optional.empty());
        when(addressApiClient.fetchAddressInfo(anyString())).thenReturn(Optional.empty());

        // when
        CompletableFuture<Antocker> resultFuture = antockerDataProcessor.processAntockerData(sampleCsvData);
        Antocker resultAntocker = resultFuture.get();

        // then
        assertNotNull(resultAntocker);
        assertNull(resultAntocker.getCorporateRegistrationNumber());
        assertNull(resultAntocker.getAdministrativeCode());
    }
}
