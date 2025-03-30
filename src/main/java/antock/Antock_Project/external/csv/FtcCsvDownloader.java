package antock.Antock_Project.external.csv;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FtcCsvDownloader implements CsvDownloader {

    private final WebClient webClient;

    public FtcCsvDownloader(WebClient.Builder webClientBuilder) {
        // 버퍼 크기를 10MB로 증가
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.webClient = webClientBuilder
                .baseUrl("https://www.ftc.go.kr")
                .exchangeStrategies(exchangeStrategies)
                .build();
    }

    @Override
    public File downloadCsvFile(String city, String district) {
        try {
            // URL 인코딩 적용
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String encodedDistrict = URLEncoder.encode(district, StandardCharsets.UTF_8);
            String encodedSearch = URLEncoder.encode("전국", StandardCharsets.UTF_8);

            // 절대 URI 사용 (https://로 시작)
            String uriString = String.format(
                    "https://www.ftc.go.kr/www/selectBizCommOpenList.do?key=255&search=%s&opt_area1=%s&opt_area2=%s",
                    encodedSearch, encodedCity, encodedDistrict);

            URI uri = new URI(uriString);

            log.info("CSV 다운로드 요청 URI: {}", uri);

            byte[] csvBytes = webClient.get()
                    .uri(uri) // 절대 URI 사용
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (csvBytes == null || csvBytes.length == 0) {
                log.warn("CSV 파일 다운로드 실패: 응답 내용 없음");
                return null; // 또는 예외 처리
            }

            // 임시 파일 생성 (다운로드 받은 CSV 파일 저장)
            File tempFile = File.createTempFile("ftc_bizcomm_", ".csv");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(csvBytes);
            }
            log.info("CSV 파일 다운로드 완료. 임시 파일 경로: {}", tempFile.getAbsolutePath());
            return tempFile;

        } catch (URISyntaxException | IOException e) {
            log.error("CSV 파일 다운로드 중 오류 발생", e);
            // 예외 처리 (BusinessException 던지거나, null 반환 등)
            return null; // 또는 예외 처리
        }
    }
}
