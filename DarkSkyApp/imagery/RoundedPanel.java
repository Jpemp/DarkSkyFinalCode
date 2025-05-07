package com.darksky.imagery;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * File Header Comment:
 * The RoundedPanel class extends JPanel and is used to create a custom panel with rounded corners.
 * It allows for transparent backgrounds and provides the ability to customize the corner radius.
 */

/**
 * Class Header Comment:
 * The RoundedPanel class extends JPanel and is designed to display a panel with rounded corners.
 * The corner radius can be customized by passing a value to the constructor, allowing for a
 * visually appealing, rounded appearance. The panel's background color is set by the container.
 */
public class RoundedPanel extends JPanel {
    // Instance variable to store the radius of the rounded corners
    private final int cornerRadius;  // The radius of the panel's rounded corners

    /**
     * Constructor to initialize the rounded panel with a specified corner radius.
     *
     * @param cornerRadius The radius of the rounded corners to be used in the panel.
     */
    public RoundedPanel(int cornerRadius) {
        // Assign the given radius to the instance variable
        this.cornerRadius = cornerRadius;  // Store the radius for later use

        // Set the panel to be transparent (opaque = false) so the background color is not shown
        setOpaque(false);  // Makes the panel transparent to let the background color show through
    }

    /**
     * Overridden paintComponent method to draw the rounded rectangle on the panel.
     *
     * @param g The Graphics object used to draw the panel.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);  // Call the parent class's paintComponent method to handle basic painting

        // Cast the Graphics object to Graphics2D for better control over rendering
        Graphics2D g2d = (Graphics2D) g;  // Cast to Graphics2D for improved rendering capabilities

        // Enable anti-aliasing to smooth the edges of the rounded corners
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);  // Enables anti-aliasing for smoother rendering

        // Set the color to the background color of the panel
        g2d.setColor(getBackground());  // Use the current background color of the panel

        // Fill a rounded rectangle with the specified corner radius
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);  // Draw the rounded rectangle with the specified dimensions and corner radius
    }
}