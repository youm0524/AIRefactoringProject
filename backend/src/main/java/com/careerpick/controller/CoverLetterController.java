package com.careerpick.controller;

import com.careerpick.dto.*;
import com.careerpick.service.CoverLetterService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/cover-letters")
@CrossOrigin(origins = "http://localhost:5173")
@SecurityRequirement(name = "bearerAuth")
public class CoverLetterController {
    private final CoverLetterService service;
    public CoverLetterController(CoverLetterService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<CoverLetterDetailResponse> create(@RequestHeader(name = "Authorization", required = false) String authorization,
                                                           @Valid @RequestBody CreateCoverLetterRequest request) {
        var result = service.create(authorization, request.jobId());
        return result.created() ? ResponseEntity.created(URI.create("/api/cover-letters/" + result.detail().id())).body(result.detail())
            : ResponseEntity.ok(result.detail());
    }

    @GetMapping
    public CoverLetterListResponse list(@RequestHeader(name = "Authorization", required = false) String authorization,
                                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(authorization, page, size);
    }
    @GetMapping("/{coverLetterId}")
    public CoverLetterDetailResponse detail(@RequestHeader(name = "Authorization", required = false) String authorization,
                                            @PathVariable long coverLetterId) {
        return service.detail(authorization, coverLetterId);
    }

    @PutMapping("/{coverLetterId}")
    public CoverLetterDetailResponse update(@RequestHeader(name = "Authorization", required = false) String authorization,
                                            @PathVariable long coverLetterId, @Valid @RequestBody UpdateCoverLetterRequest request) {
        return service.update(authorization, coverLetterId, request);
    }

    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "초안 삭제 완료")
    @DeleteMapping("/{coverLetterId}")
    public ResponseEntity<Void> delete(@RequestHeader(name = "Authorization", required = false) String authorization,
                                       @PathVariable long coverLetterId) {
        service.delete(authorization, coverLetterId);
        return ResponseEntity.noContent().build();
    }
}
