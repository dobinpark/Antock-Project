package antock.Antock_Project.external.csv;

import java.nio.file.Path;

/**
 * CSV 파일을 다운로드하는 기능을 정의하는 인터페이스
 */
public interface CsvDownloader {

    /**
     * 지정된 조건(예: 지역)에 맞는 CSV 파일을 다운로드하고 저장된 파일 경로를 반환합니다.
     *
     * @param condition 다운로드 조건 (예: 지역 코드, 검색어 등)
     * @return 다운로드된 CSV 파일의 경로
     */
    Path downloadCsvFile(String condition);
}
