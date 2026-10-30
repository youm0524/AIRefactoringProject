package com.careerpick.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cover_letter_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cover_letter_items_key", columnNames = {"cover_letter_id", "question_key"}),
    @UniqueConstraint(name = "uk_cover_letter_items_order", columnNames = {"cover_letter_id", "display_order"})
})
public class CoverLetterItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cover_letter_id", nullable = false)
    private CoverLetter coverLetter;
    @Column(name = "question_key", nullable = false, length = 50)
    private String questionKey;
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(name = "max_length", nullable = false)
    private int maxLength;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    protected CoverLetterItem() {}

    CoverLetterItem(CoverLetter coverLetter, String questionKey, String questionText, int displayOrder, int maxLength) {
        if (displayOrder <= 0 || maxLength <= 0) throw new IllegalArgumentException("문항 순서와 글자 제한은 양수여야 합니다.");
        this.coverLetter = coverLetter;
        this.questionKey = questionKey;
        this.questionText = questionText;
        this.displayOrder = displayOrder;
        this.maxLength = maxLength;
        this.answer = "";
    }

    void validateAnswer(String answer) {
        if (answer == null || answer.length() > maxLength) throw new IllegalArgumentException("답변의 글자 제한을 확인해 주세요.");
    }

    void updateAnswer(String answer) { this.answer = answer; }
    public Long id() { return id; }
    public String questionKey() { return questionKey; }
    public String questionText() { return questionText; }
    public int displayOrder() { return displayOrder; }
    public int maxLength() { return maxLength; }
    public String answer() { return answer; }
}
