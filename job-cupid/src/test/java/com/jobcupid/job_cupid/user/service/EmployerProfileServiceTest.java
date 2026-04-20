package com.jobcupid.job_cupid.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobcupid.job_cupid.shared.exception.ResourceNotFoundException;
import com.jobcupid.job_cupid.user.dto.UpdateEmployerProfileRequest;
import com.jobcupid.job_cupid.user.entity.EmployerProfile;
import com.jobcupid.job_cupid.user.repository.EmployerProfileRepository;

@ExtendWith(MockitoExtension.class)
class EmployerProfileServiceTest {

    @Mock
    EmployerProfileRepository repository;

    @InjectMocks
    EmployerProfileService service;

    @Test
    void createOrUpdate_createsNewProfile_whenNoneExists() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateEmployerProfileRequest request = new UpdateEmployerProfileRequest();
        request.setCompanyName("Acme Corp");
        request.setIndustry("Technology");

        service.createOrUpdate(userId, request);

        ArgumentCaptor<EmployerProfile> captor = ArgumentCaptor.forClass(EmployerProfile.class);
        verify(repository).save(captor.capture());
        EmployerProfile saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getCompanyName()).isEqualTo("Acme Corp");
        assertThat(saved.getIndustry()).isEqualTo("Technology");
    }

    @Test
    void createOrUpdate_updatesExistingProfile_whenAlreadyExists() {
        UUID userId = UUID.randomUUID();
        EmployerProfile existing = EmployerProfile.builder()
                .userId(userId)
                .companyName("Old Corp")
                .companySize("1-10")
                .build();

        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateEmployerProfileRequest request = new UpdateEmployerProfileRequest();
        request.setCompanyName("New Corp");
        request.setIndustry("Fintech");

        service.createOrUpdate(userId, request);

        ArgumentCaptor<EmployerProfile> captor = ArgumentCaptor.forClass(EmployerProfile.class);
        verify(repository).save(captor.capture());
        EmployerProfile saved = captor.getValue();

        assertThat(saved.getCompanyName()).isEqualTo("New Corp");    // updated
        assertThat(saved.getCompanySize()).isEqualTo("1-10");        // preserved
        assertThat(saved.getIndustry()).isEqualTo("Fintech");        // set
    }

    @Test
    void createOrUpdate_nullFieldsAreIgnored_existingValuesPreserved() {
        UUID userId = UUID.randomUUID();
        EmployerProfile existing = EmployerProfile.builder()
                .userId(userId)
                .companyName("Keep Corp")
                .companyWebsite("https://keep.com")
                .build();

        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateEmployerProfileRequest request = new UpdateEmployerProfileRequest();
        request.setIndustry("Healthcare");  // only this field set

        service.createOrUpdate(userId, request);

        ArgumentCaptor<EmployerProfile> captor = ArgumentCaptor.forClass(EmployerProfile.class);
        verify(repository).save(captor.capture());
        EmployerProfile saved = captor.getValue();

        assertThat(saved.getCompanyName()).isEqualTo("Keep Corp");       // preserved
        assertThat(saved.getCompanyWebsite()).isEqualTo("https://keep.com"); // preserved
        assertThat(saved.getIndustry()).isEqualTo("Healthcare");          // new value
    }

    @Test
    void getByUserId_throwsResourceNotFoundException_whenProfileNotFound() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }
}
