package ru.nikidzawa.golink;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<ChatEntity, Long> {
    List<ChatEntity> findByParticipantsContaining (UserEntity userEntity);
}
