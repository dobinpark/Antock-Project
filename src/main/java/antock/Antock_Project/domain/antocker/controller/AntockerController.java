package antock.Antock_Project.domain.antocker.controller;

import antock.Antock_Project.domain.antocker.service.AntockerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/antockers")
@RequiredArgsConstructor
public class AntockerController {

    private final AntockerService antockerService;

    /**
     * 지정된 지역의 통신판매사업자 데이터 처리 프로세스를 시작합니다.
     * 비동기 서비스 호출 후 즉시 응답합니다.
     *
     * @param region 처리할 지역 (예: "서울특별시")
     * @return 처리 시작 응답 (HTTP 202 Accepted)
     */
    @PostMapping("/process")
    public ResponseEntity<String> startProcessing(@RequestParam String region) {
        log.info("Received request to start processing for region: {}", region);

        // AntockerService의 비동기 메소드 호출
        CompletableFuture<Integer> futureResult = antockerService.processAndSaveAntockerData(region);

        // 비동기 작업 완료 여부와 관계없이 즉시 응답
        // 비동기 작업 결과 처리가 필요하다면 콜백 등을 사용하여 별도 처리
        futureResult.whenComplete((result, ex) -> {
            if (ex != null) {
                // 비동기 작업 중 예외 발생 시 로그 기록 (별도의 에러 알림 메커니즘 고려)
                log.error("Asynchronous processing failed for region: {} - Error: {}", region, ex.getMessage(), ex);
            } else {
                // 비동기 작업 성공 시 로그 기록
                log.info("Asynchronous processing completed successfully for region: {}. Saved {} items.", region, result);
            }
        });

        // 클라이언트에게는 처리 요청이 접수되었음을 알림 (HTTP 202 Accepted)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Processing started for region: " + region);
    }
}
