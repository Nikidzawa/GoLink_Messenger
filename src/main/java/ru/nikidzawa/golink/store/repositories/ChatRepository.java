package ru.nikidzawa.golink.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.golink.store.entities.ChatEntity;
import ru.nikidzawa.golink.store.entities.UserEntity;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {
    List<ChatEntity> findByParticipantsContaining (UserEntity userEntity);
}
