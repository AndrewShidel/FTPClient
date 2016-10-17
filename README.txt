This README file specifies how compile and run the source code for the FTP client.


Project Structure:
    - ./build.xml                    => The ant build file. See “Compiling” section for more details.
    - ./FTPClient                    => A Bash script that will run the project.
    - ./src/com/ftpClient/FtpClient.java => The Ftp Client library. All ftp functionality is in this file.
    - ./src/com/ftpClient/Logger.java    => A class to help with generating the log file.
    - ./src/com/ftpClient/Main.java      => The main class that contains the main method. This class is in charge of the user interface, and with interpreting commands.


Compiling
Run: “ant all”


Running:
Run: “./FTPClient [ip address] logs.out 21”




Below is a list of available commands. Note that all commands are case insensitive.


get/retr [file] ->      Retrieves [file] from the server and stores it in the client's working directory.
ls/list         ->      Lists the files and directories in the server's working directory.
cd/cwd [dir]    ->      Changes the server's working directory to [dir].
cdup            ->      Goes up one directory. "cd .." also works.
pwd             ->      Prints out the server's current working directory.
port            ->      Changes to active mode.
pasv            ->      Changes to passive mode.
h/help          ->      Prints this message.
quit            ->      Closes the connection to FTP server.
