package antock.Antock_Project.external.Antocker;

import antock.Antock_Project.common.exception.BusinessException;
import antock.Antock_Project.common.exception.ErrorCode;
import antock.Antock_Project.external.Antocker.dto.AntockerDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class FtcAntockerApiClient implements AntockerApiClient {

    private final WebClient webClient;

    // application.yml 파일의 키와 일치하도록 수정
    @Value("${api.antocker.endpoint}") // 키 이름 변경 (url -> endpoint)
    private String apiUrl;

    @Value("${api.antocker.key}") // 키 이름 변경 (api.ftc.antocker.key -> api.antocker.key)
    private String apiKey;

    public FtcAntockerApiClient(WebClient.Builder webClientBuilder) {
        // 기본 WebClient 설정 (타임아웃 등)
        this.webClient = webClientBuilder
                // .baseUrl(apiUrl) // Base URL 설정 시
                .defaultHeader("Authorization", "Bearer " + apiKey) // API 키 헤더 (인증 방식에 맞게 수정)
                .build();
    }

    @Override
    public Optional<AntockerDetailResponse> fetchAntockerDetails(String businessRegistrationNumber) {
        log.info("Fetching Antocker details for businessRegistrationNumber: {}", businessRegistrationNumber);

        return webClient.get()
                .uri(apiUrl + "?bizRegNum={bizRegNum}", businessRegistrationNumber) // 실제 엔드포인트 및 파라미터로 변경
                .retrieve()
                // 4xx, 5xx 에러 처리 (수정된 부분)
                .onStatus(HttpStatusCode::isError, clientResponse ->
                    clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("[No Error Body]") // 에러 본문이 없을 경우 기본값 사용
                            .flatMap(errorBody -> {
                                log.error("API call failed with status: {}, body: {}", clientResponse.statusCode(), errorBody);
                                BusinessException ex = new BusinessException(ErrorCode.API_REQUEST_FAILED,
                                        "API 요청 실패 (상태 코드: " + clientResponse.statusCode() + ", Body: " + errorBody + ")");
                                return Mono.error(ex);
                            })
                )
                .bodyToMono(AntockerDetailResponse.class) // 응답 본문을 DTO로 변환
                // 재시도 로직 (예: 네트워크 오류 시 3번 재시도)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof IOException || throwable instanceof BusinessException))
                // Optional 로 감싸서 반환 (값이 없을 수도 있음, 예: 404 Not Found)
                .blockOptional(Duration.ofSeconds(10)); // 최대 10초 대기 (블로킹 방식, 비동기 유지 시 .subscribe() 사용)
                // .onErrorResume(e -> { // 에러 발생 시 빈 Optional 반환 (또는 예외 전파)
                //     log.error("Error fetching Antocker details: {}", e.getMessage());
                //     return Mono.empty();
                // })
                // .blockOptional(Duration.ofSeconds(10)); // .onErrorResume 사용 시
    }
}
