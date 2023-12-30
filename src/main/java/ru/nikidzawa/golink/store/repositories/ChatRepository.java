package ru.nikidzawa.golink.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.golink.store.entities.ChatEntity;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {
}