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
     * 지정된 조건(예: 시/도 이름)에 해당하는 통신판매사업자 데이터를
     * 다운로드, 파싱, 처리하여 데이터베이스에 저장하는 전체 프로세스를 비동기적으로 실행합니다.
     *
     * @param condition 다운로드 조건 (예: "서울특별시")
     * @return 비동기 처리 결과를 나타내는 CompletableFuture (저장된 데이터 수)
     */
    @Async // common.config.AsyncConfig 에서 설정한 스레드 풀에서 비동기 실행
    @Transactional // 메소드 전체를 하나의 트랜잭션으로 관리
    public CompletableFuture<Integer> processAndSaveAntockerData(String condition) {
        log.info("Starting asynchronous processing for condition: {}", condition);
        Path downloadedCsvPath = null;
        int savedCount = 0;

        try {
            // 1. CSV 파일 다운로드
            downloadedCsvPath = ftcCsvDownloader.downloadCsvFile(condition);
            if (downloadedCsvPath == null || !Files.exists(downloadedCsvPath)) {
                log.error("CSV file download failed or file not found for condition: {}", condition);
                throw new BusinessException(ErrorCode.CSV_DOWNLOAD_FAILED, "CSV 파일 다운로드 실패 또는 파일을 찾을 수 없음");
            }

            // 2. CSV 파일 파싱
            List<Map<String, String>> parsedData = openCsvParser.parseCsvFile(downloadedCsvPath);
            if (parsedData.isEmpty()) {
                log.warn("Parsed CSV data is empty for condition: {}", condition);
                return CompletableFuture.completedFuture(0); // 처리할 데이터 없으면 0 반환
            }

            log.info("Parsed {} data rows for condition: {}", parsedData.size(), condition);

            // 3. 데이터 처리 및 저장 준비 (데이터 처리 결과를 리스트에 모음)
            List<Antocker> antockersToSave = new ArrayList<>();
            for (Map<String, String> csvRow : parsedData) {
                Optional<Antocker> processedAntockerOpt = antockerDataProcessor.processData(csvRow);
                processedAntockerOpt.ifPresent(antockersToSave::add);
                // processData 내부에서 오류 발생 시 Optional.empty() 반환되어 리스트에 추가되지 않음
            }

            // 중복 데이터 처리 로직 추가 (선택 사항) - 예: 사업자등록번호 기준
            List<Antocker> uniqueAntockers = filterUniqueAntockers(antockersToSave);

            // 4. 데이터베이스에 저장 (saveAll 사용)
            if (!uniqueAntockers.isEmpty()) {
                List<Antocker> savedAntockers = antockerRepository.saveAll(uniqueAntockers);
                savedCount = savedAntockers.size();
                log.info("Successfully saved {} Antocker entities for condition: {}", savedCount, condition);
            } else {
                log.info("No new unique Antocker entities to save for condition: {}", condition);
            }

            return CompletableFuture.completedFuture(savedCount);

        } catch (BusinessException e) {
            log.error("BusinessException during processing for condition {}: {} - {}", condition, e.getErrorCode(), e.getMessage());
            // 비동기 예외 처리를 위해 CompletableFuture.failedFuture 사용 가능하나,
            // 여기서는 로그만 남기고 정상 완료 (결과 0) 처리 또는 특정 예외 전파 고려
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

        List<Antocker> existingAntockers = antockerRepository.findAllById(getExistingIds(businessNumbers));
        List<String> existingBusinessNumbers = existingAntockers.stream()
                                                .map(Antocker::getBusinessRegistrationNumber)
                                                .toList();

        return antockers.stream()
                .filter(a -> !existingBusinessNumbers.contains(a.getBusinessRegistrationNumber()))
                .collect(Collectors.toList());
    }

    /**
     * 사업자등록번호 리스트를 받아 DB에 이미 존재하는 Antocker의 ID 리스트를 반환합니다.
     */
    private List<Long> getExistingIds(List<String> businessNumbers) {
        List<Long> existingIds = new ArrayList<>();
        // 성능 고려: 한 번에 많은 데이터를 조회하기보다 적절한 크기로 나누어 조회 (예: 1000개씩)
        int batchSize = 1000;
        for (int i = 0; i < businessNumbers.size(); i += batchSize) {
            List<String> batch = businessNumbers.subList(i, Math.min(i + batchSize, businessNumbers.size()));
            batch.forEach(num -> antockerRepository.findByBusinessRegistrationNumber(num)
                                                 .ifPresent(a -> existingIds.add(a.getId())));
        }
        return existingIds;
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
