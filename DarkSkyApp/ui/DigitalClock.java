package com.darksky.ui;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * File Header Comment:
 * The DigitalClock class displays a real-time digital clock on the screen and updates every second.
 */

/**
 * Class Header Comment:
 * The DigitalClock class extends JLabel and implements Runnable to create a digital clock
 * that continuously updates and displays the current time in a specified format.
 */
public class DigitalClock extends JLabel implements Runnable {
    // Constant to define the time format for the digital clock
    private static final String TIME_FORMAT = "hh:mm a";  // Time format used for the clock (12-hour format with AM/PM)

    // The thread responsible for updating the clock every second
    private Thread clockThread;  // A thread to run the clock updates in the background

    /**
     * Constructor to initialize the digital clock's appearance and start the clock.
     *
     * @param screenWidth The width of the screen, used to position the clock.
     * @param screenHeight The height of the screen, used to position the clock.
     */
    public DigitalClock(int screenWidth, int screenHeight) {
        setFont(new Font("Arial", Font.BOLD, 35));  // Set font style, weight, and size for the clock display
        setForeground(Color.WHITE);  // Set the clock text color to white
        setHorizontalAlignment(SwingConstants.CENTER);  // Align the text to the center of the label
        setBounds(screenWidth - 250, 50, 200, 40);  // Set the position and size of the clock label on the screen
        startClock();  // Start the clock's background thread to update time
    }

    /**
     * Starts the clock by creating and starting a new thread to run the clock updates.
     */
    private void startClock() {
        clockThread = new Thread(this);  // Create a new thread that will run the clock updates
        clockThread.start();  // Start the thread to run the clock
    }

    /**
     * The run method continuously updates the clock every second in a separate thread.
     */
    @Override
    public void run() {
        while (true) {  // Infinite loop to update the clock continuously
            updateClock();  // Update the clock display
            try {
                Thread.sleep(1000);  // Pause for 1 second before updating again
            } catch (InterruptedException e) {
                e.printStackTrace();  // Print stack trace if an interruption occurs
            }
        }
    }

    /**
     * Updates the clock's label with the current time.
     */
    private void updateClock() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);  // Create a new SimpleDateFormat with the defined time format
        String currentTime = sdf.format(new Date());  // Get the current time formatted as a string
        setText(currentTime);  // Set the label's text to the current time
    }
}
