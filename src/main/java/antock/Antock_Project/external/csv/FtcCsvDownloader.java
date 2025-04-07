package antock.Antock_Project.external.csv;

import antock.Antock_Project.common.exception.BusinessException;
import antock.Antock_Project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
//@RequiredArgsConstructor // WebDriver는 prototype scope이므로 생성자 주입 대신 직접 받아와야 할 수 있음
public class FtcCsvDownloader implements CsvDownloader {

    private final WebDriver webDriver; // Prototype scope WebDriver 주입
    private final String downloadFilePath;
    private final String targetUrl = "https://www.ftc.go.kr/www/selectBizCommOpenList.do?key=255"; // 대상 URL

    // WebDriverConfig에서 설정한 다운로드 경로 주입 (application.properties 또는 직접 경로 지정 필요)
    // 여기서는 WebDriverConfig의 경로를 직접 사용하도록 가정
    public FtcCsvDownloader(WebDriver webDriver) {
        this.webDriver = webDriver;
        // WebDriverConfig에서 설정한 경로와 동일하게 설정
        this.downloadFilePath = Paths.get("downloads").toAbsolutePath().toString();
    }


    @Override
    public Path downloadCsvFile(String condition) { // condition은 '시/도' 이름 (예: "서울특별시")
        log.info("Starting CSV download for condition: {}", condition);
        WebDriver driver = null; // WebDriver 인스턴스 관리

        try {
            driver = this.webDriver; // Prototype scope 빈에서 새 인스턴스 가져오기
            driver.get(targetUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // 최대 20초 대기

            // 1. '시/도 선택' 드롭다운 요소 찾기 및 선택
            WebElement cityDropdownElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("opt_area1")));
            Select citySelect = new Select(cityDropdownElement);
            citySelect.selectByVisibleText(condition); // 입력받은 condition(시/도 이름)으로 선택
            log.info("Selected city: {}", condition);

            // 2. '자료 다운로드' 버튼 요소 찾기 및 클릭
            // 버튼 텍스트나 다른 속성으로 찾기 (XPath 예시, 실제 웹사이트 구조에 맞게 수정 필요)
            WebElement downloadButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'자료 다운로드')]")));
            // JavaScript 클릭이 더 안정적일 수 있음
             ((JavascriptExecutor) driver).executeScript("arguments[0].click();", downloadButton);
            // downloadButton.click(); // 일반 클릭
            log.info("Clicked download button.");


            // 3. 파일 다운로드 대기
            Path downloadedFile = waitForFileDownload(condition);
            log.info("CSV file downloaded successfully: {}", downloadedFile);
            return downloadedFile;

        } catch (NoSuchElementException e) {
            log.error("Failed to find element for condition '{}': {}", condition, e.getMessage());
            throw new BusinessException(ErrorCode.ELEMENT_NOT_FOUND, "웹 페이지 요소를 찾을 수 없습니다 (지역: " + condition + ")");
        } catch (TimeoutException e) {
            log.error("Timeout waiting for element or download for condition '{}': {}", condition, e.getMessage());
            throw new BusinessException(ErrorCode.DOWNLOAD_TIMEOUT, "웹 페이지 로딩 또는 파일 다운로드 시간 초과 (지역: " + condition + ")");
        } catch (Exception e) { // WebDriverException 등 Selenium 관련 다른 예외 처리
            log.error("Error during CSV download for condition '{}': {}", condition, e.getMessage(), e);
            throw new BusinessException(ErrorCode.CSV_DOWNLOAD_FAILED, "CSV 파일 다운로드 중 오류 발생 (지역: " + condition + ")");
        } finally {
            if (driver != null) {
                driver.quit(); // WebDriver 종료 (리소스 해제)
                log.info("WebDriver quit for condition: {}", condition);
            }
        }
    }

    private Path waitForFileDownload(String condition) {
        // 예상 파일 이름 패턴 (시/도 이름만 포함하는 경우)
        // 실제 파일 이름에 구/군 정보가 포함될 수 있으므로 startsWith 사용
         String expectedFileNamePrefix = "통신판매사업자_" + condition; // 예: "통신판매사업자_서울특별시"
         String fileExtension = ".csv";
         long timeoutMillis = 60000; // 최대 대기 시간 (60초)
         long pollIntervalMillis = 1000; // 확인 간격 (1초)
         long startTime = System.currentTimeMillis();

        Path downloadDir = Paths.get(downloadFilePath);

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try (Stream<Path> stream = Files.list(downloadDir)) {
                 Optional<Path> downloadedFile = stream
                         .filter(p -> !Files.isDirectory(p)) // 디렉토리는 제외
                         .filter(p -> p.getFileName().toString().startsWith(expectedFileNamePrefix)) // 예상 접두사 확인
                         .filter(p -> p.getFileName().toString().endsWith(fileExtension)) // .csv 확장자 확인
                         .filter(p -> !p.getFileName().toString().endsWith(".crdownload")) // Chrome 임시 파일 제외
                         .findFirst(); // 여러 파일이 매칭될 경우 첫 번째 파일 선택 (가장 최근 파일일 가능성 높음)

                if (downloadedFile.isPresent()) {
                     // 파일이 완전히 다운로드되었는지 추가 확인 (간단한 방법: 잠시 대기 후 파일 크기 확인)
                    try {
                        Thread.sleep(2000); // 2초 대기 (파일 쓰기 완료 시간 확보)
                    } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

                     // 파일이 여전히 존재하는지 확인 (다운로드 중 오류로 삭제되었을 수 있음)
                    if (Files.exists(downloadedFile.get())) {
                         return downloadedFile.get();
                    }
                }
            } catch (IOException e) {
                log.error("Error checking download directory: {}", e.getMessage());
                // 디렉토리 접근 오류 시 잠시 후 다시 시도
            }

            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("File download wait interrupted.");
                break; // 인터럽트 발생 시 루프 종료
            }
        }

        // Timeout
        throw new TimeoutException("CSV file download timed out after " + (timeoutMillis / 1000) + " seconds for condition: " + condition);
    }

}
