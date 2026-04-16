package com.jobcupid.job_cupid.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobcupid.job_cupid.shared.exception.BusinessRuleException;
import com.jobcupid.job_cupid.user.dto.UpdateCandidateProfileRequest;
import com.jobcupid.job_cupid.user.entity.CandidateProfile;
import com.jobcupid.job_cupid.user.repository.CandidateProfileRepository;

@ExtendWith(MockitoExtension.class)
class CandidateProfileServiceTest {

    @Mock
    CandidateProfileRepository repository;

    @InjectMocks
    CandidateProfileService service;

    @Test
    void createOrUpdate_createsNewProfile_whenNoneExists() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateCandidateProfileRequest request = new UpdateCandidateProfileRequest();
        request.setHeadline("Backend Engineer");
        request.setSkills(List.of("Java", "Kafka"));

        CandidateProfile result = service.createOrUpdate(userId, request);

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(repository).save(captor.capture());
        CandidateProfile saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getHeadline()).isEqualTo("Backend Engineer");
        assertThat(saved.getSkills()).containsExactly("Java", "Kafka");
        assertThat(result).isNotNull();
    }

    @Test
    void createOrUpdate_updatesExistingProfile_whenAlreadyExists() {
        UUID userId = UUID.randomUUID();
        CandidateProfile existing = CandidateProfile.builder()
                .userId(userId)
                .headline("Old Headline")
                .desiredSalaryMin(3000)
                .build();

        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateCandidateProfileRequest request = new UpdateCandidateProfileRequest();
        request.setHeadline("New Headline");
        request.setDesiredSalaryMax(8000);

        service.createOrUpdate(userId, request);

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(repository).save(captor.capture());
        CandidateProfile saved = captor.getValue();

        assertThat(saved.getHeadline()).isEqualTo("New Headline");       // updated
        assertThat(saved.getDesiredSalaryMin()).isEqualTo(3000);         // preserved
        assertThat(saved.getDesiredSalaryMax()).isEqualTo(8000);         // set
    }

    @Test
    void createOrUpdate_throwsBusinessRuleException_whenSalaryMinExceedsMax() {
        UUID userId = UUID.randomUUID();

        UpdateCandidateProfileRequest request = new UpdateCandidateProfileRequest();
        request.setDesiredSalaryMin(9000);
        request.setDesiredSalaryMax(5000);

        assertThatThrownBy(() -> service.createOrUpdate(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("desiredSalaryMin");
    }

    @Test
    void createOrUpdate_nullFieldsAreIgnored_existingValuesPreserved() {
        UUID userId = UUID.randomUUID();
        CandidateProfile existing = CandidateProfile.builder()
                .userId(userId)
                .headline("Keep Me")
                .preferredRemote(true)
                .build();

        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // request with only salary — other fields null
        UpdateCandidateProfileRequest request = new UpdateCandidateProfileRequest();
        request.setDesiredSalaryMin(4000);
        request.setDesiredSalaryMax(6000);

        service.createOrUpdate(userId, request);

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(repository).save(captor.capture());
        CandidateProfile saved = captor.getValue();

        assertThat(saved.getHeadline()).isEqualTo("Keep Me");       // not overwritten
        assertThat(saved.getPreferredRemote()).isTrue();             // not overwritten
        assertThat(saved.getDesiredSalaryMin()).isEqualTo(4000);    // new value
    }
}
