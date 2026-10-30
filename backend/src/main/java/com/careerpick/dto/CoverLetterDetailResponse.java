package com.careerpick.dto;
import com.careerpick.entity.CoverLetter;
import java.time.Instant;
import java.util.List;
public record CoverLetterDetailResponse(long id, long jobId, String company, String jobTitle, Instant createdAt, Instant updatedAt, List<CoverLetterItemResponse> items) {
    public static CoverLetterDetailResponse from(CoverLetter letter) {
        return new CoverLetterDetailResponse(letter.id(), letter.job().id(), letter.job().company(), letter.job().title(), letter.createdAt(), letter.updatedAt(), letter.items().stream().map(CoverLetterItemResponse::from).toList());
    }
}
