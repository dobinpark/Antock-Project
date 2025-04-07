package antock.Antock_Project.external.address;

import antock.Antock_Project.common.exception.BusinessException;
import antock.Antock_Project.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import antock.Antock_Project.external.address.dto.AddressResponse;

@Slf4j
@Component
public class JusoAddressApiClient implements AddressApiClient {

    private final WebClient webClient;

    @Value("${api.address.endpoint}") // 키 이름 변경 (url -> endpoint)
    private String apiUrl;

    @Value("${api.address.key}") // 키 이름 변경 (api.juso.key -> api.address.key)
    private String apiKey;

    public JusoAddressApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                // .baseUrl(apiUrl) // 필요시 Base URL 설정
                .build();
    }

    @Override
    public Optional<AddressResponse> fetchAddressInfo(String address) {
        log.debug("Fetching address info for address: {}", address);

        // 주소 API는 일반적으로 결과가 여러 개일 수 있으므로, 첫 번째 결과를 사용하거나 별도 처리가 필요할 수 있음
        // 여기서는 첫 번째 결과만 가져오는 것을 가정
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(apiUrl) // URL Path가 있다면 여기에 설정, 없다면 baseUrl 사용
                        .queryParam("confmKey", apiKey)
                        .queryParam("currentPage", 1)
                        .queryParam("countPerPage", 1) // 결과 1개만 요청
                        .queryParam("keyword", address)
                        .queryParam("resultType", "json") // 응답 형식 JSON
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("[No Error Body]")
                        .flatMap(errorBody -> {
                            log.error("Juso API call failed with status: {}, body: {}", clientResponse.statusCode(),
                                    errorBody);
                            // 주소 API의 에러 응답 형식에 따라 상세 에러 메시지 파싱 가능
                            BusinessException ex = new BusinessException(ErrorCode.API_REQUEST_FAILED,
                                    "주소 API 요청 실패 (상태 코드: " + clientResponse.statusCode() + ", Body: " + errorBody
                                            + ")");
                            return Mono.error(ex);
                        }))
                // 주소 API 응답은 실제 데이터가 'results.juso[0]' 와 같이 중첩 구조일 수 있음
                // 따라서 바로 AddressResponse.class로 변환하기 전에 중간 처리가 필요할 수 있음
                // 여기서는 직접 매핑을 시도하고, 실제 구조에 맞게 수정 필요
                .bodyToMono(AddressApiResponseWrapper.class) // 실제 응답 구조에 맞는 Wrapper 클래스 필요
                .flatMap(wrapper -> { // flatMap으로 변경
                    if (wrapper != null && wrapper.getResults() != null && wrapper.getResults().getJuso() != null
                            && !wrapper.getResults().getJuso().isEmpty()) {
                        // 결과가 있으면 Mono<AddressResponse> 반환
                        return Mono.just(wrapper.getResults().getJuso().get(0));
                    } else {
                        log.warn("No address found or unexpected response structure for: {}", address);
                        // 결과가 없으면 빈 Mono 반환
                        return Mono.empty();
                    }
                })
                // .cast(AddressResponse.class) // flatMap으로 타입이 명확해졌으므로 cast 불필요
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof IOException))
                .blockOptional(Duration.ofSeconds(10)); // 이제 Mono<AddressResponse>에 대해 blockOptional 호출
    }

    // 주소 API 응답의 전체 구조를 감싸는 Wrapper 클래스 (실제 응답 구조에 맞게 정의 필요)
    @lombok.Data
    private static class AddressApiResponseWrapper {
        private JusoResults results;
    }

    @lombok.Data
    private static class JusoResults {
        private Common common;
        private java.util.List<AddressResponse> juso;
    }

    @lombok.Data
    private static class Common {
        private String totalCount;
        private String currentPage;
        private String countPerPage;
        private String errorCode;
        private String errorMessage;
    }
}
