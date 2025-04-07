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
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
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

    private Path sampleCsvPath;
    private List<Map<String, String>> sampleParsedData;
    private Antocker sampleAntocker1;
    private Antocker sampleAntocker2;

    @BeforeEach
    void setUp() throws IOException {
        // 테스트용 임시 파일 생성
        sampleCsvPath = Files.createFile(tempDir.resolve("sample.csv"));

        // 샘플 파싱 데이터
        sampleParsedData = List.of(
                Map.of("사업자등록번호", "111", "상호명", "A"),
                Map.of("사업자등록번호", "222", "상호명", "B"),
                Map.of("사업자등록번호", "333", "상호명", "C") // DB에 이미 존재한다고 가정할 데이터
        );

        // 샘플 처리된 데이터
        sampleAntocker1 = Antocker.builder().id(1L).businessRegistrationNumber("111").companyName("A").build();
        sampleAntocker2 = Antocker.builder().id(2L).businessRegistrationNumber("222").companyName("B").build();
        // Antocker 3은 DB에 이미 존재해서 필터링될 것
        Antocker sampleAntocker3Existing = Antocker.builder().id(3L).businessRegistrationNumber("333").companyName("C")
                .build();

        // Mock 설정 기본값 (필요시 각 테스트에서 오버라이드)
        // - DataProcessor는 각 row에 대해 Optional<Antocker> 반환
        when(antockerDataProcessor.processData(sampleParsedData.get(0))).thenReturn(Optional.of(sampleAntocker1));
        when(antockerDataProcessor.processData(sampleParsedData.get(1))).thenReturn(Optional.of(sampleAntocker2));
        when(antockerDataProcessor.processData(sampleParsedData.get(2)))
                .thenReturn(Optional.of(sampleAntocker3Existing)); // 일단 처리 성공

        // - Repository의 findByBusinessRegistrationNumber 설정 (중복 필터링용)
        // findAllById 는 private 메소드 테스트가 어려워 findByBusinessRegistrationNumber 를 활용
        when(antockerRepository.findByBusinessRegistrationNumber("111")).thenReturn(Optional.empty()); // 신규
        when(antockerRepository.findByBusinessRegistrationNumber("222")).thenReturn(Optional.empty()); // 신규
        when(antockerRepository.findByBusinessRegistrationNumber("333"))
                .thenReturn(Optional.of(sampleAntocker3Existing)); // 기존

        // - saveAll은 저장 요청된 리스트를 그대로 반환한다고 가정
        when(antockerRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("정상 처리: 다운로드, 파싱, 처리, 중복제거, 저장 성공")
    void processAndSaveAntockerData_Success() throws ExecutionException, InterruptedException {
        // given
        String condition = "서울특별시";
        when(ftcCsvDownloader.downloadCsvFile(condition)).thenReturn(sampleCsvPath);
        when(openCsvParser.parseCsvFile(sampleCsvPath)).thenReturn(sampleParsedData);
        // setUp에서 설정된 Mock 동작 사용

        // when
        CompletableFuture<Integer> future = antockerService.processAndSaveAntockerData(condition);
        Integer savedCount = future.get(); // 비동기 작업 완료 대기 및 결과 얻기

        // then
        assertThat(savedCount).isEqualTo(2); // 중복(333) 제외 2개 저장 확인

        // 각 컴포넌트 호출 검증
        verify(ftcCsvDownloader).downloadCsvFile(condition);
        verify(openCsvParser).parseCsvFile(sampleCsvPath);
        verify(antockerDataProcessor, times(3)).processData(any(Map.class)); // 3번 호출
        // filterUniqueAntockers 내부 동작 검증 (findByBusinessRegistrationNumber 호출 횟수)
        verify(antockerRepository, times(3)).findByBusinessRegistrationNumber(anyString());
        // saveAll 호출 검증 (argThat 수정)
        verify(antockerRepository).saveAll(argThat(iterable -> {
            if (iterable instanceof List) {
                List<Antocker> list = (List<Antocker>) iterable;
                return list.size() == 2 &&
                       list.stream().anyMatch(a -> a.getBusinessRegistrationNumber().equals("111")) &&
                       list.stream().anyMatch(a -> a.getBusinessRegistrationNumber().equals("222"));
            } else {
                // Iterable이 List가 아닌 경우, 예상치 못한 상황이므로 false 반환 또는 예외 처리
                // 여기서는 간단히 false 반환
                return false;
            }
        }));
        // 파일 삭제 로직 호출 확인 (cleanupDownloadedFile 내부 Files.delete)
        // Files.delete는 static 메소드라 직접 verify하기 어려움.
        // 여기서는 임시 파일이 실제로 삭제되었는지 확인 (테스트 환경에 따라 동작 다를 수 있음)
        // 또는 cleanupDownloadedFile 메소드 자체를 spy하여 호출 여부 확인 가능
        assertThat(Files.exists(sampleCsvPath)).isFalse(); // 파일이 삭제되었는지 확인
    }

    @Test
    @DisplayName("CSV 다운로드 실패 시 BusinessException 발생 및 종료")
    void processAndSaveAntockerData_DownloadFails() {
        // given
        String condition = "서울특별시";
        when(ftcCsvDownloader.downloadCsvFile(condition))
                .thenThrow(new BusinessException(ErrorCode.CSV_DOWNLOAD_FAILED));

        // when
        CompletableFuture<Integer> future = antockerService.processAndSaveAntockerData(condition);

        // then
        // CompletableFuture가 BusinessException으로 실패했는지 확인
        assertThatThrownBy(future::get) // .get() 호출 시 예외 발생
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.CSV_DOWNLOAD_FAILED.getMessage());

        // 다른 컴포넌트 미호출 검증
        verify(openCsvParser, never()).parseCsvFile(any());
        verify(antockerDataProcessor, never()).processData(any());
        verify(antockerRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("CSV 파싱 결과가 비어있을 경우 0 반환 및 종료")
    void processAndSaveAntockerData_ParsingReturnsEmpty() throws ExecutionException, InterruptedException {
        // given
        String condition = "서울특별시";
        when(ftcCsvDownloader.downloadCsvFile(condition)).thenReturn(sampleCsvPath);
        when(openCsvParser.parseCsvFile(sampleCsvPath)).thenReturn(List.of()); // 빈 리스트 반환

        // when
        CompletableFuture<Integer> future = antockerService.processAndSaveAntockerData(condition);
        Integer savedCount = future.get();

        // then
        assertThat(savedCount).isZero(); // 저장된 개수 0 확인

        // 이후 컴포넌트 미호출 검증
        verify(antockerDataProcessor, never()).processData(any());
        verify(antockerRepository, never()).saveAll(anyList());
        // 파일 삭제는 수행되어야 함
        assertThat(Files.exists(sampleCsvPath)).isFalse();
    }

    @Test
    @DisplayName("모든 데이터 처리 실패 시(Optional.empty) 0 반환 및 종료")
    void processAndSaveAntockerData_ProcessingAllFail() throws ExecutionException, InterruptedException {
        // given
        String condition = "서울특별시";
        when(ftcCsvDownloader.downloadCsvFile(condition)).thenReturn(sampleCsvPath);
        when(openCsvParser.parseCsvFile(sampleCsvPath)).thenReturn(sampleParsedData);
        // DataProcessor가 항상 empty 반환하도록 Mock 재설정
        when(antockerDataProcessor.processData(any(Map.class))).thenReturn(Optional.empty());

        // when
        CompletableFuture<Integer> future = antockerService.processAndSaveAntockerData(condition);
        Integer savedCount = future.get();

        // then
        assertThat(savedCount).isZero(); // 저장된 개수 0 확인

        // saveAll 미호출 검증
        verify(antockerRepository, never()).saveAll(anyList());
        // 파일 삭제는 수행되어야 함
        assertThat(Files.exists(sampleCsvPath)).isFalse();
    }
}
