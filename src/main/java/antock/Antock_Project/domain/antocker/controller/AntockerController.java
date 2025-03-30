package antock.Antock_Project.domain.antocker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import antock.Antock_Project.domain.antocker.dto.AntockerRequest;
import antock.Antock_Project.domain.antocker.dto.AntockerResponse;
import antock.Antock_Project.domain.antocker.service.AntockerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/antockers")
public class AntockerController {

    private final AntockerService antockerService;

    public AntockerController(AntockerService antockerService) {
        this.antockerService = antockerService;
    }

    @PostMapping // POST 요청 처리, "/api/sellers" 경로로 매핑
    public ResponseEntity<AntockerResponse> processAntockers(@Valid @RequestBody AntockerRequest request) {
        log.info("POST /api/antockers 요청: city={}, district={}", request.getCity(), request.getDistrict());

        CompletableFuture<Void> future = antockerService.processAntockers(request.getCity(), request.getDistrict());

        future.thenRun(() -> {
            log.info("AntockerService.processAntockers 비동기 작업 완료");
        }).exceptionally(e -> {
            log.error("AntockerService.processAntockers 비동기 작업 실패", e);
            return null; // or throw exception
        });

        AntockerResponse response = new AntockerResponse("통신 판매자 정보 처리 요청이 접수되었습니다.", true);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
