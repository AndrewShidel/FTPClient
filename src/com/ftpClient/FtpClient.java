package com.ftpClient;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FtpClient {
    private boolean pasv = true;
    private boolean useIP6 = false;
    private boolean useExtended = true;
    private final int port;
    private int dataPort;
    private final String ip;
    private Socket serverSocket;
    private ServerSocket activeSocket;
    private Logger logger;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * This is the list of commands that can be sent to the FTP Server
     */
    public static final String[] COMMANDS = {
        "USER", "PASS", "QUIT", "LIST", "PORT", "PASV", "RETR", "PWD", "CWD", "CDUP", "EPRT", "EPSV"
    };

    /**
     * Use command mappings to allow for alternate ways of expressing a command.
     */
    public static Map<String, String> commandMappings;
    static {
        commandMappings = new HashMap<String, String>();
        commandMappings.put("ls", "list");
        commandMappings.put("cd", "cwd");
        commandMappings.put("get", "retr");
    }

    // A list of indices into the COMMANDS array.
    public static final int USER = 0;
    public static final int PASS = 1;
    public static final int QUIT = 2;
    public static final int LIST = 3;
    public static final int PORT = 4;
    public static final int PASV = 5;
    public static final int RETR = 6;
    public static final int PWD  = 7;
    public static final int CWD = 8;
    public static final int CDUP = 9;
    public static final int EPRT = 10;
    public static final int EPSV = 11;

    // The Socket and Reader to get passive data from
    private Socket pasvSocket;
    private BufferedReader pasvIn;


    /**
     * Creates a new FtpClient, and opens the connection to the server.
     * @param ip IP address of the server.
     * @param port Server's port (usually 21).
     * @param outputFile The log file to write to.
     */
    public FtpClient(String ip, int port, String outputFile) {
        this.ip = ip;
        this.port = port;

        try {
            this.logger = new Logger(outputFile);
        } catch (IOException e) {
            this.logger = new Logger();
            logger.log("Could not open " + outputFile + " for writing, defaulting to standard out.", true);
        }

        open();
    }

    /**
     * Opens a socket to the server.
     */
    private void open() {
        logger.log("Opening connection on " + ip + ":" + port);
        try {
            serverSocket = new Socket(ip, port);
            out = new PrintWriter(serverSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            System.out.println(getLine());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not connect to " + ip + ":" + port);
        }
    }

    /**
     * Closes the connection to the server. Should be called before exiting.
     */
    public void close() {
        logger.log("Closing connection to FTP server");
        try {
            out.println(COMMANDS[QUIT]);
            System.out.println(getLine());
            out.close();
            in.close();
            serverSocket.close();
            logger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs into the FTP server with the user and password. Must be called before performing any other operations.
     */
    public void login(String user, String pass) {
        logger.log("Logging in for " + user);
        out.println(COMMANDS[USER] + " " + user);
        System.out.println(getLine());

        logger.log("USER command sent");

        out.println(COMMANDS[PASS] + " " + pass);
        System.out.println(getLine());

        logger.log("PASS command sent");
    }

    /**
     * Sets up a data transfer with default method.
     */
    public void setupTransfer() {
        setupTransfer(pasv);
    }

    /**
     * Sets up a data transfer with the specified method
     * @param pasv True for passive, false for active
     */
    public void setupTransfer(boolean pasv) {
        if (pasv) {
            setPasv();
        }else{
            setActive();
        }
    }

    /**
     * Closes the data transfer. Must be called before beginning a new transfer.
     */
    public void closeTransfer() {
        logger.log("Closing the data transfer.");
        try {
            if (pasv) {
                pasvSocket.close();
                pasvIn.close();
            } else {
                activeSocket.close();
            }
        }catch (IOException e) {
            logger.log("Error closing the file transfer connection: " + e.getMessage(), true);
        }
        System.out.println(getLine());
    }

    /**
     * Starts a passive transfer.
     */
    public void setPasv() {
        logger.log("Starting a passive data transfer.");
        pasv = true;
        String pasvCMD;
        if (useExtended) {
            pasvCMD = COMMANDS[EPSV] + " " + (useIP6?2:1);
        }else{
            pasvCMD = COMMANDS[PASV];
        }

        out.println(pasvCMD);
        String result = getLine();
        logger.log(result);

        if (!result.startsWith(useExtended?"229":"227")) {
            abort("Error opening passive connection with FTP server: " + result);
        }
        String splitRegex = useExtended?"\\|":",";
        String[] parts = result.split(splitRegex);

        int port;

        if (useExtended) {
            port = Integer.parseInt(parts[3]);
        } else {
            port = Integer.parseInt(parts[4]) * 256 +
                    Integer.parseInt(parts[5].replaceAll("[^\\d]", ""));
        }

        try {
            pasvSocket = new Socket(ip, port, null, 0);
            pasvIn = new BufferedReader(
                    new InputStreamReader(pasvSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            abort("Error creating a data port: " + e.getMessage());
        }
    }

    /**
     * Starts an active transfer.
     */
    public void setActive() {
        logger.log("Starting an active data transfer.");
        pasv = false;
        try {
            activeSocket = new ServerSocket(0);
        } catch (IOException e) {
            logger.log("Error creating a data port. Aborting.", true);
            close();
            System.exit(1);
        }
        byte[] address = serverSocket.getLocalAddress().getAddress();
        int port = activeSocket.getLocalPort();
        String portCmd;
        if (useExtended) {
            try {
                portCmd = COMMANDS[EPRT] + " |" +
                        (useIP6 ? 2 : 1) + "|" +
                        InetAddress.getLocalHost().getHostAddress() + "|" +
                        port + "|";
            }catch (UnknownHostException e) {
                logger.log("Could not find local ip address: " + e.getMessage(), true);
                return;
            }
        } else {
            portCmd = COMMANDS[PORT] + " " +
                    (address[0]&0xFF) + "," +
                    (address[1]&0xFF) + "," +
                    (address[2]&0xFF) + "," +
                    (address[3]&0xFF) + "," +
                    (port / 256) + "," +
                    (port % 256);
        }
        System.out.println(portCmd);

        out.println(portCmd);
        System.out.println(getLine());
    }

    /**
     * Retrieves a file from the FTP server, and stores it in the client's working directory.
     * @param path The absolute or relative path to the file on the FTP server.
     */
    public void getFile(String path) {
        logger.log("Retrieving " + path + " from FTP server.");
        setupTransfer();

        out.println(COMMANDS[RETR] + " " + path);
        String response = getLine();

        if (!response.startsWith("150")) {
            if (response.startsWith("550")) {
                logger.log(path + " could not be found on the FTP server.", true);
            }else {
                logger.log("Invalid response from FTP server for file transfer: " + response, true);
            }
            return;
        }

        System.out.println(response);

        try {
            Socket activeSocketTmp = null;
            InputStream input;
            if (pasv) {
                input = pasvSocket.getInputStream();
            } else {
                activeSocketTmp = activeSocket.accept();
                input = activeSocketTmp.getInputStream();
            }

            BufferedInputStream inStream = new BufferedInputStream(input);
            FileOutputStream outStream = new FileOutputStream(
                    new File(path).getName());
            int byteCount;
            byte[] bytes = new byte[512];

            while ( (byteCount = inStream.read(bytes, 0, 512)) != -1) {
                outStream.write(bytes, 0, byteCount);
            }

            outStream.close();
            inStream.close();
            if (activeSocketTmp != null) {
                activeSocketTmp.close();
            }
        } catch (IOException e) {
            logger.log("Error getting " + path + " from FTP server: " + e.getMessage(), true);
        }

        closeTransfer();
    }

    /**
     * Lists the current working directory.
     */
    public void pwd() {
        logger.log("Sending PWD to server.");
        out.println(COMMANDS[PWD]);
        System.out.println(getLine());
    }

    /**
     * Changes the working directory.
     * @param path The path to move to.
     */
    public void cd(String path) {
        logger.log("Sending CWD " + path + " to server.");
        out.println(COMMANDS[CWD] + " " + path);
        System.out.println(getLine());
    }

    /**
     * Go up one directory.
     */
    public void cdup() {
        logger.log("Sending CDUP to server.");
        out.println(COMMANDS[CDUP]);
        System.out.println(getLine());
    }

    /**
     * Lists the files in the working directory.
     */
    public void list() {
        logger.log("Sending list to server.");
        setupTransfer();

        out.println(COMMANDS[LIST]);
        System.out.println(getLine());

        BufferedReader in;
        Socket activeSocket = null;

        try {
            if (pasv) {
                in = pasvIn;
            } else {
                activeSocket = this.activeSocket.accept();
                in = new BufferedReader(new InputStreamReader(activeSocket.getInputStream()));
            }

            String line;
            while( (line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();
            if (activeSocket != null) {
                activeSocket.close();
            }

        }catch (IOException e) {
            e.printStackTrace();
        }
        closeTransfer();
        logger.log("Finished retrieving list from server.");
    }

    /**
     * Read the next line from the FTP server. Hangs if no line is available.
     * @return The line (not including newline), "" if there is nothing in the stream,
     *          or null if end if stream is reached or if there was a read error.
     */
    private String getLine() {
        try {
            String line = in.readLine();
            logger.log("Received: " + line);
            return line;
        } catch (IOException e) {
            logger.log("Error reading response from server: " + e.getMessage(), true);
        }
        return null;
    }

    /**
     * Logs a message, closes the FTP connection, and exits the program.
     * @param msg The error message to show to the user.
     */
    private void abort(String msg) {
        logger.log(msg, true);
        System.out.println(msg);
        close();
        System.exit(1);
    }
}
