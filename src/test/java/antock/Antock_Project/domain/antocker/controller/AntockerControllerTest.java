package antock.Antock_Project.domain.antocker.controller;

import antock.Antock_Project.domain.antocker.service.AntockerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest: 웹 레이어(컨트롤러) 관련 빈만 로드하여 테스트 (더 가벼움)
// AntockerController만 테스트하므로 @SpringBootTest 대신 사용 가능
@WebMvcTest(AntockerController.class)
class AntockerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AntockerService antockerService;

    @Test
    @DisplayName("POST /api/antockers/process 요청 시 서비스 호출 및 202 응답 확인")
    void startProcessing_Success() throws Exception {
        // given
        String region = "서울특별시";
        // Mock AntockerService 설정: processAndSaveAntockerData 호출 시 성공적으로 완료된 CompletableFuture 반환
        when(antockerService.processAndSaveAntockerData(anyString()))
                .thenReturn(CompletableFuture.completedFuture(10)); // 예시로 10개 저장 완료

        // when & then
        mockMvc.perform(post("/api/antockers/process") // POST 요청 시뮬레이션
                        .param("region", region) // 요청 파라미터 설정
                        .contentType(MediaType.APPLICATION_JSON)) // 요청 타입 (필수는 아님)
                .andExpect(status().isAccepted()) // HTTP 상태 코드 202 Accepted 확인
                .andExpect(content().string("Processing started for region: " + region)); // 응답 본문 확인

        // AntockerService의 processAndSaveAntockerData 메소드가 "서울특별시" 인수로 호출되었는지 검증
        verify(antockerService).processAndSaveAntockerData(region);
    }
}
