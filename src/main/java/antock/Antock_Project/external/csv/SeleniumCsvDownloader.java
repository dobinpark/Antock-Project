package antock.Antock_Project.external.csv;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Primary
@Profile("prod") // 운영 환경에서만 사용
public class SeleniumCsvDownloader implements CsvDownloader {

    private static final String FTC_URL = "https://www.ftc.go.kr/www/selectBizCommOpenList.do?key=255";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Path DOWNLOAD_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "ftc_downloads");

    public SeleniumCsvDownloader() {
        // 생성자에서는 WebDriver 설정을 하지 않음
        // 초기화 시 다운로드 디렉토리 생성
        File downloadDir = DOWNLOAD_DIR.toFile();
        if (!downloadDir.exists()) {
            if (!downloadDir.mkdirs()) {
                log.error("Failed to create download directory: {}", DOWNLOAD_DIR);
            }
        }
    }

    @PostConstruct
    @ConditionalOnProperty(name = "selenium.enabled", havingValue = "true", matchIfMissing = false)
    public void setupWebDriver() {
        log.info("selenium.enabled=true, setting up WebDriverManager...");
        // 특정 버전 지정 없이 시스템에 설치된 Chrome에 맞는 드라이버 설정
        try {
            WebDriverManager.chromedriver().setup();
            log.info("WebDriverManager setup complete.");
        } catch (Exception e) {
            log.error("Failed to setup WebDriverManager", e);
            // WebDriver 설정 실패 시 예외를 던지거나, 적절한 처리가 필요할 수 있음
            throw new RuntimeException("Failed to setup WebDriverManager", e);
        }
    }

    @Override
    public Path downloadCsvFile(String condition) {
        // condition에서 city와 district 추출 (예: "서울,강남구" 형태 가정)
        String[] parts = condition.split(",");
        if (parts.length != 2) {
            log.error("잘못된 condition 형식입니다. '시,구' 형태여야 합니다: {}", condition);
            throw new IllegalArgumentException("Condition은 '시,구' 형태여야 합니다.");
        }
        String city = parts[0].trim();
        String district = parts[1].trim();

        log.info("Selenium을 사용하여 CSV 파일 다운로드 시작: city={}, district={}", city, district);

        WebDriver driver = null;
        try {
            // Chrome 설정 (다운로드 디렉토리, 헤드리스 모드 등)
            ChromeOptions options = setupChromeOptions();

            // WebDriver 생성
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // 공정거래위원회 사이트 접속
            driver.get(FTC_URL);
            log.info("공정거래위원회 사이트 접속 완료");

            // 페이지 소스 출력으로 디버깅
            // log.info("페이지 소스: " + driver.getPageSource().substring(0, 500) + "...");

            try {
                WebElement searchArea1 = driver.findElement(By.name("searchArea1"));
                log.info("검색 영역1 찾음: " + searchArea1.getTagName());
            } catch (Exception e) {
                log.error("검색 영역1 찾기 실패: " + e.getMessage());
            }

            // iframe이 있을 경우 먼저 이동
            try {
                WebElement iframe = driver.findElement(By.tagName("iframe"));
                driver.switchTo().frame(iframe);
                log.info("iframe으로 전환 완료");
            } catch (Exception e) {
                log.info("iframe 없음, 기본 페이지에서 계속");
            }

            // 시/도 선택
            WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
            WebElement citySelect = wait.until(ExpectedConditions.elementToBeClickable(By.name("searchArea1")));
            new Select(citySelect).selectByVisibleText(city);
            log.info("시/도 선택 완료: {}", city);

            // 구/군 선택 (시/도 선택 후 대기 필요)
            WebElement districtSelect = wait.until(ExpectedConditions.refreshed(
                    ExpectedConditions.elementToBeClickable(By.name("searchArea2"))));
            new Select(districtSelect).selectByVisibleText(district);
            log.info("구/군 선택 완료: {}", district);

            // 다운로드 버튼 클릭
            WebElement downloadButton = wait
                    .until(ExpectedConditions.elementToBeClickable(By.linkText("다운로드")));
            downloadButton.click();
            log.info("다운로드 버튼 클릭 완료");

            // 다운로드 완료 대기 (약 10초)
            Thread.sleep(10000);

            // 다운로드된 파일 찾기
            File downloadedFile = findDownloadedFile();
            if (downloadedFile != null) {
                log.info("CSV 파일 다운로드 완료: {}", downloadedFile.getAbsolutePath());
                return downloadedFile.toPath();
            } else {
                log.error("다운로드된 CSV 파일을 찾을 수 없습니다.");
                throw new RuntimeException("다운로드된 CSV 파일을 찾을 수 없습니다.");
            }

        } catch (Exception e) {
            log.error("Selenium CSV 다운로드 중 오류 발생", e);
            throw new RuntimeException("Selenium CSV 다운로드 중 오류 발생", e);
        } finally {
            // WebDriver 종료
            if (driver != null) {
                driver.quit();
                log.info("WebDriver 종료");
            }
        }
    }

    private ChromeOptions setupChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new"); // 헤드리스 모드 필요시 활성화
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*"); // 원격 연결 허용
        options.addArguments("--disable-extensions"); // 확장 기능 비활성화
        // options.addArguments("--remote-debugging-port=9222"); // 디버깅 포트 설정 필요시 활성화

        // 다운로드 설정
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", DOWNLOAD_DIR.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", false); // 안전 브라우징 비활성화 (다운로드 차단 방지)
        options.setExperimentalOption("prefs", prefs);

        return options;
    }

    private File findDownloadedFile() {
        File dir = DOWNLOAD_DIR.toFile();
        if (!dir.exists())
            return null; // 디렉토리가 없으면 null 반환

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv")); // 모든 CSV 파일 대상

        if (files != null && files.length > 0) {
            // 가장 최근에 수정된 파일 반환
            File latestFile = files[0];
            for (int i = 1; i < files.length; i++) {
                if (files[i].lastModified() > latestFile.lastModified()) {
                    latestFile = files[i];
                }
            }
            // 특정 이름 패턴 확인 (선택 사항)
            if (latestFile.getName().contains("통신판매사업자") || latestFile.getName().contains("tospbizr")) {
                log.info("다운로드된 파일 확인: {}", latestFile.getName());
                return latestFile;
            } else {
                log.warn("다운로드된 최신 파일 이름이 예상 패턴과 다릅니다: {}", latestFile.getName());
                // 패턴이 달라도 일단 반환하거나, 더 엄격하게 처리할 수 있음
                return latestFile;
            }
        }
        return null;
    }
}
