package com.darksky.utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * ViewReports class - A JFrame that allows users to view the "DarkSky Reports" directory in their Documents folder.
 * This class handles opening the directory and showing appropriate messages if errors occur.
 */
public class ViewReports extends JFrame {

    /**
     * Constructor for the ViewReports class.
     * Initializes the window and attempts to open the "DarkSky Reports" directory.
     */
    public ViewReports() {
        openViewReportsDirectory();  // Calls the method to open the reports directory
    }

    /**
     * Opens the "DarkSky Reports" directory located in the user's Documents folder.
     * Displays a message if the directory is missing or an error occurs while opening it.
     */
    private void openViewReportsDirectory() {
        try {
            // Getting the path to the user's home directory (e.g., C:\Users\Username or /home/username)
            String userHome = System.getProperty("user.home");

            // Defining the path to the "DarkSky Reports" folder inside the Documents directory
            File reportsDir = new File(userHome, "Documents/DarkSky Reports");

            // Checking if the reports directory exists and is a directory
            if (reportsDir.exists() && reportsDir.isDirectory()) {
                // Check if the directory is empty
                File[] files = reportsDir.listFiles();
                if (files != null && files.length > 0) {
                    // If files exist in the directory, open it
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(reportsDir);  // Open the directory
                    } else {
                        // Show an error message if desktop operations are not supported on the system
                        JOptionPane.showMessageDialog(this, "Desktop operations not supported on this system", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // If the directory is empty, show the "No reports found!" message
                    JOptionPane.showMessageDialog(this, "No reports found!", "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                // If the reports directory does not exist or is not a valid directory, show an information message
                JOptionPane.showMessageDialog(this, "No reports found!", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            // Handle any I/O exceptions that occur when trying to open the directory
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}