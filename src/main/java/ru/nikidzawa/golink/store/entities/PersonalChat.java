package ru.nikidzawa.golink.store.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonalChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private ChatEntity chat;

    @ManyToOne
    @JoinColumn(name = "interlocutor_id")
    private UserEntity interlocutor;

    private int newMessagesCount;
}
