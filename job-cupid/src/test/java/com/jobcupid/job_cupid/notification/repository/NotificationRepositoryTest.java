package com.jobcupid.job_cupid.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.jobcupid.job_cupid.notification.entity.Notification;
import com.jobcupid.job_cupid.notification.entity.NotificationType;
import com.jobcupid.job_cupid.user.entity.User;
import com.jobcupid.job_cupid.user.entity.UserRole;
import com.jobcupid.job_cupid.user.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class NotificationRepositoryTest {

    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository         userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("notif-user-" + UUID.randomUUID() + "@test.com")
                .passwordHash("x").firstName("Alice").lastName("Chen")
                .role(UserRole.USER).build());
        userId = user.getId();
    }

    private Notification buildNotification(boolean isRead) {
        return Notification.builder()
                .userId(userId)
                .type(NotificationType.MATCH_CREATED)
                .title("You have a new match!")
                .body("A company liked your application.")
                .isRead(isRead)
                .build();
    }

    @Test
    void countByUserIdAndIsReadFalse_returns3_when5NotificationsWith3Unread() {
        List<Notification> notifications = List.of(
                buildNotification(false),
                buildNotification(false),
                buildNotification(false),
                buildNotification(true),
                buildNotification(true)
        );
        notificationRepository.saveAllAndFlush(notifications);

        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void findByUserIdAndIsReadFalseOrderByCreatedAtDesc_returnsOnlyUnread_forMixedReadUnread() {
        notificationRepository.saveAllAndFlush(List.of(
                buildNotification(false),
                buildNotification(true),
                buildNotification(false)
        ));

        Page<Notification> result = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(n -> !n.getIsRead());
    }
}
