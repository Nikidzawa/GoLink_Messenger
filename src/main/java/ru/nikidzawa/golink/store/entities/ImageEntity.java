package ru.nikidzawa.golink.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageEntity {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    byte[] metadata;

    LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private UserEntity sender;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private ChatEntity chat;
}
