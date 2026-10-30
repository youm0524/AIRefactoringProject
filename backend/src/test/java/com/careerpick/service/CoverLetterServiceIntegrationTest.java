package com.careerpick.service;

import com.careerpick.entity.*;
import com.careerpick.repository.CoverLetterRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@Import({CoverLetterService.class, JobService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CoverLetterServiceIntegrationTest {
    @Autowired private CoverLetterService service;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager manager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CoverLetterRepository letters;
    @MockitoBean private AuthService auth;

    @Test void concurrentCreationAndIndependentUserPages() throws Exception {
        var tx = new TransactionTemplate(manager);
        User owner = new User("작성자", UUID.randomUUID()+"@test.kr", "hash");
        User other = new User("다른 사용자", UUID.randomUUID()+"@test.kr", "hash");
        Job first = job("첫 공고"), second = job("두 번째 공고");
        tx.executeWithoutResult(status -> {
            entityManager.persist(owner); entityManager.persist(other);
            entityManager.persist(first); entityManager.persist(second);
        });
        when(auth.requireUserEntity("owner")).thenReturn(owner);
        when(auth.requireUserEntity("other")).thenReturn(other);
        var executor = Executors.newFixedThreadPool(4);
        try {
            var ready = new CountDownLatch(4);
            var start = new CountDownLatch(1);
            List<Future<CoverLetterService.CreationResult>> futures = new ArrayList<>();
            for (int i=0;i<4;i++) futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("시작 대기 초과");
                return service.create("owner",first.id());
            }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = new ArrayList<CoverLetterService.CreationResult>();
            for (var future : futures) results.add(future.get(30,TimeUnit.SECONDS));
            assertThat(results.stream().filter(CoverLetterService.CreationResult::created).count()).isEqualTo(1);
            long id = results.get(0).detail().id();
            assertThat(results).allSatisfy(result -> assertThat(result.detail().id()).isEqualTo(id));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cover_letter_items WHERE cover_letter_id=?",Integer.class,id)).isEqualTo(2);
            assertThat(service.list("other",0,20).items()).isEmpty();
            var another = service.create("owner", second.id());
            var otherDraft = service.create("other",first.id());
            assertThat(otherDraft.detail().id()).isNotEqualTo(id);
            var page = service.list("owner",0,1);
            assertThat(page.totalElements()).isEqualTo(2);
            assertThat(page.items()).extracting(item -> item.id()).containsExactly(another.detail().id());
            assertThat(service.list("owner",1,1).items()).extracting(item -> item.id()).containsExactly(id);
            assertThat(service.list("owner",2,1).items()).isEmpty();
            assertThat(service.list("other",0,20).totalElements()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10,TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                jdbc.update("DELETE FROM cover_letters WHERE user_id IN (?,?)",owner.id(),other.id());
                jdbc.update("DELETE FROM jobs WHERE id IN (?,?)",first.id(),second.id());
                jdbc.update("DELETE FROM users WHERE id IN (?,?)",owner.id(),other.id());
            });
        }
    }
    @Test void readsUpdatesRejectsInvalidAccessAndDeletesWithRecreation() {
        var tx = new TransactionTemplate(manager);
        User owner = new User("작성자", UUID.randomUUID()+"@test.kr", "hash");
        User other = new User("타인", UUID.randomUUID()+"@test.kr", "hash");
        Job job = job("상세 검증 공고");
        tx.executeWithoutResult(status -> { entityManager.persist(owner); entityManager.persist(other); entityManager.persist(job); });
        when(auth.requireUserEntity("owner")).thenReturn(owner);
        when(auth.requireUserEntity("other")).thenReturn(other);
        when(auth.requireUserEntity(null)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
        try {
            var initial = service.create("owner", job.id()).detail();
            long id = initial.id(), first = initial.items().get(0).id(), second = initial.items().get(1).id();
            assertThat(service.detail("owner", id)).isEqualTo(initial);
            var valid = request(first, "  답변\n", second, "😀".repeat(350));
            var updated = service.update("owner",id,valid);
            assertThat(updated.updatedAt()).isAfter(initial.updatedAt());
            assertThat(service.detail("owner",id)).isEqualTo(updated);
            assertThat(updated.items().get(0).answer()).isEqualTo("  답변\n");
            var foreign = service.create("other",job.id()).detail();
            var invalid = List.of(
                request(first,"변경",second,"가".repeat(701)),
                request(first,"변경",second,"😀".repeat(351)),
                request(first,"변경",second,null),
                request(first,"변경",first,"중복"),
                request(first,"변경",foreign.items().get(0).id(),"타 초안"),
                new com.careerpick.dto.UpdateCoverLetterRequest(List.of(new com.careerpick.dto.UpdateCoverLetterRequest.Item(first,"누락"))),
                new com.careerpick.dto.UpdateCoverLetterRequest(List.of()));
            for (var body : invalid) {
                assertStatus(400, () -> service.update("owner",id,body));
                assertThat(service.detail("owner",id)).isEqualTo(updated);
            }
            assertStatus(404, () -> service.detail("other",id));
            assertStatus(404, () -> service.update("other",id,valid));
            assertStatus(404, () -> service.delete("other",id));
            assertStatus(401, () -> service.detail(null,id));
            assertStatus(401, () -> service.update(null,id,valid));
            assertStatus(401, () -> service.delete(null,id));
            assertStatus(400, () -> service.detail("owner",0));
            assertStatus(400, () -> service.update("owner",-1,valid));
            assertStatus(400, () -> service.delete("owner",0));
            assertThat(service.detail("owner",id)).isEqualTo(updated);
            var pool = Executors.newFixedThreadPool(2);
            try {
                var start = new CountDownLatch(1);
                var one = pool.submit(() -> { start.await(); return service.update("owner",id,request(first,"요청 A",second,"😀".repeat(350))); });
                var two = pool.submit(() -> { start.await(); return service.update("owner",id,request(first,"  답변\n",second,"요청 B")); });
                start.countDown();
                var a = one.get(10,TimeUnit.SECONDS);
                var b = two.get(10,TimeUnit.SECONDS);
                var finalItems = service.detail("owner",id).items();
                assertThat(finalItems).isIn(a.items(),b.items());
            } catch (Exception exception) {
                throw new AssertionError(exception);
            } finally {
                pool.shutdownNow();
            }
            var empty = service.update("owner",id,request(first,"",second,""));
            assertThat(empty.items()).allSatisfy(item -> assertThat(item.answer()).isEmpty());
            service.delete("owner",id);
            assertStatus(404, () -> service.detail("owner",id));
            assertStatus(404, () -> service.update("owner",id,valid));
            assertStatus(404, () -> service.delete("owner",id));
            assertThat(service.list("owner",0,20).items()).isEmpty();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cover_letter_items WHERE cover_letter_id=?",Integer.class,id)).isZero();
            var recreated = service.create("owner",job.id()).detail();
            assertThat(recreated.id()).isNotEqualTo(id);
            assertThat(recreated.items()).hasSize(2).allSatisfy(item -> assertThat(item.answer()).isEmpty());
        } finally {
            tx.executeWithoutResult(status -> {
                jdbc.update("DELETE FROM cover_letters WHERE user_id IN (?,?)",owner.id(),other.id());
                jdbc.update("DELETE FROM jobs WHERE id=?",job.id());
                jdbc.update("DELETE FROM users WHERE id IN (?,?)",owner.id(),other.id());
            });
        }
    }
    private com.careerpick.dto.UpdateCoverLetterRequest request(long first, String firstAnswer, long second, String secondAnswer) {
        return new com.careerpick.dto.UpdateCoverLetterRequest(List.of(
            new com.careerpick.dto.UpdateCoverLetterRequest.Item(first,firstAnswer),
            new com.careerpick.dto.UpdateCoverLetterRequest.Item(second,secondAnswer)));
    }
    private void assertStatus(int expected, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode().value()).isEqualTo(expected));
    }
    private Job job(String title) {
        return new Job("기업", "C", "red", title, "개발", "신입", "서울", "D-1", List.of());
    }
}
