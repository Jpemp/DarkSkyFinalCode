package com.darksky.utils;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgcodecs.Imgcodecs;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AutoCapture {

    private VideoCapture capture;
    private boolean isCapturing = false;
    private volatile int captureInterval;  // Time interval


    public AutoCapture() {
        captureInterval = 60000; // 60 seconds
    }

    public boolean isCapturing() {
        return isCapturing;
    }

    public void startCapture() {
        if (isCapturing) return;  // Prevent starting multiple captures at the same time
        isCapturing = true;
        capture = new VideoCapture(0);

        // Verify if camera is correct.
        if (!capture.isOpened()) {
            System.out.println("Camera not found!");
            return;
        }

        setMaxCameraResolution();

        new Thread(() -> {
            Mat frame = new Mat();
            while (isCapturing) {
                if (capture.read(frame)) {
                    captureImage(frame);
                }
                try {
                    Thread.sleep(captureInterval); // Waiting interval between pictures
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stopCapture() {
        isCapturing = false;
        if (capture != null) {
            capture.release();
        }
    }

    /**
     * Resets the capture process. Stops and restarts the capture.
     */
    public void reset() {
        stopCapture();
        startCapture();
    }

    private void captureImage(Mat frame) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String filename = timestamp + ".png";

        try {
            Path reportsDir = Paths.get(System.getProperty("user.home"), "Documents", "DarkSky Reports");

            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Path dateFolder = reportsDir.resolve(currentDate);

            Path imagePath = dateFolder.resolve(filename);
            System.out.println("Attempting to save to: " + imagePath);

            if (!Imgcodecs.imwrite(imagePath.toString(), frame)) {
                throw new Exception("Failed to save image.");
            }

            System.out.println("Snapshot saved as " + filename);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMaxCameraResolution() {
        // Get the current maximum width and height supported by the camera
        double maxWidth = capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        double maxHeight = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);

        // Set the resolution to 1920x1080 if it's smaller, or the camera's max resolution
        if (maxWidth < 1920) {
            capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 1920);
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 1080);
        } else {
            capture.set(Videoio.CAP_PROP_FRAME_WIDTH, maxWidth);
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, maxHeight);
        }

        // Confirm the final resolution
        System.out.println("Using resolution: " + capture.get(Videoio.CAP_PROP_FRAME_WIDTH) + "x" + capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
    }

    public int getCaptureInterval() {
        return captureInterval;
    }

    public void setCaptureInterval(int interval) {
        this.captureInterval = interval;
        if (isCapturing) {
            reset();
        }
    }
}