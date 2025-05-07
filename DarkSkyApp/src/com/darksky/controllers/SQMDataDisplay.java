package com.darksky.controllers;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

/**
 * File Header Comment:
 * This class is responsible for retrieving and displaying real-time light pollution data
 * from the SQM-LE (Sky Quality Meter) device. The data is fetched over a network connection
 * and displayed on the screen. Additionally, it is logged into an Excel file for future analysis.
 */

/**
 * Class Header Comment:
 * SQMDataDisplay extends JLabel and continuously updates with real-time sky brightness
 * measurements from the SQM-LE device. It runs in a separate thread to fetch and refresh
 * data every few seconds.
 */
public class SQMDataDisplay extends JLabel implements Runnable {

    // IP address of the SQM-LE device (make sure this is correct!)
    private static final String SERVER_IP = "192.168.1.74";
    private static final int PORT = 10001; // The port number used to communicate with the device
    private int measurementInterval = 5000; // Default: 5 seconds

    private Thread sqmThread; // This thread will keep fetching and updating the data.

    /**
     * Method Header Comment:
     * Initializes the SQMDataDisplay label with styling and starts the background thread
     * to fetch real-time light pollution data.
     *
     * @param screenWidth  Used to position the label correctly on the screen.
     * @param screenHeight Used to determine the font size and positioning.
     */
    public SQMDataDisplay(int screenWidth, int screenHeight) {
        // Set up the text style (font size, color, and alignment)
        setFont(new Font("Arial", Font.BOLD, 25));
        setForeground(Color.YELLOW);
        setHorizontalAlignment(SwingConstants.CENTER);

        int x = (int) (screenWidth * 0.02);
        int y = (int) (screenHeight * 0.90);
        int width = (int) (screenWidth * 0.7);
        int height = (int) (screenHeight * 0.10);

        setBounds(x, y, width, height);

        // Set default text before any real data is fetched
        setText("SQM Data: ---");

        // Start getting the data from the SQM-LE device
        startSQM();
    }

    /**
     * Method Header Comment:
     * Starts a new thread to constantly fetch and update SQM data.
     */
    private void startSQM() {
        sqmThread = new Thread(this); // Create a new thread
        sqmThread.start(); // Start running it
    }

    /**
     * Stops the current thread (if running) and starts a new one
     * to apply a new interval or restart measurements.
     */
    public void reset() {
        if (sqmThread != null && sqmThread.isAlive()) {
            sqmThread.interrupt(); // Interrupt the running thread
        }
        startSQM(); // Start a new one
    }

    /**
     * Method Header Comment:
     * This method runs continuously, fetching new light pollution data
     * from the SQM-LE device and updating the display label.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String measurement = getSQMMeasurement();
            setText("SQM Data: " + measurement);
            SQMExcelLogger.saveMeasurementToExcelWithGraph(measurement);

            try {
                Thread.sleep(measurementInterval);
            } catch (InterruptedException e) {
                // Exit loop if thread is interrupted
                break;
            }
        }
    }

    /**
     * Method Header Comment:
     * Connects to the SQM-LE device, requests the latest measurement, and returns the data.
     *
     * @return The measurement data as a string, or an error message if something goes wrong.
     */
    private String getSQMMeasurement() {
        try (
                // Open a connection to the SQM-LE device
                Socket socket = new Socket(SERVER_IP, PORT);

                // Set up a way to send data to the device
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Set up a way to receive data from the device
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            // Send the command "rx" (this tells the device to send us the latest data)
            out.println("rx");

            // Read and return the response from the device
            return in.readLine();
        } catch (IOException e) {
            // If something goes wrong (like the device is offline), print an error and return a message
            e.printStackTrace();
            return "Error retrieving data";
        }
    }

    public int getMeasurementInterval() {
        return measurementInterval;
    }

    public void setMeasurementInterval(int interval) {
        this.measurementInterval = interval;
    }
}