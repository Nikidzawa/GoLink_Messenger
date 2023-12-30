package ru.nikidzawa.golink.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int port;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private List<MessageEntity> messages;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private List<ImageEntity> images;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PersonalChat> personalChats;
}