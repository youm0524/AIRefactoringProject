package com.careerpick.service;

import com.careerpick.dto.*;
import com.careerpick.entity.CoverLetter;
import com.careerpick.repository.CoverLetterRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CoverLetterService {
    private final CoverLetterRepository letters;
    private final AuthService auth;
    private final JobService jobs;
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate readTransaction;

    public CoverLetterService(CoverLetterRepository letters, AuthService auth, JobService jobs, PlatformTransactionManager manager) {
        this.letters = letters;
        this.auth = auth;
        this.jobs = jobs;
        writeTransaction = new TransactionTemplate(manager);
        writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        readTransaction = new TransactionTemplate(manager);
        readTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        readTransaction.setReadOnly(true);
    }

    public record CreationResult(boolean created, CoverLetterDetailResponse detail) {}

    public CreationResult create(String authorization, long jobId) {
        var user = auth.requireUserEntity(authorization);
        if (jobId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공고 ID를 확인해 주세요.");
        var job = jobs.findById(jobId);
        if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다.");
        try {
            return writeTransaction.execute(status -> letters.findByUserIdAndJobId(user.id(), jobId)
                .map(letter -> new CreationResult(false, CoverLetterDetailResponse.from(letter)))
                .orElseGet(() -> new CreationResult(true, CoverLetterDetailResponse.from(letters.saveAndFlush(new CoverLetter(user, job))))));
        } catch (DataIntegrityViolationException exception) {
            if (!isDuplicateDraft(exception)) throw exception;
            // 실패한 INSERT 트랜잭션이 종료된 후 새 스냅샷에서 기존 초안을 조회한다.
            return readTransaction.execute(status -> letters.findByUserIdAndJobId(user.id(), jobId)
                .map(letter -> new CreationResult(false, CoverLetterDetailResponse.from(letter))).orElseThrow(() -> exception));
        }
    }

    public CoverLetterListResponse list(String authorization, int page, int size) {
        var user = auth.requireUserEntity(authorization);
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "페이지와 조회 개수를 확인해 주세요.");
        return readTransaction.execute(status -> {
            var result = letters.findByUserIdOrderByUpdatedAtDescIdDesc(user.id(), PageRequest.of(page, size));
            return new CoverLetterListResponse(result.stream().map(CoverLetterSummaryResponse::from).toList(), page, size, result.getTotalElements());
        });
    }

    private boolean isDuplicateDraft(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null
                && violation.getConstraintName().contains("uk_cover_letters_user_job")) return true;
        }
        return false;
    }
}
