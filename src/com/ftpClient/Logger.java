package com.ftpClient;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * A simple class for handling logging to a file.
 */
public class Logger {
    private boolean tee = false;
    private final Writer out;

    /**
     * Opens the log file for writing.
     * @param outputFile The path to the output file
     * @throws IOException If there is an error opening the output file
     */
    public Logger(String outputFile) throws IOException {
        out = new FileWriter(outputFile);
        out.write(getTime() + " (SUCCESS) Opened log file: " + outputFile + "\n");
    }

    /**
     * Opens the standard output for writing.
     */
    public Logger() {
        out = new OutputStreamWriter(System.out);
    }

    /**
     * Should be called when the Logger is done being used. Closes the log file.
     * @throws IOException If there is an issue closing the log file.
     */
    public void close() throws IOException {
        out.close();
    }

    /**
     * Writes the msg to the log file with success as the status.
     */
    public void log(String msg) {
        log(msg, false);
    }

    /**
     * Writes the msg to the log file.
     */
    public void log(String msg, boolean error) {
        String time = getTime();
        String status = error?"(ERROR)":"(SUCCESS)";

        if (tee) {
            System.out.print(msg + "\n");
        }

        try {
            String message = time + " " + status + " " + msg + "\n";
            out.write(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private String getTime() {
        return "9/25/16 20:00:00.0002";
    }
}