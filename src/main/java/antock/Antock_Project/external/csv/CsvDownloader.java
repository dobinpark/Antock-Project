package antock.Antock_Project.external.csv;

import java.io.File;

public interface CsvDownloader {
    File downloadCsvFile(String city, String district);
}
