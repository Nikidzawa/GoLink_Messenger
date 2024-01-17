package ru.nikidzawa.golink.services.sound;

import lombok.Getter;
import lombok.SneakyThrows;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class AudioHelper {

    @Getter
    private static File file;
    private static final String filename = "audio_";
    private static int suffix = 0;
    private static final AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private static final int MONO = 1;
    private static final AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, 48000, 16, MONO, 2, 48000, true);
    private static TargetDataLine mike;

    public static void startRecording() {
        createNewFile();
        new Thread() {
            public void run() {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    JOptionPane.showMessageDialog(null, "Line not supported" +
                                    info, "Line not supported",
                            JOptionPane.ERROR_MESSAGE);
                }
                try {
                    mike = (TargetDataLine) AudioSystem.getLine(info);
                    mike.open(format, mike.getBufferSize());
                    AudioInputStream sound = new AudioInputStream(mike);
                    mike.start();
                    AudioSystem.write(sound, fileType, file);
                } catch (LineUnavailableException ex) {
                    JOptionPane.showMessageDialog(null, "Line not available" +
                                    ex, "Line not available",
                            JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "I/O Error " + ex,
                            "I/O Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.start();
    }

    private static void createNewFile() {
        try {
            do {
                String soundFileName = filename + (suffix++) + "." + fileType.getExtension();
                file = new File(soundFileName);
            } while (!file.createNewFile());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void stopRecording() {
        mike.stop();
        mike.close();
    }

    public static byte[] convertAudioToBytes(File audioFile) throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = audioInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        audioInputStream.close();
        byteArrayOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    @SneakyThrows
    public static File convertBytesToAudio(byte[] audioBytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);
        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioBytes.length / format.getFrameSize());

        File audioFile = new File("audio.wav");
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, audioFile);

        audioInputStream.close();
        byteArrayInputStream.close();
        return audioFile;
    }
}