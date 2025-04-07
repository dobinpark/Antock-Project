package antock.Antock_Project.external.csv;

import antock.Antock_Project.common.exception.BusinessException;
import antock.Antock_Project.common.exception.ErrorCode;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenCsvParser implements CsvParser {

    // 공정거래위원회 CSV 파일의 인코딩 (EUC-KR 가능성 높음, 실제 파일 확인 필요)
    private static final Charset CSV_CHARSET = Charset.forName("EUC-KR"); // 또는 StandardCharsets.UTF_8

    @Override
    public List<Map<String, String>> parseCsvFile(Path filePath) {
        log.info("Starting CSV parsing for file: {}", filePath);
        List<Map<String, String>> dataList = new ArrayList<>();

        String[] headers;
        try {
            headers = readHeaders(filePath); // 헤더 읽기
            if (headers == null || headers.length == 0) {
                log.error("Failed to read headers or headers are empty from CSV file: {}", filePath);
                throw new BusinessException(ErrorCode.CSV_PARSE_FAILED, "CSV 파일 헤더를 읽을 수 없거나 비어있습니다.");
            }
        } catch (IOException e) {
            log.error("IOException while reading headers from file {}: {}", filePath, e.getMessage(), e);
            throw new BusinessException(ErrorCode.CSV_PARSE_FAILED, "CSV 파일 헤더를 읽는 중 오류 발생: " + e.getMessage());
        }

        // try-with-resources를 사용하여 Reader 자동 종료
        try (Reader reader = new FileReader(filePath.toFile(), CSV_CHARSET);
             // 헤더를 이미 읽었으므로 여기서는 skipLines(0) 또는 기본값 사용 가능, 단, readAll()은 헤더 포함하여 읽음
             // 여기서는 CSVReader만 사용하여 readAll()로 전체를 읽고, 첫 줄(헤더)은 제외하는 방식으로 변경
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> lines = csvReader.readAll();

            // 첫 줄(헤더) 제외하고 처리
            if (lines.size() <= 1) {
                log.warn("CSV file has no data rows (only header or empty): {}", filePath);
                return dataList; // 데이터 없음
            }

            // 헤더를 제외한 실제 데이터 라인부터 처리
            for (int i = 1; i < lines.size(); i++) {
                String[] line = lines.get(i);
                if (line.length == headers.length) { // 헤더 개수와 데이터 개수가 일치하는지 확인
                    Map<String, String> dataMap = new HashMap<>();
                    for (int j = 0; j < headers.length; j++) {
                        dataMap.put(headers[j].trim(), line[j].trim()); // 헤더와 데이터 매핑 (공백 제거)
                    }
                    dataList.add(dataMap);
                } else {
                    log.warn("Skipping line #{} due to column count mismatch. Expected: {}, Actual: {}, Line: {}",
                            i + 1, headers.length, line.length, String.join(",", line));
                }
            }

            log.info("Successfully parsed {} lines from CSV file: {}", dataList.size(), filePath);
            return dataList;

        } catch (IOException e) {
            log.error("IOException during CSV parsing for file {}: {}", filePath, e.getMessage(), e);
            throw new BusinessException(ErrorCode.CSV_PARSE_FAILED, "CSV 파일을 읽는 중 오류 발생: " + e.getMessage());
        } catch (CsvException e) { // CsvValidationException 포함 CsvException 처리
            log.error("CsvException during CSV parsing for file {}: {}", filePath, e.getMessage(), e);
            throw new BusinessException(ErrorCode.CSV_PARSE_FAILED, "CSV 파일 파싱 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * CSV 파일의 첫 번째 줄을 읽어 헤더 배열을 반환합니다.
     * CsvValidationException 처리를 추가합니다.
     */
    private String[] readHeaders(Path filePath) throws IOException {
        try (Reader reader = new FileReader(filePath.toFile(), CSV_CHARSET);
             CSVReader csvReader = new CSVReader(reader)) {
            return csvReader.readNext(); // 첫 번째 줄 읽기
        } catch (CsvValidationException e) {
            // CsvValidationException은 CSV 내용 자체의 유효성 문제일 수 있음
            log.error("CsvValidationException while reading headers from file {}: {}", filePath, e.getMessage(), e);
            // IOException으로 감싸서 반환하거나, BusinessException으로 처리할 수 있음
            // 여기서는 파싱 실패로 간주하고 BusinessException 발생
            throw new BusinessException(ErrorCode.CSV_PARSE_FAILED, "CSV 파일 헤더 유효성 검증 실패: " + e.getMessage());
        }
        // IOException은 메서드 시그니처에 throws로 선언되어 호출부에서 처리됨
    }
}
