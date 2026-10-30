package com.careerpick.service;

import com.careerpick.entity.*;
import com.careerpick.repository.CoverLetterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoverLetterServiceTest {
    private final CoverLetterRepository letters = mock(CoverLetterRepository.class);
    private final AuthService auth = mock(AuthService.class);
    private final JobService jobs = mock(JobService.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final CoverLetterService service = new CoverLetterService(letters, auth, jobs, manager);
    private final User user = new User("사용자", "test@test.kr", "hash");
    private final Job job = new Job("기업", "C", "red", "공고", "개발", "신입", "서울", "D-1", List.of());
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(job, "id", 2L);
        when(auth.requireUserEntity("token")).thenReturn(user);
        when(jobs.findById(2)).thenReturn(job);
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
    }
    @Test void returnsExistingWithoutChangingContentOrTime() {
        CoverLetter letter = new CoverLetter(user, job);
        ReflectionTestUtils.setField(letter, "id", 3L);
        for (int i=0;i<2;i++) ReflectionTestUtils.setField(letter.items().get(i), "id", (long)i+10);
        letter.updateAnswers(Map.of(10L,"기존 답변",11L,""));
        var updated = letter.updatedAt();
        when(letters.findByUserIdAndJobId(1,2)).thenReturn(Optional.of(letter));
        var result = service.create("token",2);
        assertThat(result.created()).isFalse();
        assertThat(result.detail().updatedAt()).isEqualTo(updated);
        assertThat(result.detail().items().get(0).answer()).isEqualTo("기존 답변");
        verify(letters, never()).saveAndFlush(any());
    }
    @Test void rejectsMissingJobAndInvalidPages() {
        assertThatThrownBy(() -> service.create("token", 99)).isInstanceOf(ResponseStatusException.class);
        for (int[] values : new int[][] {{-1,20},{0,0},{0,101}}) {
            assertThatThrownBy(() -> service.list("token",values[0],values[1])).isInstanceOf(ResponseStatusException.class);
        }
        verifyNoInteractions(letters);
    }
    @Test void requiresAuthenticationBeforeRepositoryAccess() {
        when(auth.requireUserEntity(null)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.create(null,2)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.list(null,0,20)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(letters, jobs);
    }
    @Test void recoversOnlyDraftUniquenessFailureInFreshTransaction() {
        CoverLetter letter = new CoverLetter(user, job);
        ReflectionTestUtils.setField(letter, "id", 3L);
        for (int i=0;i<2;i++) ReflectionTestUtils.setField(letter.items().get(i), "id", (long)i+10);
        var duplicate = new org.springframework.dao.DataIntegrityViolationException("duplicate",
            new org.hibernate.exception.ConstraintViolationException("duplicate", new java.sql.SQLException("duplicate", "23000", 1062), "uk_cover_letters_user_job"));
        when(letters.findByUserIdAndJobId(1,2)).thenReturn(Optional.empty(), Optional.of(letter));
        when(letters.saveAndFlush(any())).thenThrow(duplicate);
        assertThat(service.create("token",2).created()).isFalse();
        var order = inOrder(manager, letters);
        order.verify(manager).getTransaction(any());
        order.verify(letters).findByUserIdAndJobId(1,2);
        order.verify(letters).saveAndFlush(any());
        order.verify(manager).rollback(any());
        order.verify(manager).getTransaction(any());
        order.verify(letters).findByUserIdAndJobId(1,2);
        order.verify(manager).commit(any());
    }
    @Test void doesNotHideOtherDatabaseFailures() {
        var failure = new org.springframework.dao.DataIntegrityViolationException("unrelated");
        when(letters.findByUserIdAndJobId(1,2)).thenReturn(Optional.empty());
        when(letters.saveAndFlush(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.create("token",2)).isSameAs(failure);
        verify(letters, times(1)).findByUserIdAndJobId(1,2);
    }
}
