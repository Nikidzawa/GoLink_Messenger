package ru.nikidzawa.networkAPI.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.networkAPI.store.entities.ChatEntity;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {
}