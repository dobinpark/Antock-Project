package antock.Antock_Project.external.csv;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * CSV 파일을 파싱하는 기능을 정의하는 인터페이스
 */
public interface CsvParser {

    /**
     * 지정된 경로의 CSV 파일을 파싱하여 데이터를 Map 리스트 형태로 반환합니다.
     * Map의 Key는 CSV 헤더가 됩니다.
     *
     * @param filePath 파싱할 CSV 파일의 경로
     * @return 파싱된 데이터 리스트 (List of Maps)
     */
    List<Map<String, String>> parseCsvFile(Path filePath);
}
