package com.jobcupid.job_cupid.swipe.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.jobcupid.job_cupid.swipe.entity.CandidateSwipe;
import com.jobcupid.job_cupid.swipe.entity.SwipeAction;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CandidateSwipeRepositoryTest {

    @Autowired CandidateSwipeRepository candidateSwipeRepository;

    private CandidateSwipe buildSwipe(UUID candidateId, UUID jobId, SwipeAction action) {
        return CandidateSwipe.builder()
                .candidateId(candidateId)
                .jobId(jobId)
                .action(action)
                .swipedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void existsByCandidateIdAndJobIdAndAction_returnsTrue_whenLikeSaved() {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        candidateSwipeRepository.save(buildSwipe(candidateId, jobId, SwipeAction.LIKE));

        boolean result = candidateSwipeRepository
                .existsByCandidateIdAndJobIdAndAction(candidateId, jobId, SwipeAction.LIKE);

        assertThat(result).isTrue();
    }

    @Test
    void existsByCandidateIdAndJobIdAndAction_returnsFalse_whenPassSaved() {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        candidateSwipeRepository.save(buildSwipe(candidateId, jobId, SwipeAction.PASS));

        boolean result = candidateSwipeRepository
                .existsByCandidateIdAndJobIdAndAction(candidateId, jobId, SwipeAction.LIKE);

        assertThat(result).isFalse();
    }

    @Test
    void save_throwsDataIntegrityViolationException_whenDuplicateCandidateAndJob() {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        candidateSwipeRepository.saveAndFlush(buildSwipe(candidateId, jobId, SwipeAction.LIKE));

        assertThatThrownBy(() ->
                candidateSwipeRepository.saveAndFlush(buildSwipe(candidateId, jobId, SwipeAction.PASS)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
