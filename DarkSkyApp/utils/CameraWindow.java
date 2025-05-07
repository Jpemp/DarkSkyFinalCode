package com.darksky.utils;

import com.darksky.ui.DashboardWindow;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * File Header Comment:
 * This class represents a camera window that captures and displays live video from a webcam.
 * It provides functionality for capturing images and saving them to the user's local file system.
 */

/**
 * Class Header Comment:
 * The CameraWindow class creates and displays a live video feed from a webcam.
 * It allows the user to toggle the camera on/off, capture images, and navigate back to the previous screen.
 */
public class CameraWindow extends JFrame {

    // Key UI components for camera display and interaction
    private JLabel cameraLabel;  // Displays the live video feed
    private VideoCapture capture;  // Represents the video capture device (webcam)
    private boolean isRunning;  // Tracks whether the camera is running or not
    private JButton toggleButton, snapshotButton, backButton;  // Buttons for camera control and image capture
    private JLayeredPane layeredPane;  // Layout container for layered UI components
    private JFrame dashboardWindow;  // Reference to the dashboard window

    /**
     * Constructor to initialize and set up the camera window UI and functionality.
     */
    public CameraWindow(JFrame dashboardWindow) {
        this.dashboardWindow = dashboardWindow;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        // Set up the window properties
        setTitle("Live Video Feed");
        setUndecorated(true);
        setSize(screenWidth, screenHeight);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Initialize the layered pane and add the camera label
        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        add(layeredPane);

        // Define margins for positioning the buttons
        int margin = 10; // Space from the edges
        int buttonWidth = screenWidth / 10;
        int buttonHeight = screenHeight / 20;

        // Define the starting position for the buttons
        int startX = margin;
        int startY = margin;

        cameraLabel = new JLabel();
        cameraLabel.setBounds(0, 0, screenWidth, screenHeight);
        cameraLabel.setOpaque(false);
        layeredPane.add(cameraLabel, Integer.valueOf(0));


        // Create and position the buttons horizontally
        toggleButton = createTransparentButton("Stop Camera", startX, startY);
        snapshotButton = createTransparentButton("Capture Image", startX + buttonWidth + margin, startY);
        backButton = createTransparentButton("Back", startX + 2 * (buttonWidth + margin), startY);

        // Add buttons to the layered pane
        layeredPane.add(toggleButton, Integer.valueOf(1));
        layeredPane.add(snapshotButton, Integer.valueOf(1));
        layeredPane.add(backButton, Integer.valueOf(1));

        // Set button actions
        toggleButton.addActionListener(e -> toggleCamera());
        snapshotButton.addActionListener(e -> captureImage());
        backButton.addActionListener(e -> goBack());

        // Start the camera feed
        startCamera();
        setVisible(true);
    }

    /**
     * Creates a transparent button with the specified text and position.
     *
     * @param text The button's label text.
     * @param x The x-coordinate of the button's position.
     * @param y The y-coordinate of the button's position.
     * @return A configured JButton.
     */
    private JButton createTransparentButton(String text, int x, int y) {
        JButton button = new JButton(text);
        button.setBounds(x, y, 125, 25);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(true);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0, 0, 0, 100));
        return button;
    }

    /**
     * Starts the camera capture and displays the live video feed.
     */
    private void startCamera() {

        if (((DashboardWindow) dashboardWindow).autoCapture.isCapturing()) {
            return;
        }
        isRunning = true;
        capture = new VideoCapture(0);

        double maxWidth = capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        double maxHeight = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);

        if (maxWidth < 1920) {
            capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 1920);
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 1080);
        } else {
            capture.set(Videoio.CAP_PROP_FRAME_WIDTH, maxWidth);
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, maxHeight);
        }

        System.out.println("Using resolution: " + capture.get(Videoio.CAP_PROP_FRAME_WIDTH) +
                "x" + capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));

        if (!capture.isOpened()) {
            JOptionPane.showMessageDialog(this, "Camera not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            Mat frame = new Mat();
            while (isRunning) {
                if (capture.read(frame)) {
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);
                    updateCameraDisplay(frame);
                }
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Stops the camera capture and releases the resources.
     */
    private void stopCamera() {
        isRunning = false;
        if (capture != null) {
            capture.release();
        }
    }

    /**
     * Toggles the camera on or off when the button is clicked.
     */
    private void toggleCamera() {
        if (isRunning) {
            stopCamera();
            toggleButton.setText("Start Camera");
        } else {
            // Instead of directly starting the camera, reset it to ensure fresh start
            ((DashboardWindow) dashboardWindow).autoCapture.reset(); // Reset AutoCapture here
            toggleButton.setText("Stop Camera");
        }
    }

    /**
     * Captures a snapshot from the live video feed and saves it in the same way as AutoCapture.
     */
    private void captureImage() {
        if (!isRunning || capture == null) return;

        Mat frame = new Mat();
        if (capture.read(frame)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String filename = timestamp + ".png";

            try {
                Path reportsDir = Paths.get(System.getProperty("user.home"), "Documents", "DarkSky Reports");

                // Create a date-based subfolder like AutoCapture does
                String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                Path dateFolder = reportsDir.resolve(currentDate);

                // Ensure directory exists
                if (Files.notExists(dateFolder)) {
                    Files.createDirectories(dateFolder);
                }

                Path imagePath = dateFolder.resolve(filename);
                System.out.println("Attempting to save to: " + imagePath);

                if (!Imgcodecs.imwrite(imagePath.toString(), frame)) {
                    throw new Exception("Failed to save image.");
                }

                JOptionPane.showMessageDialog(this, "Snapshot saved as " + filename + " in " + imagePath);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Goes back to the previous window and stops the camera.
     */
    private void goBack() {
        stopCamera();  // Stop the camera feed
        setVisible(false);  // Hide the camera window
        if (dashboardWindow != null) {
            dashboardWindow.setVisible(true);  // Make the dashboard visible again
            ((DashboardWindow) dashboardWindow).autoCapture.reset(); // Reset AutoCapture
        }
        dispose();
    }


    /**
     * Updates the camera display with the given frame.
     *
     * @param frame The current frame to display on the screen.
     */
    private void updateCameraDisplay(Mat frame) {
        ImageIcon image = new ImageIcon(convertMatToBufferedImage(frame));

        Image scaledImage = image.getImage().getScaledInstance(
                cameraLabel.getWidth(), cameraLabel.getHeight(), Image.SCALE_SMOOTH
        );

        cameraLabel.setIcon(new ImageIcon(scaledImage));
    }


    /**
     * Converts a Mat object (OpenCV frame) to a BufferedImage for display in Swing components.
     *
     * @param mat The OpenCV Mat object to convert.
     * @return A BufferedImage representing the Mat object.
     */
    private BufferedImage convertMatToBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        int channels = mat.channels();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);
        image.getRaster().setDataElements(0, 0, width, height, sourcePixels);
        return image;
    }
}