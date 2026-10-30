package com.careerpick.repository;

import com.careerpick.entity.CoverLetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {
    @EntityGraph(attributePaths = {"job", "items"})
    Optional<CoverLetter> findByUserIdAndJobId(long userId, long jobId);

    @EntityGraph(attributePaths = {"job", "items"})
    Optional<CoverLetter> findByIdAndUserId(long id, long userId);

    @EntityGraph(attributePaths = "job")
    Page<CoverLetter> findByUserIdOrderByUpdatedAtDescIdDesc(long userId, Pageable pageable);
}
