package antock.Antock_Project.domain.antocker.service;

import antock.Antock_Project.domain.antocker.entity.Antocker;
import antock.Antock_Project.external.csv.CsvDownloader;
import antock.Antock_Project.external.csv.CsvParser;
import antock.Antock_Project.infrastructure.storage.AntockerStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AntockerService {

    private final AntockerStorageService antockerStorageService;
    private final CsvDownloader csvDownloader;
    private final CsvParser csvParser;
    private final AntockerDataProcessor antockerDataProcessor;

    public AntockerService(AntockerStorageService antockerStorageService,
            CsvDownloader csvDownloader,
            CsvParser csvParser,
            AntockerDataProcessor antockerDataProcessor) {
        this.antockerStorageService = antockerStorageService;
        this.csvDownloader = csvDownloader;
        this.csvParser = csvParser;
        this.antockerDataProcessor = antockerDataProcessor;
    }

    @Async // 비동기 처리 활성화 (메인 스레드와 분리)
    public CompletableFuture<Void> processAntockers(String city, String district) {
        log.info("processAntockers 시작: city={}, district={}", city, district);

        File csvFile = csvDownloader.downloadCsvFile(city, district);
        if (csvFile == null) {
            log.error("CSV 파일 다운로드 실패");
            return CompletableFuture.completedFuture(null); // or CompletableFuture.failedFuture(...)
        }

        List<Map<String, String>> parsedCsvData = csvParser.parseCsvFile(csvFile);
        if (parsedCsvData.isEmpty()) {
            log.warn("CSV 파일 파싱 결과 데이터 없음");
            return CompletableFuture.completedFuture(null);
        }

        List<Map<String, String>> corporateAntockers = parsedCsvData.stream()
                .filter(row -> isCorporateAntocker(row.get("사업자등록번호"))) // "사업자등록번호" 컬럼명으로 변경
                .collect(Collectors.toList());

        log.info("총 {}개 법인 필터링 완료", corporateAntockers.size());

        // 2. 데이터 변환 및 처리 서비스 호출 (멀티쓰레딩 적용)
        List<CompletableFuture<Antocker>> antockerFutures = corporateAntockers.stream()
                .map(antockerDataProcessor::processAntockerData) // AntockerDataProcessor.processAntockerData 비동기 호출
                .collect(Collectors.toList());

        // CompletableFuture 리스트를 모두 완료될 때까지 기다림
        CompletableFuture.allOf(antockerFutures.toArray(CompletableFuture[]::new)).join();

        List<Antocker> antockers = antockerFutures.stream()
                .map(CompletableFuture::join) // 각 CompletableFuture 결과를 가져옴 (동기적으로 대기)
                .collect(Collectors.toList());

        antockerStorageService.saveAll(antockers); // AntockerStorageService 사용
        log.info("총 {}개 Antocker DB 저장 완료", antockers.size());

        log.info("processAntockers 완료: city={}, district={}", city, district);
        return CompletableFuture.completedFuture(null);
    }

    private boolean isCorporateAntocker(String bizRegNum) {
        // 사업자등록번호 10자리 중 9번째 자리가 8로 시작하면 법인으로 간주 (임시 로직, 실제 검증 로직 필요)
        return bizRegNum != null && bizRegNum.length() == 10 && bizRegNum.charAt(8) == '8';
    }
}
