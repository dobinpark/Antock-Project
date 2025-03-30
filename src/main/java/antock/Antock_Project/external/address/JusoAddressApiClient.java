package antock.Antock_Project.external.address;

import antock.Antock_Project.external.address.dto.AddressResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class JusoAddressApiClient implements AddressApiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String endpoint;

    public JusoAddressApiClient(WebClient.Builder webClientBuilder,
            @Value("${api.address.key}") String apiKey,
            @Value("${api.address.endpoint}") String endpoint) {
        this.webClient = webClientBuilder.baseUrl(endpoint.substring(0, endpoint.lastIndexOf("/"))).build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public AddressResponse getAddressInfo(String address) {
        log.info("공공주소 API 요청: 주소 = {}", address);

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint.substring(endpoint.lastIndexOf("/")))
                            .queryParam("currentPage", "1")      // 현재 페이지
                            .queryParam("countPerPage", "10")    // 페이지당 항목 수
                            .queryParam("keyword", address)      // 검색할 주소
                            .queryParam("confmKey", apiKey)      // API 키
                            .queryParam("resultType", "json")    // 응답 형식
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(AddressResponse.class)
                    .block();  // 동기적으로 처리
        } catch (Exception e) {
            log.error("공공주소 API 호출 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }
}
