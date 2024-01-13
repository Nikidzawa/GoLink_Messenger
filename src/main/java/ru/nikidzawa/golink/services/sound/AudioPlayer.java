package ru.nikidzawa.golink.services.sound;

import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.SneakyThrows;

import java.io.File;
import java.util.Objects;

public class AudioPlayer {
    private final MediaPlayer mediaPlayer;

    private String pauseImage;
    private String playingImage;
    private boolean isPaused = true;
    private boolean isStopped = false;

    public interface OnReadyCallback {
        void onReady(Duration duration);
    }

    @SneakyThrows
    public AudioPlayer(File audioFile, ProgressBar progressBar, OnReadyCallback callback, boolean isMyMessage, ImageView imageView) {
        Media media = new Media(audioFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                double progress = newValue.toMillis() / media.getDuration().toMillis();
                progressBar.setProgress(progress);
            }
        });

        mediaPlayer.setOnReady(() -> {
            if (callback != null) {
                callback.onReady(mediaPlayer.getMedia().getDuration());
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            imageView.setImage(new Image(Objects.requireNonNull(playingImage)));
            isStopped = true;
        });

        progressBar.setProgress(0);
        progressBar.setOnMouseClicked((MouseEvent event) -> {
            double totalWidth = progressBar.getWidth();
            double mouseX = event.getX();
            double progress = mouseX / totalWidth;

            Duration seekTime = media.getDuration().multiply(progress);
            mediaPlayer.seek(seekTime);
        });

        setGUI(isMyMessage);
        imageView.setImage(new Image(Objects.requireNonNull(playingImage)));
        imageView.setOnMouseClicked(mouseEvent -> {
            if (isPaused) {
                imageView.setImage(new Image(Objects.requireNonNull(pauseImage)));
                mediaPlayer.play();
                isPaused = false;
            } else if (isStopped) {
                imageView.setImage(new Image(Objects.requireNonNull(pauseImage)));
                mediaPlayer.seek(Duration.ZERO);
                isStopped = false;
                mediaPlayer.play();
            }
            else {
                imageView.setImage(new Image(Objects.requireNonNull(playingImage)));
                mediaPlayer.pause();
                isPaused = true;
            }
        });
    }

    private void setGUI(boolean isMyMessage) {
        if (isMyMessage) {
            playingImage = Objects.requireNonNull(getClass().getResource("/img/play_sound_white.png")).toExternalForm();
            pauseImage = Objects.requireNonNull(getClass().getResource("/img/pause_white.png")).toExternalForm();
        } else {
            playingImage = Objects.requireNonNull(getClass().getResource("/img/play_sound_blue.png")).toExternalForm();
            pauseImage = Objects.requireNonNull(getClass().getResource("/img/pause_blue.png")).toExternalForm();
        }
    }
}