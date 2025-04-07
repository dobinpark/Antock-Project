package antock.Antock_Project.external.csv;

import antock.Antock_Project.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OpenCsvParserTest {

    private OpenCsvParser openCsvParser;

    // JUnit 5의 @TempDir: 테스트 실행 시 임시 디렉토리 생성 및 자동 삭제
    @TempDir
    Path tempDir;

    private Path sampleCsvFile;

    // OpenCsvParser에서 사용하는 인코딩과 동일하게 설정 (EUC-KR 또는 UTF-8 등)
    private static final Charset TEST_CSV_CHARSET = Charset.forName("EUC-KR");

    @BeforeEach
    void setUp() {
        openCsvParser = new OpenCsvParser(); // 테스트 대상 인스턴스 생성
    }

    // 테스트용 임시 CSV 파일 생성 헬퍼 메소드
    private Path createTempCsvFile(String fileName, String content) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, TEST_CSV_CHARSET)) {
            writer.write(content);
        }
        return filePath;
    }

    @Test
    @DisplayName("정상적인 CSV 파일 파싱 성공")
    void parseCsvFile_Success() throws IOException {
        // given
        String csvContent = "사업자등록번호,상호명,사업장소재지\n" + // Header
                "1111111111,상점A,주소A\n" +
                "2222222222,상점B,\"주소B, 상세주소\"\n" + // 주소에 콤마 포함
                "3333333333, 상점C , 주소C "; // 앞뒤 공백 포함
        sampleCsvFile = createTempCsvFile("success.csv", csvContent);

        // when
        List<Map<String, String>> result = openCsvParser.parseCsvFile(sampleCsvFile);

        // then
        assertThat(result).hasSize(3); // 데이터 3행 확인

        // 첫 번째 행 데이터 검증
        assertThat(result.get(0))
                .containsEntry("사업자등록번호", "1111111111")
                .containsEntry("상호명", "상점A")
                .containsEntry("사업장소재지", "주소A");

        // 두 번째 행 데이터 검증 (콤마 포함 데이터)
        assertThat(result.get(1))
                .containsEntry("사업자등록번호", "2222222222")
                .containsEntry("상호명", "상점B")
                .containsEntry("사업장소재지", "주소B, 상세주소");

        // 세 번째 행 데이터 검증 (공백 제거 확인)
        assertThat(result.get(2))
                .containsEntry("사업자등록번호", "3333333333")
                .containsEntry("상호명", "상점C")
                .containsEntry("사업장소재지", "주소C");
    }

    @Test
    @DisplayName("비어있는 CSV 파일 처리")
    void parseCsvFile_EmptyFile() throws IOException {
        // given
        sampleCsvFile = createTempCsvFile("empty.csv", ""); // 빈 파일 생성

        // when & then
        // 빈 파일은 헤더를 읽을 수 없으므로 BusinessException 발생 예상
        assertThatThrownBy(() -> openCsvParser.parseCsvFile(sampleCsvFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV 파일 헤더를 읽을 수 없거나 비어있습니다.");
    }

    @Test
    @DisplayName("헤더만 있는 CSV 파일 처리")
    void parseCsvFile_HeaderOnly() throws IOException {
        // given
        sampleCsvFile = createTempCsvFile("header_only.csv", "사업자등록번호,상호명,사업장소재지");

        // when
        List<Map<String, String>> result = openCsvParser.parseCsvFile(sampleCsvFile);

        // then
        assertThat(result).isEmpty(); // 데이터 행이 없으므로 빈 리스트 반환
    }

    @Test
    @DisplayName("컬럼 수가 맞지 않는 행은 건너뛰고 파싱")
    void parseCsvFile_ColumnCountMismatch() throws IOException {
        // given
        String csvContent = "번호,이름\n" +
                "1,홍길동\n" +
                "2,임꺽정,추가정보\n" + // 컬럼 수 불일치
                "3,장보고";
        sampleCsvFile = createTempCsvFile("mismatch.csv", csvContent);

        // when
        List<Map<String, String>> result = openCsvParser.parseCsvFile(sampleCsvFile);

        // then
        assertThat(result).hasSize(2); // 컬럼 수가 맞는 1, 3번 행만 파싱
        assertThat(result.get(0)).containsEntry("번호", "1").containsEntry("이름", "홍길동");
        assertThat(result.get(1)).containsEntry("번호", "3").containsEntry("이름", "장보고");
        // 로그에 경고 메시지가 출력되었는지 확인하는 것은 Mocking 로거 필요 (여기서는 생략)
    }

    @Test
    @DisplayName("존재하지 않는 파일 처리 시 예외 발생")
    void parseCsvFile_FileNotFound() {
        // given
        Path nonExistentFile = tempDir.resolve("non_existent.csv");

        // when & then
        assertThatThrownBy(() -> openCsvParser.parseCsvFile(nonExistentFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV 파일 헤더를 읽는 중 오류 발생"); // IOException이 BusinessException으로 변환
    }

    // TODO: 필요시 다른 인코딩 테스트 케이스 추가
    // @Test
    // @DisplayName("UTF-8 인코딩 CSV 파일 파싱")
    // void parseCsvFile_Utf8Encoding() throws IOException { ... }

}
