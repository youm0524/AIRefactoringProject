package com.careerpick.repository;

import com.careerpick.entity.CoverLetter;
import com.careerpick.entity.CoverLetterItem;
import com.careerpick.entity.Job;
import com.careerpick.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class CoverLetterRepositoryTest {
    @Autowired private CoverLetterRepository letters;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    private User user;
    private User otherUser;
    private Job job;
    private Job otherJob;

    @BeforeEach void setUp() {
        user = new User("사용자", "owner@test.kr", "hash");
        otherUser = new User("다른 사용자", "other@test.kr", "hash");
        job = job("첫 공고");
        otherJob = job("다른 공고");
        entityManager.persist(user);
        entityManager.persist(otherUser);
        entityManager.persist(job);
        entityManager.persist(otherJob);
        entityManager.flush();
    }

    @Test void savesQuestionsAnswersAndTimesWithoutUpdatingOnRead() {
        CoverLetter letter = letters.saveAndFlush(new CoverLetter(user, job));
        Instant created = letter.createdAt();
        Instant initialUpdated = letter.updatedAt();
        Long id = letter.id();
        entityManager.clear();
        letter = letters.findByIdAndUserId(id, user.id()).orElseThrow();
        assertThat(letter.createdAt()).isEqualTo(created);
        assertThat(letter.updatedAt()).isEqualTo(initialUpdated);
        assertThat(letter.items()).extracting(CoverLetterItem::questionKey).containsExactly("motivation", "experience");
        assertThat(letter.items()).extracting(CoverLetterItem::displayOrder).containsExactly(1, 2);
        assertThat(letter.items()).allSatisfy(item -> {
            assertThat(item.answer()).isEmpty();
            assertThat(item.maxLength()).isEqualTo(700);
            assertThat(item.questionText()).isNotBlank();
        });
        letter.updateAnswers(Map.of(letter.items().get(0).id(), "  지원 동기\n", letter.items().get(1).id(), ""));
        letters.flush();
        entityManager.clear();
        CoverLetter loaded = letters.findByUserIdAndJobId(user.id(), job.id()).orElseThrow();
        assertThat(loaded.items()).extracting(CoverLetterItem::answer).containsExactly("  지원 동기\n", "");
        assertThat(loaded.updatedAt()).isAfter(initialUpdated);
        assertThat(loaded.createdAt()).isEqualTo(created);
    }

    @Test void validatesAllAnswersBeforeChangingAnyItem() {
        CoverLetter letter = letters.saveAndFlush(new CoverLetter(user, job));
        Long first = letter.items().get(0).id(), second = letter.items().get(1).id();
        Instant updated = letter.updatedAt();
        assertThatThrownBy(() -> letter.updateAnswers(Map.of(first, "변경", second, "가".repeat(701))))
            .isInstanceOf(IllegalArgumentException.class);
        Map<Long, String> nullAnswer = new HashMap<>();
        nullAnswer.put(first, "변경");
        nullAnswer.put(second, null);
        assertThatThrownBy(() -> letter.updateAnswers(nullAnswer)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> letter.updateAnswers(Map.of(first, "누락"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> letter.updateAnswers(Map.of(first, "", Long.MAX_VALUE, "")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> letter.updateAnswers(Map.of(first, "", second, "😀".repeat(351))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(letter.items()).allSatisfy(item -> assertThat(item.answer()).isEmpty());
        assertThat(letter.updatedAt()).isEqualTo(updated);
        letter.updateAnswers(Map.of(first, "😀".repeat(350), second, ""));
        letters.flush();
        entityManager.clear();
        assertThat(letters.findByIdAndUserId(letter.id(), user.id()).orElseThrow().items().get(0).answer())
            .isEqualTo("😀".repeat(350));
    }

    @Test void scopesAndPaginatesWithStableNewestFirstOrder() {
        CoverLetter first = letters.saveAndFlush(new CoverLetter(user, job));
        CoverLetter second = letters.saveAndFlush(new CoverLetter(user, otherJob));
        letters.saveAndFlush(new CoverLetter(otherUser, job));
        Timestamp sameTime = Timestamp.from(Instant.parse("2026-10-30T01:00:00Z"));
        jdbc.update("UPDATE cover_letters SET updated_at = ? WHERE user_id = ?", sameTime, user.id());
        entityManager.clear();
        var page = letters.findByUserIdOrderByUpdatedAtDescIdDesc(user.id(), PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(CoverLetter::id).containsExactly(second.id());
        assertThat(page.getContent().get(0).job().title()).isEqualTo("다른 공고");
        assertThat(letters.findByUserIdOrderByUpdatedAtDescIdDesc(user.id(), PageRequest.of(1, 1)).getContent())
            .extracting(CoverLetter::id).containsExactly(first.id());
        jdbc.update("UPDATE cover_letters SET updated_at = ? WHERE id = ?", Timestamp.from(Instant.parse("2026-10-31T01:00:00Z")), first.id());
        entityManager.clear();
        assertThat(letters.findByUserIdOrderByUpdatedAtDescIdDesc(user.id(), PageRequest.of(0, 1)).getContent())
            .extracting(CoverLetter::id).containsExactly(first.id());
        assertThat(letters.findByIdAndUserId(first.id(), otherUser.id())).isEmpty();
        assertThat(letters.findByUserIdAndJobId(otherUser.id(), otherJob.id())).isEmpty();
    }

    @Test void rejectsDuplicateUserAndJob() {
        letters.saveAndFlush(new CoverLetter(user, job));
        assertThatThrownBy(() -> letters.saveAndFlush(new CoverLetter(user, job)))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void enforcesItemUniquenessChecksAndForeignKeys() {
        CoverLetter letter = letters.saveAndFlush(new CoverLetter(user, job));
        assertThatThrownBy(() -> insertItem(letter.id(), "motivation", 3, 700)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertItem(letter.id(), "new", 1, 700)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertItem(letter.id(), "new", 0, 700)).hasRootCauseMessage("Check constraint 'ck_cover_letter_items_order' is violated.");
        assertThatThrownBy(() -> insertItem(letter.id(), "new", 3, 0)).hasRootCauseMessage("Check constraint 'ck_cover_letter_items_max_length' is violated.");
        assertThatThrownBy(() -> insertItem(Long.MAX_VALUE, "new", 3, 700)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertLetter(Long.MAX_VALUE, otherJob.id())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertLetter(otherUser.id(), Long.MAX_VALUE)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void jpaDeleteRemovesItemsPreservesReferencesAndAllowsRecreation() {
        CoverLetter letter = letters.saveAndFlush(new CoverLetter(user, job));
        Long oldId = letter.id();
        letters.delete(letter);
        letters.flush();
        entityManager.clear();
        assertThat(itemCount(oldId)).isZero();
        assertThat(entityManager.find(User.class, user.id())).isNotNull();
        assertThat(entityManager.find(Job.class, job.id())).isNotNull();
        CoverLetter replacement = letters.saveAndFlush(new CoverLetter(entityManager.find(User.class, user.id()), entityManager.find(Job.class, job.id())));
        assertThat(replacement.id()).isNotEqualTo(oldId);
        assertThat(itemCount(replacement.id())).isEqualTo(2);
    }

    @Test void directSqlDeleteCascadesItemsAndRestrictsUserAndJobDeletion() {
        CoverLetter letter = letters.saveAndFlush(new CoverLetter(user, job));
        assertThatThrownBy(() -> jdbc.update("DELETE FROM users WHERE id = ?", user.id())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM jobs WHERE id = ?", job.id())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        jdbc.update("DELETE FROM cover_letters WHERE id = ?", letter.id());
        entityManager.clear();
        assertThat(itemCount(letter.id())).isZero();
        assertThat(letters.findById(letter.id())).isEmpty();
    }

    private int itemCount(long letterId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM cover_letter_items WHERE cover_letter_id = ?", Integer.class, letterId);
    }

    private void insertItem(long letterId, String key, int order, int maxLength) {
        jdbc.update("INSERT INTO cover_letter_items (cover_letter_id, question_key, question_text, display_order, max_length, answer) VALUES (?, ?, '질문', ?, ?, '')", letterId, key, order, maxLength);
    }

    private void insertLetter(long userId, long jobId) {
        jdbc.update("INSERT INTO cover_letters (user_id, job_id, created_at, updated_at) VALUES (?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))", userId, jobId);
    }

    private Job job(String title) {
        return new Job("기업", "C", "#6556df", title, "개발", "신입", "서울", "D-3", List.of());
    }
}
