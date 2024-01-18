package ru.nikidzawa.networkAPI.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonalChatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private ChatEntity chat;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interlocutor_id")
    private UserEntity interlocutor;

    private int newMessagesCount;
}
