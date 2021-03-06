package client.tcp;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;


public class ConnectionHandler implements Closeable{
    private Socket socket;
    private CommandManager commandManager;
    private volatile boolean isClosing = false;
    private UserInput userInput;

    DataInputStream readStream;
    DataOutputStream outputStream;

    public ConnectionHandler(Socket socket, CommandManager commandManager) {
        this.socket = socket;
        this.commandManager = commandManager;
        this.userInput = new UserInput();

        try {
            readStream = new DataInputStream(this.socket.getInputStream());
            outputStream = new DataOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        close(false);
    }

    private void close(boolean selfClose) {
        this.isClosing = true;

        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.socket = null;
        }
    }

    public void run() {
        try {
            MyThread myThread = null;

            while (!this.isClosing) {
                byte[] request;

                String[] userCommand = userInput.getUserCommand();

                if (userCommand.length == 0) {
                    continue;
                }

                // If user enter "exit"
                if (userCommand[0].getBytes()[0] == Const.CMD_EXIT) {
                    close();
                    if (myThread != null)
                        myThread.interrupt();
                    break;
                }

                // Get the current command for decoding server response in the future
                byte currentCommand = userCommand[0].getBytes()[0];

                // Get encoded data for sending
                request = commandManager.execute(userCommand);
                if (request.length == 0)
                    continue;

                // Sending Request to the server.
                commandManager.sendRequest(request, this.outputStream);

                // Wait for the server to read the socket messages and reply
                for (int i = 0; i< 3; i++) {
                    System.out.print(".");
                    Thread.sleep(333);
                }
                System.out.println();

                // Get a response from the server using the method
                byte[] response = commandManager.receiveResponse(this.readStream);

                // Run a new thread to retrieve messages and files,
                // and check which users are logged in to the server.
                // Perform these actions only when the user logs in to the server.
                if (response[0] == (byte)6 || response[0] == (byte)7) {
                    myThread = new MyThread(this.commandManager, this.socket);
                    myThread.start();
                }

                // decode the response
                this.commandManager.decode(currentCommand, response);
            }
        }
        catch (IOException | InterruptedException ex) {
            ex.getStackTrace();
        }
    }
}



class MyThread extends Thread {

    private CommandManager commandManager;

    private byte[] receiveMsgRequest;
    private byte[] receiveFileRequest;
    private byte[] receiveListRequest;
    Socket socket;

    DataInputStream readStream;
    DataOutputStream outputStream;
    ArrayList<String> currUsers = new ArrayList<>();
    ArrayList<String> oldUsers = new ArrayList<>();

    private boolean isEncoded = false;

    MyThread(CommandManager commandManager, Socket socket) {
        this.commandManager = commandManager;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.readStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());

            if (!isEncoded) {
                receiveMsgRequest = new byte[] {25};
                receiveFileRequest = new byte[] {30};
                receiveListRequest = new byte[] {10};

                isEncoded = true;
            }

            while (!socket.isClosed()){
                try {
                    checkUsers();
                    runCommand(receiveMsgRequest);
                    runCommand(receiveFileRequest);

                }catch (InterruptedException e) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void runCommand(byte[] cmd) throws InterruptedException, IOException {
        Thread.sleep(2000);
        commandManager.sendRequest(cmd, this.outputStream);
        Thread.sleep(500);

        // TODO: write comments
        byte[] response = commandManager.receiveResponse(this.readStream);
        // and decode it
        this.commandManager.decode(cmd[0], response);
    }


    private void checkUsers() throws IOException, InterruptedException {
        Thread.sleep(1000);
        commandManager.sendRequest(receiveListRequest, this.outputStream);
        Thread.sleep(500);

        // TODO: write comments
        byte[] response = commandManager.receiveResponse(this.readStream);
        // and decode it
        String[] usersOnServer = commandManager.getActiveUsers(response);

        if (usersOnServer.length != 0){
            if (oldUsers.size() == 0) {
                for (String user: usersOnServer) {
                    oldUsers.add(user);
                    System.out.println(user + " is logged in.");
                    System.out.println();
                }
            } else {
                // Add all users to currentUsers
                currUsers.addAll(Arrays.asList(usersOnServer));

                for (String user: currUsers) {
                    if (!oldUsers.contains(user)) {
                        System.out.println(user + " is logged in.");
                    }
                }

                for (String user: oldUsers) {
                    if (!currUsers.contains(user)) {
                        System.out.println(user + " is logged out.");
                    }
                }

                oldUsers = (ArrayList<String>) currUsers.clone();
                currUsers.clear();
            }
        }
    }
}
