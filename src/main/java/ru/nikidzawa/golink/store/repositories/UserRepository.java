package ru.nikidzawa.golink.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.golink.store.entities.UserEntity;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findFirstByPhone(Long number);

    Optional<UserEntity> findFirstByNickname(String nickname);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserEntity u WHERE u.nickname = :phone AND u.password = :password")
    boolean existsByPhoneAndPassword(@Param("phone") String nickname, @Param("password") String password);
}