package com.careerpick.controller;

import com.careerpick.dto.*;
import com.careerpick.service.CoverLetterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CoverLetterControllerTest {
    private final CoverLetterService service = mock(CoverLetterService.class);
    private MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new CoverLetterController(service)).setControllerAdvice(new CoverLetterExceptionHandler()).build();
    }
    @Test void createsOrReturnsExistingDraft() throws Exception {
        var detail = new CoverLetterDetailResponse(10, 3, "기업", "공고", Instant.EPOCH, Instant.EPOCH,
            List.of(new CoverLetterItemResponse(100, "motivation", "질문", 1, 700, "답변")));
        when(service.create("Bearer token", 3)).thenReturn(new CoverLetterService.CreationResult(true, detail), new CoverLetterService.CreationResult(false, detail));
        mvc.perform(post("/api/cover-letters").header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"jobId\":3}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/cover-letters/10"))
            .andExpect(jsonPath("$.items[0].answer").value("답변"));
        mvc.perform(post("/api/cover-letters").header("Authorization", "Bearer token").contentType(MediaType.APPLICATION_JSON).content("{\"jobId\":3}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
    }
    @Test void returnsPageAndDefaults() throws Exception {
        when(service.list(null, 0, 20)).thenReturn(new CoverLetterListResponse(List.of(), 0, 20, 0));
        mvc.perform(get("/api/cover-letters")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20));
    }
    @Test void rejectsMalformedBodyAndQuery() throws Exception {
        for (String body : List.of("{}", "{\"jobId\":0}", "{\"jobId\":-1}", "null", "{")) {
            mvc.perform(post("/api/cover-letters").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        mvc.perform(get("/api/cover-letters?page=abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void mapsAuthenticationNotFoundAndInternalErrors() throws Exception {
        when(service.list(null, 0, 20)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
        mvc.perform(get("/api/cover-letters")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        when(service.create(null, 3)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다."));
        mvc.perform(post("/api/cover-letters").contentType(MediaType.APPLICATION_JSON).content("{\"jobId\":3}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
        doThrow(new RuntimeException("비공개 내부 메시지")).when(service).list(null, 0, 20);
        mvc.perform(get("/api/cover-letters")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value("요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }
}
