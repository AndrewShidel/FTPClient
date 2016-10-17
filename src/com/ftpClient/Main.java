package com.ftpClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {

    private static final String commandsMessage =
            "Below is a list of available commands. Note that all commands are case insensitive.\n\n" +
            "get/retr [file] ->      Retrieves [file] from the server and stores it in the client's working directory.\n" +
            "ls/list         ->      Lists the files and directories in the server's working directory.\n" +
            "cd/cwd [dir]    ->      Changes the server's working directory to [dir].\n" +
            "cdup            ->      Goes up one directory. \"cd ..\" also works.\n" +
            "pwd             ->      Prints out the server's current working directory.\n" +
            "port            ->      Changes to active mode.\n" +
            "pasv            ->      Changes to passive mode.\n" +
            "h/help          ->      Prints this message.\n" +
            "quit            ->      Closes the connection to FTP server.";

    /**
     * A scanner for reading user input.
     */
    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        // Check for correct number of arguments
        if (args.length < 2) {
            System.out.println("Not enough arguments specified.");
            printUsage();
            return;
        }

        // Get the host/ip address
        String host = args[0];
        String ip;
        try {
            ip = InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println(host + " cannot be found.");
            return;
        }

        // Get the path to the log file.
        String logPath = args[1];

        // Get the optional port number. Set default to 21.
        int port = 21;
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[2]);
            } catch(NumberFormatException e) {
                System.out.println(args[2] + " is not a valid port number.");
                return;
            }
        }



        String user = prompt("Enter a username: ");
        String password;
        System.out.println("Enter a password: ");
        if (System.console() != null) {
            password = new String(System.console().readPassword());
        }else{
            password = in.nextLine();
        }


        FtpClient client = new FtpClient(ip, port, logPath);
        client.login(user, password);

        System.out.println();
        printCommands();
        System.out.println();

        // Begin reading user input and executing commands.
        String userInput;
        while( !(userInput = prompt("> ").toLowerCase()).equals("quit") ) {
            if (userInput.isEmpty()) {
              continue;
            } else if (userInput.toLowerCase().startsWith("h")) {
                printCommands();
                continue;
            }
            executeCmd(userInput, client);
        }

        // Close the connection to the server
        client.close();
    }

    /**
     * Interprets the given user input, and executes a command on the client.
     * @param userInput A line of user input. See help message for details.
     * @param client The client to execute commands on.
     */
    private static void executeCmd(String userInput, FtpClient client) {
        String[] parts = userInput.split(" ");
        String command = parts[0].toLowerCase();
        if (client.commandMappings.containsKey(command)) {
            command = client.commandMappings.get(command);
        }

        int i;
        for (i=0; i<client.COMMANDS.length; ++i) {
            if (command.equalsIgnoreCase(client.COMMANDS[i])) {
                break;
            }
        }
        switch(i) {
            case FtpClient.RETR:
                if(parts.length == 1) {
                    System.out.println("The \"" + parts[0] + "\" command must have a file specified. Ex: \"" + parts[0] + " filename.txt\"");
                    return;
                }
                client.getFile(parts[1]);
                break;
            case FtpClient.LIST:
                client.list();
                break;
            case FtpClient.PORT:
                client.setActive();
                break;
            case FtpClient.PASV:
                client.setPasv();
                break;
            case FtpClient.PWD:
                client.pwd();
                break;
            case FtpClient.CWD:
                if (parts.length == 1 && parts[1].trim().isEmpty()) {
                    System.out.println("cd command must have a path specified.");
                    return;
                }
                client.cd(parts[1]);
                break;
            case FtpClient.CDUP:
                client.cdup();
                break;
            default:
                System.out.println("Invalid command " + parts[0]);
        }
    }

    /**
     * Print msg, and returns a line of input from the console.
     */
    private static String prompt(String msg) {
        System.out.print(msg);
        return in.nextLine();
    }

    /**
     * Prints the usage message to the console.
     */
    private static void printCommands() {
        System.out.println(commandsMessage);
    }

    private static void printUsage() {
        System.out.println("Usage: FTPClient ");
    }
}
