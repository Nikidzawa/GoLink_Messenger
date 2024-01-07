package ru.nikidzawa.golink.services;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.Objects;

public class Sounds {

    public static void notification () {
        play(Objects.requireNonNull(Sounds.class.getResource("/sounds/notification.wav")).getPath());
    }

    private static void play (String soundFile) {
        Media media = new Media(new File(soundFile).toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();
    }
}