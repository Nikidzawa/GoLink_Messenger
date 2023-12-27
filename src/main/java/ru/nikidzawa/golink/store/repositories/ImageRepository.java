package ru.nikidzawa.golink.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nikidzawa.golink.store.entities.ImageEntity;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

}