package antock.Antock_Project;

import antock.Antock_Project.external.csv.SeleniumCsvDownloader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AntockProjectApplication.class)
@ActiveProfiles("test")
class AntockProjectApplicationTests {

	// SeleniumCsvDownloader가 컨텍스트 로딩 시 WebDriver를 초기화하려고 시도하는 것을 방지
	@MockBean
	private SeleniumCsvDownloader seleniumCsvDownloader; // CsvDownloader 인터페이스 대신 구체 클래스 사용

	@Test
	void contextLoads() {
		// 컨텍스트 로딩 성공 여부만 확인
	}
}
