package ru.nikidzawa.golink.services.GrapgicPatterns;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

@Component
public class DefaultAvatarCreator {
    private final Image defaultAvatar = new Image(Objects.requireNonNull(getClass().getResource("/img/defaultAvatar.jpg")).toExternalForm());

    @SneakyThrows
    public byte[] createAvatar(String firstNameSymbol) {
        Canvas canvas = new Canvas(600, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFontSmoothingType(FontSmoothingType.LCD);
        gc.drawImage(defaultAvatar, 0, 0);
        double x = 300;
        double y = 300;
        Font font = Font.font("Arial", FontWeight.NORMAL, 300);
        gc.setFont(font);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(Color.WHITE);
        gc.fillText(firstNameSymbol, x, y);

        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(new SnapshotParameters(), writableImage);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
