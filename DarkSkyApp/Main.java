// Main.java
// This is the entry point for the Dark Sky project application. It loads the necessary OpenCV library
// and initializes the Dashboard window for user authentication.

package com.darksky;

// Importing the Dashboard window class from the UI package to display the Dashboard interface
import com.darksky.ui.DashboardWindow;
// Importing the Core class from OpenCV to load necessary native libraries
import org.opencv.core.Core;

public class Main {

    // Main method to start the Dark Sky application
    // @param args Command line arguments passed when running the program
    public static void main(String[] args) {

        // Loading the OpenCV library required for image processing functionalities
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Creating a new instance of the Dashboard window
        new DashboardWindow();
    }
}