package com.careerpick.dto;
import com.careerpick.entity.CoverLetterItem;
public record CoverLetterItemResponse(long id, String questionKey, String questionText, int displayOrder, int maxLength, String answer) {
    public static CoverLetterItemResponse from(CoverLetterItem item) {
        return new CoverLetterItemResponse(item.id(), item.questionKey(), item.questionText(), item.displayOrder(), item.maxLength(), item.answer());
    }
}
