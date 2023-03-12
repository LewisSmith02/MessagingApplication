import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    private static final int SERVER_PORT = 5035;
    private static final String SERVER_IP = "localhost";
    private Socket client;
    private BufferedWriter out;
    private BufferedReader in;
    private Object messageObj;
    public static Message message = new Message();
    private String username;
    private String channel;
    public ArrayList<String> subscribedChannels = new ArrayList<>();
    private static final Scanner input = new Scanner(System.in);
    public Client(Socket client, String username, String currentChannel) {
        try {
            this.client = client;
            this.out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.username = username;
            this.channel = currentChannel;

            this.subscribedChannels.add(this.channel);

        } catch (IOException e) {
            closeEverything(client, out, in);
        }
    }

    public void sendMessage () {
        try {
            while (true) {
                // Writes current username, read by ClientHandler constructor
                out.write(username);
                out.newLine();
                out.flush();

                // Sends open Request, read by ClientHandler constructor
                Object openRequest = Message.OpenRequest(channel).toString();
                out.write(openRequest.toString());
                out.newLine();
                out.flush();

                // Writes subscribed channels ArrayList<String> to ClientHandler constructor
                out.write(subscribedChannels.toString());
                out.newLine();
                out.flush();

                while (client.isConnected()) {

                    while (true) {

                        String messageToSend = input.nextLine();

                        // Checking for command input from user, process command if found
                        if (messageToSend.startsWith("/")) {

                            String command;
                            String body = "";
                            String [] parts = messageToSend.split(" ");

                            command = parts[0];

                            if (parts.length > 1) {
                                body = parts[1];
                            }

                            switch(command) {

                                case("/subscribe"):
                                    messageObj = Message.SubscribeRequest(username, body);
                                    // Writes JSON String message to output stream
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;

                                case("/unsubscribe"):
                                    messageObj = Message.UnsubscribeRequest(username, body);
                                    // Writes JSON String message to output stream
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;

                                case("/messages"):
                                    messageObj = Message.GetRequest(username, channel, body);
                                    // Writes JSON String message to output stream
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;

                                case("/channel"):
                                    messageObj = Message.EncodeCommand(command, body, username);
                                    // Writes JSON String message to output stream
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;

                                default:
                                    messageObj = Message.EncodeCommand(command, "", username);
                                    // Writes JSON String message to output stream
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;
                            }
                        }
                        // If no command input from user, process message
                        else {
                            // Setting message object values in message class
                            message.setFrom(username);
                            message.setBody(messageToSend);
                            message.setClass("Message");
                            messageObj = Message.EncodeMessage(messageToSend, channel, "");

                            // Writes JSON String message to output stream
                            out.write(messageObj.toString());
                            out.newLine();
                            out.flush();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(client, out, in);
        }
    }

    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                String messageStr;
                Object messageObj;

                while (client.isConnected()) {
                    try {
                        // Reads incoming messages from ClientHandler's broadcastMessageObject function
                        messageStr = in.readLine();
                        messageObj = messageStr;

                        // If incoming message from ClientHandler is a Request or Response, decoded and output
                        if (messageObj.toString().contains("{")) {
                            Object decodedMessageObj = Message.DecodeMessage(messageObj);
                            System.out.println(decodedMessageObj);
                        }
                        // Else output message from ClientHandler
                        else {
                            System.out.println(messageObj);
                        }

                        // Disconnect client if any exceptions thrown
                    }  catch (IOException e) {
                        e.printStackTrace();
                        closeEverything(client, out, in);
                        break;
                    }
                }
            }
        }).start();
    }

    public void closeEverything (Socket client, BufferedWriter out, BufferedReader in) {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        System.out.println("Welcome! Type /commands to view a list of program commands.");
        System.out.print("Enter desired username: ");
        String clientUsername = input.nextLine();

        String currentChannel = clientUsername;

        System.out.println("Connecting...");
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);

        Client client = new Client(socket, clientUsername, currentChannel);
        System.out.println("Connected!");

        client.listenForMessage();
        client.sendMessage();
    }
}