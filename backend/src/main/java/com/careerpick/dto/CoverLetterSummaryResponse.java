package com.careerpick.dto;
import com.careerpick.entity.CoverLetter;
import java.time.Instant;
public record CoverLetterSummaryResponse(long id, long jobId, String company, String jobTitle, Instant updatedAt) {
    public static CoverLetterSummaryResponse from(CoverLetter letter) {
        return new CoverLetterSummaryResponse(letter.id(), letter.job().id(), letter.job().company(), letter.job().title(), letter.updatedAt());
    }
}
