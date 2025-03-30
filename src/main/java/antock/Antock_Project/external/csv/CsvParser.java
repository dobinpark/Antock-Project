package antock.Antock_Project.external.csv;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface CsvParser {
    List<Map<String, String>> parseCsvFile(File csvFile);
}
