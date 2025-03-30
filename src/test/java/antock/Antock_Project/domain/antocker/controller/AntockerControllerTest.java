package antock.Antock_Project.domain.antocker.controller;

import antock.Antock_Project.domain.antocker.dto.AntockerRequest;
import antock.Antock_Project.domain.antocker.service.AntockerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class AntockerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AntockerService antockerService;

    @Test
    void processAntockers_ValidRequest_ReturnsOk() throws Exception {
        // Given
        AntockerRequest request = new AntockerRequest();
        request.setCity("서울");
        request.setDistrict("강남구");

        when(antockerService.processAntockers(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When & Then
        ResultActions resultActions = mockMvc.perform(post("/api/antockers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("통신 판매자 정보 처리 요청이 접수되었습니다."))
                .andDo(print());
        verify(antockerService, times(1)).processAntockers(request.getCity(), request.getDistrict());
    }

    @Test
    void processAntockers_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        AntockerRequest invalidRequest = new AntockerRequest();

        // When & Then
        mockMvc.perform(post("/api/antockers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
        verify(antockerService, never()).processAntockers(anyString(), anyString());
    }
}
