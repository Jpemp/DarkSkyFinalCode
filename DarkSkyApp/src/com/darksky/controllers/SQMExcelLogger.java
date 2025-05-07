package com.darksky.controllers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xddf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * File Header Comment:
 * The SQMExcelLogger class is responsible for logging sky quality measurements into an Excel file.
 * It also generates a graph to visualize the data over time.
 */

/**
 * Class Header Comment:
 * This class handles writing SQM (Sky Quality Meter) data to an Excel file.
 * If the file does not exist, it creates one, adding a new entry for each measurement.
 */
public class SQMExcelLogger {
    private static final String FILE_NAME = System.getProperty("user.home") + "/Documents/DarkSky Reports/";

    /**
     * Method Header Comment:
     * Saves the received measurement data into an Excel file, including a graph.
     * @param measurement The SQM data received as a comma-separated string.
     */
    public static void saveMeasurementToExcelWithGraph(String measurement) {
        try {
            // Split the response into individual values
            String[] values = measurement.split(",");

            if (values.length < 6) { // Ensures the data has at least 6 values before proceeding.
                System.out.println("Invalid data format received from SQM.");
                return;
            }

            // Get the current date to organize files by day
            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String filePath = FILE_NAME + currentDate + "/SQM-Measurements-With-Graph.xlsx";

            // Ensure the folder exists before writing the file
            Path folderPath = Paths.get(FILE_NAME + currentDate);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // Open the existing Excel file or create a new one if it does not exist
            File file = new File(filePath);
            Workbook workbook;
            XSSFSheet sheet;

            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = new XSSFWorkbook(fis);
                    sheet = (XSSFSheet) workbook.getSheet("Measurements");
                } catch (IOException e) {
                    e.printStackTrace();
                    return; // Exit if the file cannot be opened
                }
            } else {
                workbook = new XSSFWorkbook();
                sheet = (XSSFSheet) workbook.createSheet("Measurements");

                // Create and set up the header row
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Timestamp", "Response", "Sky Brightness (mag/arcsec²)",
                        "Frequency (Hz)", "Period (counts)", "Period (s)", "Temperature (°C)"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
            }

            // Find the last row and append new data
            int lastRow = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(lastRow);

            row.createCell(0).setCellValue(new SimpleDateFormat("HH:mm:ss").format(new Date())); // Timestamp
            row.createCell(1).setCellValue(values[0].trim()); // Raw response from SQM

            // Extract the sky brightness value, removing the 'm' character if present
            String skyBrightnessStr = values[1].trim().replace("m", "");
            try {
                double skyBrightness = Double.parseDouble(skyBrightnessStr); // Convert string to number
                row.createCell(2).setCellValue(skyBrightness); // Store as numeric value in Excel
            } catch (NumberFormatException e) {
                System.out.println("Invalid sky brightness value: " + skyBrightnessStr);
                row.createCell(2).setCellValue(skyBrightnessStr); // Store as a string if parsing fails
            }

            // Add the remaining values from the measurement array
            for (int i = 2; i < values.length; i++) {
                row.createCell(i + 1).setCellValue(values[i].trim());
            }

            // Create a drawing object for adding a graph to the sheet
            XSSFDrawing drawing = sheet.createDrawingPatriarch();

            // Define where the graph should be placed
            XSSFClientAnchor anchor = new XSSFClientAnchor();
            anchor.setCol1(8); // Chart starting column
            anchor.setRow1(0);
            anchor.setCol2(18); // Chart ending column
            anchor.setRow2(20);

            // Create the chart itself
            XDDFChart chart = (XDDFChart) drawing.createChart(anchor);

            // Set the chart title
            chart.setTitleText("Sky Brightness vs. Time");
            chart.setTitleOverlay(false);

            // Define X and Y axes
            XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            xAxis.setTitle("Timestamp");

            XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
            yAxis.setTitle("Sky Brightness (mag/arcsec²)");

            // Create the data set for the chart
            XDDFChartData data = chart.createData(ChartTypes.LINE, xAxis, yAxis);

            // Select data range for the X-axis (timestamps)
            int rowStart = 1; // First row with data
            int rowEnd = lastRow; // Last row with data
            XDDFDataSource<String> xData = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                    new CellRangeAddress(rowStart, rowEnd, 0, 0));

            // Select data range for the Y-axis (sky brightness values)
            XDDFNumericalDataSource<Double> yData = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(rowStart, rowEnd, 2, 2));

            // Add the series to the chart
            XDDFChartData.Series series = data.addSeries(xData, yData);
            series.setTitle("Sky Brightness", null);

            // Add data to the chart
            chart.plot(data);

            // Save the updated workbook to the file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}