package ru.nikidzawa.golink.FXControllers.Configurations;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;
import ru.nikidzawa.golink.FXControllers.GoLink;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class ScrollConfig {
    private static final long SCROLLBAR_HIDE_DELAY = 2500;
    private final javafx.scene.control.ScrollPane scrollPane;
    private Timer timer;
    private final ScrollBar vBar;
    private final Node thumb;
    private boolean wait;

    public ScrollConfig(javafx.scene.control.ScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        scrollPane.getStylesheets().add(Objects.requireNonNull(GoLink.class.getResource("/styles/scroll.css")).toExternalForm());
        vBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
        thumb = scrollPane.lookup(".scroll-bar:vertical .thumb");
        initializeTimer();
        setConfig();
    }

    private void setConfig () {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            showVBar();
            startTimer();
        });
        vBar.setOnDragDetected(mouseEvent -> showVBar());
        vBar.setOnMouseEntered(mouseEvent -> {
            wait = true;
            showVBar();
        });
        vBar.setOnMouseExited(mouseEvent -> {
            wait = false;
            startTimer();
        });
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
            initializeTimer();
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!wait) {
                    hideScrollPane();
                }
            }
        }, SCROLLBAR_HIDE_DELAY);
    }

    private void initializeTimer() {
        timer = new Timer(true);
    }

    public void startScrollEvent () {
        showVBar();
        startTimer();
    }

    private void showVBar () {
        thumb.setStyle("-fx-background-color: #002b54;");
    }

    private void hideScrollPane () {
        thumb.setStyle("-fx-background-color: transparent;");
    }

    public void scrolling () {
        PauseTransition pauseTransition = new PauseTransition(Duration.millis(20));
        pauseTransition.setOnFinished(event -> scrollPane.setVvalue(1));
        pauseTransition.play();
    }
}