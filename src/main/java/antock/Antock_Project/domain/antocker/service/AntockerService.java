package antock.Antock_Project.domain.antocker.service;

import antock.Antock_Project.common.exception.BusinessException;
import antock.Antock_Project.common.exception.ErrorCode;
import antock.Antock_Project.domain.antocker.entity.Antocker;
import antock.Antock_Project.domain.antocker.repository.AntockerRepository;
import antock.Antock_Project.external.csv.CsvDownloader;
import antock.Antock_Project.external.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntockerService {

    private final CsvDownloader ftcCsvDownloader; // FtcCsvDownloader 구현체 주입
    private final CsvParser openCsvParser;       // OpenCsvParser 구현체 주입
    private final AntockerDataProcessor antockerDataProcessor;
    private final AntockerRepository antockerRepository;

    /**
     * 특정 조건(예: 지역)에 해당하는 통신판매업자 데이터를 처리하고 저장합니다.
     * 1. CSV 파일 다운로드
     * 2. CSV 파일 파싱
     * 3. 데이터 처리 (외부 API 호출 등)
     * 4. 데이터베이스 저장
     * 5. 임시 파일 정리
     *
     * @param condition 다운로드 및 처리 조건 (예: "서울,강남구")
     * @return 저장된 데이터 개수를 포함하는 CompletableFuture
     */
    @Async // common.config.AsyncConfig 에서 설정한 스레드 풀에서 비동기 실행
    @Transactional // 메소드 전체를 하나의 트랜잭션으로 관리
    public CompletableFuture<Integer> processAndSaveAntockerData(String condition) {
        log.info("Starting Antocker data processing for condition: {}", condition);
        Path downloadedCsvPath = null;
        int savedCount = 0;

        try {
            // 1. CSV 파일 다운로드
            downloadedCsvPath = ftcCsvDownloader.downloadCsvFile(condition);
            if (downloadedCsvPath == null) {
                log.error("CSV file download failed for condition: {}", condition);
                // 실패 시 빈 CompletableFuture 또는 예외를 포함한 Future 반환 고려
                return CompletableFuture.completedFuture(0);
            }
            log.info("CSV file downloaded successfully: {}", downloadedCsvPath);

            // 2. CSV 파일 파싱
            List<Map<String, String>> parsedData = openCsvParser.parseCsvFile(downloadedCsvPath);
            if (parsedData == null || parsedData.isEmpty()) {
                log.warn("Parsed CSV data is empty for condition: {}", condition);
                return CompletableFuture.completedFuture(0); // 처리할 데이터 없으면 0 반환
            }

            log.info("Parsed {} data rows for condition: {}", parsedData.size(), condition);

            // 3. 데이터 처리 (비동기 호출 및 결과 수집)
            List<CompletableFuture<Antocker>> futures = parsedData.stream()
                    .map(antockerDataProcessor::processAntockerData) // processAntockerData 호출
                    .collect(Collectors.toList());

            // 모든 비동기 작업이 완료될 때까지 대기
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            // 모든 작업 완료 후 결과 처리
            return allOf.handle((v, ex) -> { // handle 사용하여 예외 처리 포함
                int currentSavedCount = 0;
                if (ex != null) {
                    // allOf에서 예외 발생 시 (보통 futures 중 하나에서 예외 발생)
                    log.error("Error waiting for Antocker data processing completion for condition {}: {}", condition, ex.getMessage(), ex);
                     // 여기서 예외를 다시 던지거나, 에러 상황에 맞는 값을 반환할 수 있습니다.
                     // 예외를 CompletableFuture로 감싸서 전파하면 호출 측에서 처리 가능
                    throw new CompletionException(ex); // 예외를 CompletionException으로 감싸서 전파
                }

                // 성공적으로 완료된 경우, 결과 수집
                List<Antocker> antockersToSave = futures.stream()
                        .map(future -> {
                            try {
                                // 각 Future에서 결과 가져오기 (allOf로 완료 보장되므로 join 사용)
                                return future.join();
                            } catch (CompletionException ce) {
                                // 개별 processAntockerData 호출에서 발생한 예외 처리
                                log.error("Error processing individual Antocker data for condition {}: {}", condition, ce.getMessage(), ce.getCause());
                                return null; // 예외 발생 시 null 반환 (또는 다른 방식으로 처리)
                            }
                        })
                        .filter(java.util.Objects::nonNull) // null이 아닌 결과만 필터링
                        .collect(Collectors.toList());

                log.info("Successfully processed {} Antocker data entries for condition: {}", antockersToSave.size(), condition);

                // 처리된 데이터 리스트 내에서 사업자등록번호 기준으로 중복 제거
                Map<String, Antocker> uniqueAntockersMap = antockersToSave.stream()
                        .collect(Collectors.toMap(
                                Antocker::getBusinessRegistrationNumber,
                                antocker -> antocker,
                                (existing, replacement) -> existing // 중복 시 기존 값 유지 (또는 로직 추가)
                        ));
                List<Antocker> uniqueProcessedAntockers = new ArrayList<>(uniqueAntockersMap.values());
                log.info("Removed {} duplicates from processed data for condition: {}", antockersToSave.size() - uniqueProcessedAntockers.size(), condition);

                // DB에 이미 존재하는 데이터와 중복 제거
                List<Antocker> uniqueAntockersToSave = filterUniqueAntockers(uniqueProcessedAntockers);

                // 4. 데이터베이스에 저장 (saveAll 사용)
                if (!uniqueAntockersToSave.isEmpty()) {
                    List<Antocker> savedAntockers = antockerRepository.saveAll(uniqueAntockersToSave);
                    currentSavedCount = savedAntockers.size();
                    log.info("Successfully saved {} unique Antocker entities for condition: {}", currentSavedCount, condition);
                } else {
                    log.info("No new unique Antocker entities to save for condition: {}", condition);
                }

                return currentSavedCount; // 최종 저장된 개수 반환
            });

        } catch (BusinessException e) {
            log.error("BusinessException during processing for condition {}: {} - {}", condition, e.getErrorCode(), e.getMessage());
             return CompletableFuture.failedFuture(e); // 예외를 CompletableFuture로 감싸 반환
        } catch (Exception e) {
            log.error("Unexpected error during processing for condition {}: {}", condition, e.getMessage(), e);
             return CompletableFuture.failedFuture(e); // 예외를 CompletableFuture로 감싸 반환
        } finally {
            // 5. 다운로드된 임시 CSV 파일 삭제
            cleanupDownloadedFile(downloadedCsvPath);
        }
    }

    /**
     * 저장할 Antocker 리스트에서 사업자등록번호 기준으로 중복을 제거합니다.
     * (이미 DB에 있는 데이터는 제외)
     */
    private List<Antocker> filterUniqueAntockers(List<Antocker> antockers) {
        if (antockers.isEmpty()) {
            return List.of();
        }
        // DB 조회 최소화를 위해 Set 사용
        List<String> businessNumbers = antockers.stream()
                .map(Antocker::getBusinessRegistrationNumber)
                .distinct()
                .collect(Collectors.toList());

        // findAllById 대신 findByBusinessRegistrationNumberIn 사용 고려 (더 효율적일 수 있음)
        // Set<String> existingBusinessNumbers = antockerRepository.findByBusinessRegistrationNumberIn(businessNumbers)
        //         .stream()
        //         .map(Antocker::getBusinessRegistrationNumber)
        //         .collect(Collectors.toSet());
        List<String> existingBusinessNumbers = getExistingBusinessNumbers(businessNumbers);

        return antockers.stream()
                // .filter(a -> !existingBusinessNumbers.contains(a.getBusinessRegistrationNumber()))
                .filter(a -> !existingBusinessNumbers.contains(a.getBusinessRegistrationNumber())) // 수정: Set을 사용하므로 contains는 효율적
                .collect(Collectors.toList());
    }

    /**
     * 사업자등록번호 리스트를 받아 DB에 이미 존재하는 사업자등록번호 Set을 반환합니다.
     * (getExistingIds + 추가 조회 대신 한 번의 조회로 최적화)
     */
    private List<String> getExistingBusinessNumbers(List<String> businessNumbers) {
        List<String> existingNumbers = new ArrayList<>();
        int batchSize = 1000;
        for (int i = 0; i < businessNumbers.size(); i += batchSize) {
            List<String> batch = businessNumbers.subList(i, Math.min(i + batchSize, businessNumbers.size()));
            // findByBusinessRegistrationNumberIn과 같은 메소드가 Repository에 정의되어 있다고 가정
            // 만약 없다면 Repository에 추가 필요: List<Antocker> findByBusinessRegistrationNumberIn(List<String> numbers);
            existingNumbers.addAll(antockerRepository.findByBusinessRegistrationNumberIn(batch)
                                        .stream()
                                        .map(Antocker::getBusinessRegistrationNumber)
                                        .toList());
        }
        return existingNumbers;
    }

    /**
     * 다운로드된 임시 파일을 삭제합니다.
     */
    private void cleanupDownloadedFile(Path filePath) {
        if (filePath != null && Files.exists(filePath)) {
            try {
                Files.delete(filePath);
                log.info("Cleaned up temporary file: {}", filePath);
            } catch (IOException e) {
                log.error("Failed to clean up temporary file: {}", filePath, e);
            }
        }
    }
}
