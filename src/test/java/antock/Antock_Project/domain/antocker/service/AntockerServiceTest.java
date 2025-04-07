package antock.Antock_Project.domain.antocker.service;

import antock.Antock_Project.common.exception.BusinessException;
import antock.Antock_Project.common.exception.ErrorCode;
import antock.Antock_Project.domain.antocker.entity.Antocker;
import antock.Antock_Project.domain.antocker.repository.AntockerRepository;
import antock.Antock_Project.external.csv.CsvDownloader;
import antock.Antock_Project.external.csv.CsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AntockerServiceTest {

    @InjectMocks
    private AntockerService antockerService;

    @Mock
    private CsvDownloader ftcCsvDownloader;
    @Mock
    private CsvParser openCsvParser;
    @Mock
    private AntockerDataProcessor antockerDataProcessor;
    @Mock
    private AntockerRepository antockerRepository;

    @TempDir
    Path tempDir; // 임시 파일 경로 생성용

    private final String testCondition = "서울,강남구";
    private Path sampleCsvPath;
    private List<Map<String, String>> sampleParsedData;
    private Antocker sampleAntocker1;
    private Antocker sampleAntocker2;
    private Antocker sampleAntocker3Duplicate1;

    @BeforeEach
    void setUp() throws IOException {
        // 테스트용 임시 파일 생성
        sampleCsvPath = Files.createFile(tempDir.resolve("sample.csv"));

        // 샘플 파싱 데이터 (111 중복 포함)
        sampleParsedData = List.of(
                Map.of("사업자등록번호", "111", "상호명", "A"),
                Map.of("사업자등록번호", "222", "상호명", "B"),
                Map.of("사업자등록번호", "111", "상호명", "A 중복")
        );

        // 샘플 처리된 데이터
        sampleAntocker1 = Antocker.builder().id(1L).businessRegistrationNumber("111").companyName("A").build();
        sampleAntocker2 = Antocker.builder().id(2L).businessRegistrationNumber("222").companyName("B").build();
        sampleAntocker3Duplicate1 = Antocker.builder().businessRegistrationNumber("111").companyName("A 중복").build();

        // @BeforeEach에서는 공통적으로 필요한 최소한의 Mock만 설정하거나 아예 설정하지 않음
        // 각 테스트 메소드에서 필요한 Mock을 명시적으로 설정
    }

    @Test
    @DisplayName("정상 처리: CSV 다운로드, 파싱, 처리, 내부 중복 제거, DB 중복 제거 후 저장")
    void processAndSaveAntockerData_Success() throws ExecutionException, InterruptedException {
        // given
        // --- 필요한 Mock 설정 --- 
        when(ftcCsvDownloader.downloadCsvFile(testCondition)).thenReturn(sampleCsvPath);
        when(openCsvParser.parseCsvFile(sampleCsvPath)).thenReturn(sampleParsedData);

        // processAntockerData Mock 설정 (CompletableFuture 반환)
        when(antockerDataProcessor.processAntockerData(sampleParsedData.get(0)))
                .thenReturn(CompletableFuture.completedFuture(sampleAntocker1));
        when(antockerDataProcessor.processAntockerData(sampleParsedData.get(1)))
                .thenReturn(CompletableFuture.completedFuture(sampleAntocker2));
        when(antockerDataProcessor.processAntockerData(sampleParsedData.get(2)))
                .thenReturn(CompletableFuture.completedFuture(sampleAntocker3Duplicate1)); // 중복 데이터 처리 결과

        // 중복 제거 로직 Mock 설정
        // getExistingBusinessNumbers Mock 설정: "222"는 이미 DB에 존재한다고 가정
        // 내부 중복 제거 후 unique 번호는 "111", "222"
        when(antockerRepository.findByBusinessRegistrationNumberIn(List.of("111", "222")))
                .thenReturn(List.of(sampleAntocker2)); // "222"만 DB에 존재

        // saveAll Mock 설정
        when(antockerRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Antocker> antockersToSave = invocation.getArgument(0);
            // ID 할당 시뮬레이션 (실제 DB처럼)
            long nextId = 100L; // 예시 시작 ID
            for(Antocker a : antockersToSave) {
                if (a.getId() == null) {
                    a.setId(nextId++);
                }
            }
            return antockersToSave; // ID 할당된 리스트 반환
        });
        // --- Mock 설정 끝 --- 

        // when
        CompletableFuture<Integer> resultFuture = antockerService.processAndSaveAntockerData(testCondition);
        int savedCount = resultFuture.get(); // 결과 대기 및 가져오기

        // then
        assertEquals(1, savedCount); // 최종적으로 1개만 저장되어야 함 (내부 중복 제거 후 "111", DB 중복 제거 후 "111"만 저장)

        verify(ftcCsvDownloader).downloadCsvFile(testCondition);
        verify(openCsvParser).parseCsvFile(sampleCsvPath);
        // processAntockerData 호출 검증
        verify(antockerDataProcessor, times(3)).processAntockerData(any(Map.class));
        // 중복 검사를 위해 findByBusinessRegistrationNumberIn 호출 검증
        verify(antockerRepository).findByBusinessRegistrationNumberIn(anyList());
        // 최종 저장 로직(saveAll) 호출 검증 (1개 데이터 저장)
        verify(antockerRepository).saveAll(argThat((List<Antocker> list) ->
                list.size() == 1 && list.get(0).getBusinessRegistrationNumber().equals("111")
        ));
    }

    @Test
    @DisplayName("CSV 다운로드 실패 시 처리")
    void processAndSaveAntockerData_DownloadFails() throws ExecutionException, InterruptedException {
        // given
        // --- 필요한 Mock 설정 --- 
        when(ftcCsvDownloader.downloadCsvFile(testCondition)).thenReturn(null); // 다운로드 실패
        // --- Mock 설정 끝 --- 

        // when
        CompletableFuture<Integer> resultFuture = antockerService.processAndSaveAntockerData(testCondition);
        int savedCount = resultFuture.get();

        // then
        assertEquals(0, savedCount);
        // 다운로드 실패 시 이후 단계는 호출되지 않음
        verify(openCsvParser, never()).parseCsvFile(any());
        verify(antockerDataProcessor, never()).processAntockerData(any());
        verify(antockerRepository, never()).saveAll(anyList());
        verify(antockerRepository, never()).findByBusinessRegistrationNumberIn(anyList()); // 중복 체크도 안함
    }

    @Test
    @DisplayName("CSV 파싱 결과 없을 시 처리")
    void processAndSaveAntockerData_ParsingReturnsEmpty() throws ExecutionException, InterruptedException {
        // given
        // --- 필요한 Mock 설정 --- 
        when(ftcCsvDownloader.downloadCsvFile(testCondition)).thenReturn(sampleCsvPath);
        when(openCsvParser.parseCsvFile(sampleCsvPath)).thenReturn(List.of()); // 파싱 결과 없음
        // --- Mock 설정 끝 --- 

        // when
        CompletableFuture<Integer> resultFuture = antockerService.processAndSaveAntockerData(testCondition);
        int savedCount = resultFuture.get();

        // then
        assertEquals(0, savedCount);
        // 파싱 결과 없으면 이후 단계 호출 안됨
        verify(antockerDataProcessor, never()).processAntockerData(any());
        verify(antockerRepository, never()).saveAll(anyList());
        verify(antockerRepository, never()).findByBusinessRegistrationNumberIn(anyList());
    }

    @Test
    @DisplayName("데이터 처리 중 예외 발생 시 처리")
    void processAndSaveAntockerData_ProcessingThrowsException() {
        // given
        // --- 필요한 Mock 설정 --- 
        when(ftcCsvDownloader.downloadCsvFile(testCondition)).thenReturn(sampleCsvPath);
        when(openCsvParser.parseCsvFile(sampleCsvPath)).thenReturn(sampleParsedData);
        // 데이터 처리 시 예외 발생하도록 Mock 설정 (첫 번째 데이터 처리 시 발생 가정)
        when(antockerDataProcessor.processAntockerData(sampleParsedData.get(0)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing error")));
        // 다른 데이터 처리에 대한 Mock (호출될 수도, 안 될 수도 있지만 설정은 해둠)
        when(antockerDataProcessor.processAntockerData(sampleParsedData.get(1)))
                .thenReturn(CompletableFuture.completedFuture(sampleAntocker2));
        when(antockerDataProcessor.processAntockerData(sampleParsedData.get(2)))
                 .thenReturn(CompletableFuture.completedFuture(sampleAntocker3Duplicate1));
        // --- Mock 설정 끝 --- 

        // when & then
        CompletableFuture<Integer> resultFuture = antockerService.processAndSaveAntockerData(testCondition);

        // CompletableFuture가 예외로 완료되었는지 확인
        // allOf().handle(...) 내부에서 CompletionException으로 감싸서 던지므로 확인
        assertThatThrownBy(resultFuture::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(CompletionException.class)
                .hasRootCauseInstanceOf(RuntimeException.class) // 원인 예외 확인
                .hasMessageContaining("Processing error");

        // 예외가 발생했으므로 저장은 호출되지 않아야 함
        verify(antockerRepository, never()).saveAll(anyList());
        verify(antockerRepository, never()).findByBusinessRegistrationNumberIn(anyList());
    }

    @Test
    @DisplayName("저장할 고유 데이터 없을 시 처리")
    void processAndSaveAntockerData_NoUniqueDataToSave() throws ExecutionException, InterruptedException {
        // given
        // --- 필요한 Mock 설정 --- 
        List<Map<String, String>> singleExistingData = List.of(Map.of("사업자등록번호", "111")); // 데이터 1개
        when(ftcCsvDownloader.downloadCsvFile(testCondition)).thenReturn(sampleCsvPath);
        when(openCsvParser.parseCsvFile(sampleCsvPath)).thenReturn(singleExistingData);
        when(antockerDataProcessor.processAntockerData(any(Map.class)))
                .thenReturn(CompletableFuture.completedFuture(sampleAntocker1));
        // 모든 데이터가 이미 존재한다고 가정 (DB 중복 체크)
        when(antockerRepository.findByBusinessRegistrationNumberIn(List.of("111")))
                .thenReturn(List.of(sampleAntocker1));
        // --- Mock 설정 끝 --- 

        // when
        CompletableFuture<Integer> resultFuture = antockerService.processAndSaveAntockerData(testCondition);
        int savedCount = resultFuture.get();

        // then
        assertEquals(0, savedCount);
        verify(antockerRepository, never()).saveAll(anyList()); // 저장 호출 안됨
        verify(antockerRepository).findByBusinessRegistrationNumberIn(List.of("111")); // 중복 체크는 호출됨
    }
}
