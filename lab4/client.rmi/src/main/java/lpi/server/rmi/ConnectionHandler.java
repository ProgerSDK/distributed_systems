package lpi.server.rmi;

import java.io.*;
import java.rmi.RemoteException;
import java.util.Arrays;

public class ConnectionHandler implements Closeable {

    private IServer proxy;
    private BufferedReader reader;

    private boolean exit = false;

    // unique session ID (need to login)
    private String sessionId;

    public ConnectionHandler(IServer proxy) {
        this.proxy = proxy;
        this.reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Hello, what do you want to do?");
        System.out.println("Use the \"help\" command to get help.\n");
    }



    public void run() {
        try{
            while(!exit) {
                String[] userCommand = getUserCommand();
                if (userCommand[0].equalsIgnoreCase("exit"))
                    exit = true;

                getResponse(userCommand);
            }

            close();
        } catch (IOException e) {
            e.getStackTrace();
        }
    }



    private String[] getUserCommand() throws IOException {
        String userCommand;
        String[] command;

        while(true) {
            userCommand = this.reader.readLine();

            if (userCommand.length() != 0){
                command = userCommand.split(" ");
                break;
            } else {
                System.out.println("Please enter command!\n");
            }
        }

        return command;
    }





    private void getResponse(String[] command) {
        try{
            switch (command[0]) {
                case "ping":
                    proxy.ping();
                    System.out.println("ping success!\n");
                    break;
                case "echo":
                    String[] echoMessage = Arrays.copyOfRange(command, 1, command.length);
                    System.out.println(proxy.echo(String.join(" ", echoMessage)) + "\n");
                    break;
                case "login":
                    login(command);
                    break;
                case "list":
                    if (loggedIn())
                        list();
                    break;
                case "msg":
                    if (loggedIn())
                        msg(command);
                    break;
                case "file":
                    if (loggedIn())
                        file(command);
                    break;
                case "receive_msg":
                    if (loggedIn())
                        receiveMsg();
                    break;
                case "receive_file":
                    if (loggedIn())
                        receiveFile();
                    break;
                case "exit":
                    if (loggedIn())
                        proxy.exit(sessionId);
                    return;
                case "help":
                    help();
                    break;

                default:
                    System.out.println("Not found this command...\n");
                    break;
            }
        }
        catch (RemoteException e) {
            System.out.println(e.getMessage() + "\n");
        }
    }



    private boolean loggedIn(){
        if (this.sessionId != null){
            return true;
        } else {
            System.out.println("You need to login!\n");
            return false;
        }
    }


    // TODO: check "login" method
    private void login(String[] command) throws RemoteException {
        if (command.length != 3) {
            System.out.println("Wrong params!\n");
            return;
        }

        this.sessionId = proxy.login(command[1], command[2]);
        System.out.println("Login ok.\n");
    }



    // TODO: check "list" method
    private void list() throws RemoteException {
        String[] users = proxy.listUsers(this.sessionId);

        System.out.println("Number of users on the server: " + users.length + ".");
        if (users.length > 0){
            System.out.println("Users:");
            for (String user: users) {
                System.out.println("  "+ user);
            }
            System.out.println();
        }
    }



    // TODO: check "msg" method
    private void msg(String[] command) throws RemoteException {
        if (command.length < 2) {
            System.out.println("You need to enter receiver login!");
            return;
        }
        if (command.length < 3) {
            System.out.println("You need to enter a message!");
            return;
        }

        String[] message = Arrays.copyOfRange(command, 2, command.length);
        proxy.sendMessage(this.sessionId,
                new IServer.Message(command[1], String.join("", message)));
        System.out.println("Message successfully sent.\n");
    }



    // TODO: check "file" method
    private void file(String[] command) throws RemoteException {
        if (command.length < 2) {
            System.out.println("You need to enter receiver login!\n");
            return;
        }
        else if (command.length < 3) {
            System.out.println("You need to enter a path to the file!!\n");
            return;
        }

        File file = new File(command[2]);
        if (!file.isFile()) {
            System.out.println("Incorrect file path or it is not a file.\n");
            return;
        }

        IServer.FileInfo fileInfo = null;
        try {
            fileInfo = new IServer.FileInfo(command[1], file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        proxy.sendFile(this.sessionId, fileInfo);
        System.out.println("File successfully sent.\n");
    }



    // TODO: check "receiveMsg" method
    private void receiveMsg() throws RemoteException {
        IServer.Message receivedMessage =  proxy.receiveMessage(sessionId);
        if (receivedMessage != null) {
            System.out.println("You have a new message!");
            System.out.println("From: " + receivedMessage.getSender());
            System.out.println("Message: " + receivedMessage.getMessage() + "\n");
        } else {
            System.out.println("No messages yet.\n");
        }
    }



    // TODO: write "receiveFile" method
    private void receiveFile() throws RemoteException {
        String folderPath = "./receivedFiles";

        IServer.FileInfo receivedFile =  proxy.receiveFile(sessionId);

        if (receivedFile != null) {
            System.out.println("You have a new file!");
            System.out.println("From: " + receivedFile.getSender());
            System.out.println("File: " + receivedFile.getFilename() + "\n");

            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            try {
                receivedFile.saveFileTo(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No files yet.\n");
        }
    }



    // TODO: write some tips
    private void help(){
        System.out.println("  ping  - test the ability of the source computer to reach a server;");
        System.out.println("  echo  - display line of text/string that are passed as an argument;");
        System.out.println("  login - establish a new session with the server;");
        System.out.println("  list  - list all users on the server;");
        System.out.println("  msg   - send a message to a specific user;");
        System.out.println("  file  - send a file to a specific user;");
        System.out.println("  exit  - close the client.\n");
    }



    @Override
    public void close() throws IOException {
        this.reader.close();
    }
}