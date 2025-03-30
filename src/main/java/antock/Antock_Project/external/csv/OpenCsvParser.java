package antock.Antock_Project.external.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OpenCsvParser implements CsvParser {

    @Override
    public List<Map<String, String>> parseCsvFile(File csvFile) {
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            List<String[]> csvData = reader.readAll();
            if (csvData == null || csvData.isEmpty()) {
                log.warn("CSV 파일 파싱 실패: 데이터 없음");
                return List.of(); // 빈 리스트 반환
            }

            String[] headers = csvData.get(0); // 첫 번째 행을 헤더로 사용
            List<String[]> dataRows = csvData.subList(1, csvData.size()); // 데이터 행 추출

            return dataRows.stream()
                    .map(row -> {
                        Map<String, String> rowMap = new java.util.HashMap<>();
                        for (int i = 0; i < headers.length; i++) {
                            rowMap.put(headers[i].trim(), row[i].trim()); // 헤더와 데이터 매핑, 공백 제거
                        }
                        return rowMap;
                    })
                    .collect(Collectors.toList());

        } catch (IOException | CsvException e) {
            log.error("CSV 파일 파싱 중 오류 발생", e);
            return List.of(); // 빈 리스트 반환 또는 예외 처리
        }
    }
}
