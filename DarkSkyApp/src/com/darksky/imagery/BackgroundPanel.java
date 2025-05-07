package com.darksky.imagery;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * File Header Comment:
 * The BackgroundPanel class extends JPanel and is used to display a custom background image
 * in a panel. The image is loaded from a URL and scaled to fit the panel's dimensions.
 */

/**
 * Class Header Comment:
 * The BackgroundPanel class provides a custom JPanel that loads and displays an image
 * as its background. The image is fetched from a URL and scaled to fill the panel’s size
 * during rendering.
 */
public class BackgroundPanel extends JPanel {
    // Instance variable to store the background image
    private Image backgroundImage;  // Stores the background image to be displayed

    /**
     * Constructor to initialize the BackgroundPanel with a background image from the given URL.
     *
     * @param resourceUrl The URL pointing to the image to be displayed as the background.
     * @throws IllegalArgumentException if the image cannot be loaded from the provided URL.
     */
    public BackgroundPanel(URL resourceUrl) {
        try {
            // Check if the resource URL is valid
            if (resourceUrl == null) {
                throw new IllegalArgumentException("Image resource not found.");
            }
            // Load the image from the URL and assign it to the backgroundImage variable
            backgroundImage = ImageIO.read(resourceUrl);
        } catch (IOException e) {
            e.printStackTrace();  // Print the stack trace in case of an error
            throw new IllegalArgumentException("Error loading image.");  // Throw an exception if the image cannot be loaded
        }
    }

    /**
     * Overridden paintComponent method to draw the background image on the panel.
     *
     * @param g The Graphics object used for rendering.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);  // Call the parent class's paintComponent method

        // If the background image is not null, draw it scaled to the panel's size
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);  // Scale the image to fit the panel's size
        }
    }
}