package com.darksky.ui;

import com.darksky.controllers.SQMDataDisplay;
import com.darksky.imagery.RoundedPanel;
import com.darksky.imagery.BackgroundPanel;
import com.darksky.utils.AutoCapture;
import com.darksky.utils.CameraWindow;
import com.darksky.utils.ViewReports;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * File Header Comment:
 * This class represents the main dashboard window for the DarkSky application.
 * It provides the user interface where users can access features like live video, view reports, manage sensors, and settings.
 */

/**
 * Class Header Comment:
 * The DashboardWindow class is responsible for displaying the main user interface upon successful login.
 * It sets up the background, logo, and various functional buttons, and provides the layout for a user-friendly experience.
 */
public class DashboardWindow extends JFrame {

    private JLabel welcomeLabel; // Label to display a personalized welcome message.

    public AutoCapture autoCapture;

    // Variable to store the path to today's folder
    private String todaysFolderPath;
    private String currentDate;
    private Timer timer;

    private SQMDataDisplay sqmDisplay;

    /**
     * Method Header Comment:
     * Initializes the Dashboard window with the provided user data.
     * Sets up the user interface elements including background, logo, form panel, and buttons.
     */
    public DashboardWindow() {
        autoCapture = new AutoCapture();
        autoCapture.startCapture();

        setTitle("DarkSky - Dashboard");

        // Get screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        setSize(screenWidth, screenHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close only this window
        setUndecorated(true);

        // Load background image
        URL resourceUrl = getClass().getResource("/com/darksky/resources/Shelby-and-Paul-so-cute_WEB.jpg");
        BackgroundPanel backgroundPanel = new BackgroundPanel(resourceUrl);
        backgroundPanel.setLayout(new BorderLayout()); // Use layout manager

        add(backgroundPanel);
        addLogoToPanel(backgroundPanel, screenWidth);

        // Create and add the digital clock to the panel
        DigitalClock clock = new DigitalClock(screenWidth, screenHeight);
        backgroundPanel.add(clock);

        // Create and add the SQM data display to the panel
        this.sqmDisplay = new SQMDataDisplay(screenWidth, screenHeight);
        backgroundPanel.add(sqmDisplay);

        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setOpaque(false);

        // Create the rounded form panel and add it to the main panel
        RoundedPanel formPanel = createFormPanel(screenWidth, screenHeight);
        wrapperPanel.add(formPanel);

        placeComponents(formPanel, screenWidth, screenHeight);

        backgroundPanel.add(wrapperPanel, BorderLayout.CENTER);

        updateFolderPath();

        startDateCheckTimer();

        setLocationRelativeTo(null);
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (timer != null) {
                    timer.cancel();  // Stop the timer
                }
                autoCapture.stopCapture(); // Stop the capture before closing the window
                System.exit(0); // Exit the application
            }
        });
    }

    /**
     * Helper method to get the current date in "yyyy-MM-dd" format
     *
     * @return Current date as a string
     */
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    private void startDateCheckTimer() {
        // Initialize currentDate once the window is ready
        currentDate = getCurrentDate();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Check if the date has changed using the already initialized currentDate
                String currentDateInThread = getCurrentDate();
                if (!currentDateInThread.equals(currentDate)) {
                    currentDate = currentDateInThread;  // Update the currentDate
                    updateFolderPath();  // Call the method to create/update the folder
                }
                // Additional check to recreate the folder if it's missing
                else {
                    updateFolderPath();  // Recheck and create the folder if necessary
                }
            }
        }, 0, 60000);  // Check every minute
    }

    /**
     * Helper method to create or update the folder path for today
     */
    private void updateFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String newDate = sdf.format(new Date()); // Get today's date
        System.out.println("Current Date for Folder: " + newDate);

        // Use the same approach as the image capture method to get the correct path
        try {
            Path reportsDir = Paths.get(System.getProperty("user.home"), "Documents", "DarkSky Reports");

            if (Files.notExists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }

            Path todaysFolder = reportsDir.resolve(newDate);

            if (Files.notExists(todaysFolder)) {
                Files.createDirectories(todaysFolder); // Create the folder for today if it doesn't exist
            }

            todaysFolderPath = todaysFolder.toString(); // Store the path for later use

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to create and add the form panel with buttons and other UI elements.
     *
     * @param screenWidth  The width of the screen used to dynamically position components.
     * @param screenHeight The height of the screen used to dynamically position components.
     * @return The form panel containing buttons and other components.
     */
    private RoundedPanel createFormPanel(int screenWidth, int screenHeight) {
        // Create a RoundedPanel with rounded corners (e.g., 30px radius)
        RoundedPanel formPanel = new RoundedPanel(30);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(new Color(255, 255, 255, 226));  // Set a semi-transparent background

        // Set bounds for form panel
        formPanel.setPreferredSize(new Dimension((int) (screenWidth * 0.30), (int) (screenHeight * 0.40)));
        formPanel.setMinimumSize(new Dimension(300, 200));
        formPanel.setMaximumSize(new Dimension(screenWidth, screenHeight));

        return formPanel;
    }

    /**
     * Method Header Comment:
     * Adds the logo image to the top of the panel with a specific size and position.
     *
     * @param panel       The panel where the logo will be added.
     * @param screenWidth The width of the screen used to calculate the logo's size.
     */
    private void addLogoToPanel(JPanel panel, int screenWidth) {
        // Load the logo image
        URL logoUrl = getClass().getClassLoader().getResource("com/darksky/resources/blanco-ida.png");
        if (logoUrl == null) {
            throw new IllegalArgumentException("Logo image not found in com.darksky.resources!");
        }
        ImageIcon originalIcon = new ImageIcon(logoUrl);  // Load the image.
        Image img = originalIcon.getImage();

        // Resize the image to fit the screen width
        int newWidth = (int) (screenWidth * 0.28);
        int newHeight = (int) (newWidth * 0.54);

        Image resizedImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);  // Smooth resizing.
        ImageIcon resizedIcon = new ImageIcon(resizedImg);

        JLabel logoLabel = new JLabel(resizedIcon);  // Create a label with the resized logo.

        // Center the logo at the top of the window
        int logoX = (screenWidth - newWidth) / 2;
        logoLabel.setBounds(logoX, 40, newWidth, newHeight);

        panel.add(logoLabel);  // Add the logo label to the panel.
    }

    /**
     * Places buttons and labels inside the form panel.
     *
     * @param panel       The form panel where components will be placed.
     * @param screenWidth Screen width for dynamic layout.
     */
    private void placeComponents(JPanel panel, int screenWidth, int screenHeight) {
        // GridBagConstraints for flexible placement
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);  // Add padding

        // Add user title
        JLabel userTitle = new JLabel("Dashboard");
        userTitle.setFont(new Font("Arial", Font.BOLD, (int) (screenHeight * 0.05)));
        userTitle.setForeground(Color.BLACK);
        panel.add(userTitle, gbc);

        // Add welcome label
        welcomeLabel = new JLabel("Welcome!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, (int) (screenHeight * 0.025)));
        gbc.gridy++;
        panel.add(welcomeLabel, gbc);

        // Add buttons dynamically with GridBagLayout
        JButton liveVideoButton = createButton("Live Video", screenWidth, screenHeight);
        gbc.gridy++;
        panel.add(liveVideoButton, gbc);
        liveVideoButton.addActionListener(e -> {
            // Stop AutoCapture before showing CameraWindow
            if (autoCapture != null) {
                autoCapture.stopCapture();
            }

            new CameraWindow(this);
            setVisible(false);
        });

        JButton viewReportsButton = createButton("View Reports", screenWidth, screenHeight);
        gbc.gridy++;
        panel.add(viewReportsButton, gbc);
        viewReportsButton.addActionListener(e -> {
            new ViewReports();
            setVisible(true);
        });

        JButton optionsButton = createButton("Options", screenWidth, screenHeight);
        gbc.gridy++;
        panel.add(optionsButton, gbc);
        optionsButton.addActionListener(e -> showOptionsDialog());
    }

    /**
     * Creates a button with given properties.
     *
     * @param text        Button label.
     * @param screenWidth The width of the screen used to dynamically calculate the button's width.
     * @return Configured JButton.
     */
    private JButton createButton(String text, int screenWidth, int screenHeight) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, (int) (screenHeight * 0.025)));  // Scale font size
        button.setPreferredSize(new Dimension((int) (screenWidth * 0.15), (int) (screenHeight * 0.05))); // Dynamic width and height
        return button;
    }

    private void showOptionsDialog() {
        // Create text field for autocapture interval and set the current value
        JTextField captureIntervalField = new JTextField(10);
        captureIntervalField.setText(String.valueOf(autoCapture.getCaptureInterval()));  // Get the current autocapture interval

        // Create text field for SQM interval and set the current value
        JTextField sqmIntervalField = new JTextField(10);
        sqmIntervalField.setText(String.valueOf(sqmDisplay.getMeasurementInterval()));  // Get the current SQM measurement interval

        // Panel to display both interval fields, change layout from 2 to 3 rows
        JPanel panel = new JPanel(new GridLayout(3, 1)); // Change from 2 to 3 because there are two intervals
        panel.add(new JLabel("Autocapture Interval (ms):"));
        panel.add(captureIntervalField);
        panel.add(new JLabel("SQM Measurement Interval (ms):"));
        panel.add(sqmIntervalField);

        // Show the dialog with OK and Cancel options
        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Capture Settings",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // If the user clicks OK, process the input
        if (result == JOptionPane.OK_OPTION) {
            try {
                // Get the intervals from the text fields
                int captureInterval = Integer.parseInt(captureIntervalField.getText());
                int sqmInterval = Integer.parseInt(sqmIntervalField.getText());

                // Validate the intervals, they must be at least 1000 ms
                if (captureInterval < 1000) {
                    JOptionPane.showMessageDialog(this, "Autocapture Interval must be at least 1000 ms.");
                    return;
                }
                if (sqmInterval < 1000) {
                    JOptionPane.showMessageDialog(this, "SQM Measurement Interval must be at least 1000 ms.");
                    return;
                }

                // Update the intervals for both autocapture and SQM
                autoCapture.setCaptureInterval(captureInterval);  // Update autocapture interval
                sqmDisplay.setMeasurementInterval(sqmInterval);  // Update SQM measurement interval

                // Reset both processes to apply the new intervals
                autoCapture.reset();  // Reset autocapture
                sqmDisplay.reset();    // Reset SQM

                // Show success message
                JOptionPane.showMessageDialog(this, "Intervals updated successfully!");
            } catch (NumberFormatException e) {
                // Handle invalid input (non-numeric)
                JOptionPane.showMessageDialog(this, "Invalid input! Please enter valid numbers.");
            }
        }
    }
}