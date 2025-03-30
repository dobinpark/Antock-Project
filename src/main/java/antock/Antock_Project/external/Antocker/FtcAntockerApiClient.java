package antock.Antock_Project.external.Antocker;

import antock.Antock_Project.external.Antocker.dto.AntockerDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class FtcAntockerApiClient implements AntockerApiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String endpoint;

    public FtcAntockerApiClient(WebClient.Builder webClientBuilder,
            @Value("${api.antocker.key}") String apiKey,
            @Value("${api.antocker.endpoint}") String endpoint) {
        this.webClient = webClientBuilder.baseUrl(endpoint).build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        log.info("FtcAntockerApiClient initialized with endpoint: {}", this.endpoint);
    }

    @Override
    public AntockerDetailResponse getAntockerDetail(String bizRegNum) {
        log.info("통신판매사업자 상세 API 요청: 사업자등록번호 = {}", bizRegNum);

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getTospDetailInfo")
                            .queryParam("serviceKey", apiKey)
                            .queryParam("bizrno", bizRegNum)
                            .queryParam("pageNo", "1")
                            .queryParam("numOfRows", "10")
                            .queryParam("resultType", "json")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(AntockerDetailResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("통신판매사업자 상세 API 호출 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }
}
