package ru.nikidzawa.golink.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nikidzawa.golink.store.entities.PersonalChat;

public interface PersonalChatRepository extends JpaRepository<PersonalChat, Long> {

}
