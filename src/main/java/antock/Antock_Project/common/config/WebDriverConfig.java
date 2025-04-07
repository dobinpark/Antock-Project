package antock.Antock_Project.common.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebDriverConfig {

    private final String downloadFilePath = Paths.get("downloads").toAbsolutePath().toString();

    @PostConstruct
    void setupDriver() {
        // WebDriverManager를 사용하여 ChromeDriver 자동 설정
        WebDriverManager.chromedriver().setup();
        // 다운로드 폴더 생성 (없으면)
        File downloadDir = new File(downloadFilePath);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
    }

    @Bean
    @Scope("prototype") // 각 요청 또는 사용 시 새로운 WebDriver 인스턴스 생성
    public WebDriver chromeDriver() {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();

        // 다운로드 경로 설정
        prefs.put("download.default_directory", downloadFilePath);
        // 다운로드 시 확인 창 비활성화
        prefs.put("download.prompt_for_download", false);
        // PDF 파일을 브라우저에서 바로 열지 않고 다운로드하도록 설정 (옵션)
        prefs.put("plugins.always_open_pdf_externally", true);

        options.setExperimentalOption("prefs", prefs);

        // (선택) 백그라운드 실행 (Headless 모드) - 서버 환경 등에서 UI 없이 실행 시
        // options.addArguments("--headless");
        // options.addArguments("--disable-gpu"); // 일부 시스템에서 headless 모드 시 필요
        // options.addArguments("--window-size=1920,1080"); // 해상도 지정

        return new ChromeDriver(options);
    }

    // 참고: WebDriver 인스턴스 관리는 사용하는 측(예: FtcCsvDownloader)에서
    // @PreDestroy 등을 이용하거나 try-with-resources 방식으로 처리하는 것이 더 명확할 수 있습니다.
    // 여기서는 prototype scope 빈으로 생성하여 사용하는 측에서 관리하도록 유도합니다.
}
