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
import org.springframework.data.domain.PageRequest;
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
    private Job job(String title) {
        return new Job("기업", "C", "red", title, "개발", "신입", "서울", "D-1", List.of());
    }
}
