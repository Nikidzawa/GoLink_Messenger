package ru.nikidzawa.networkAPI.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.networkAPI.store.entities.MessageEntity;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
}