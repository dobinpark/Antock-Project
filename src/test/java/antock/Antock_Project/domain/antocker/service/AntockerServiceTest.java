package antock.Antock_Project.domain.antocker.service;

import antock.Antock_Project.domain.antocker.repository.AntockerRepository;
import antock.Antock_Project.external.csv.CsvDownloader;
import antock.Antock_Project.external.csv.CsvParser;
import antock.Antock_Project.infrastructure.storage.AntockerStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AntockerServiceTest {

    @Mock
    private AntockerRepository antockerRepository;

    @Mock
    private AntockerStorageService antockerStorageService;

    @Mock
    private CsvDownloader csvDownloader;

    @Mock
    private CsvParser csvParser;

    @Mock
    private AntockerDataProcessor antockerDataProcessor;

    @InjectMocks
    private AntockerService antockerService;

    @Test
    void processAntockers_ValidCityDistrict_Success() {
        // Given
        String city = "서울";
        String district = "강남구";
        File mockCsvFile = mock(File.class);
        
        List<Map<String, String>> mockParsedCsvData = List.of(
                Map.of("사업자등록번호", "1234567880", "상호", "테스트법인1", "사업장주소", "서울 강남구"),
                Map.of("사업자등록번호", "9876543280", "상호", "테스트법인2", "사업장주소", "서울 강남구")
        );
        
        when(csvDownloader.downloadCsvFile(city, district)).thenReturn(mockCsvFile);
        when(csvParser.parseCsvFile(mockCsvFile)).thenReturn(mockParsedCsvData);
        
        when(antockerDataProcessor.processAntockerData(anyMap())).thenReturn(CompletableFuture.completedFuture(null));

        // When
        CompletableFuture<Void> future = antockerService.processAntockers(city, district);

        // Then
        assertDoesNotThrow(() -> future.join());
        verify(csvDownloader, times(1)).downloadCsvFile(city, district);
        verify(csvParser, times(1)).parseCsvFile(mockCsvFile);
        verify(antockerDataProcessor, times(mockParsedCsvData.size())).processAntockerData(anyMap());
        verify(antockerStorageService, times(1)).saveAll(anyList());
    }

    @Test
    void processAntockers_CsvDownloadFail_NoDataProcessed() {
        // Given
        String city = "서울";
        String district = "강남구";
        when(csvDownloader.downloadCsvFile(city, district)).thenReturn(null);

        // When
        CompletableFuture<Void> future = antockerService.processAntockers(city, district);

        // Then
        assertDoesNotThrow(() -> future.join());
        verify(csvParser, never()).parseCsvFile(any());
        verify(antockerDataProcessor, never()).processAntockerData(anyMap());
        verify(antockerStorageService, never()).saveAll(anyList());
    }

    @Test
    void processAntockers_CsvParseFail_NoDataProcessed() {
        // Given
        String city = "서울";
        String district = "강남구";
        File mockCsvFile = mock(File.class);
        when(csvDownloader.downloadCsvFile(city, district)).thenReturn(mockCsvFile);
        when(csvParser.parseCsvFile(mockCsvFile)).thenReturn(Collections.emptyList());

        // When
        CompletableFuture<Void> future = antockerService.processAntockers(city, district);

        // Then
        assertDoesNotThrow(() -> future.join());
        verify(antockerDataProcessor, never()).processAntockerData(anyMap());
        verify(antockerStorageService, never()).saveAll(anyList());
    }
}
