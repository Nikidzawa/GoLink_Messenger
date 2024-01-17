package ru.nikidzawa.golink.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
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

    @OneToMany(mappedBy = "chat", fetch = FetchType.EAGER)
    private List<MessageEntity> messages = new ArrayList<>();

    @OneToMany(mappedBy = "chat", fetch = FetchType.EAGER)
    private List<PersonalChat> personalChats = new ArrayList<>();
}