package com.careerpick.dto;
import java.util.List;
public record CoverLetterListResponse(List<CoverLetterSummaryResponse> items, int page, int size, long totalElements) {}
