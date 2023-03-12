    import org.json.simple.parser.JSONParser;
    import org.json.simple.parser.ParseException;

    import java.io.*;
    import java.net.Socket;
    import java.net.SocketException;
    import java.util.*;

    public class ClientHandler implements Runnable, Serializable {
        private Socket clientSocket;
        private BufferedWriter out;
        private BufferedReader in;
        private final ArrayList<String> subscribedChannels = new ArrayList<>();
        private static final HashMap<String, ClientHandler> clientHandlers = new HashMap<>();
        private String clientUsername;
        private String currentChannel;
        private String subscribedChannelsListStr;
        private Boolean hasChannelChanged = false;

        public ClientHandler(Socket client) throws IOException {
            try {
                this.clientSocket = client;
                this.out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                this.clientUsername = in.readLine();

                // Receive 'openRequest' and create channel by user identity
                String openRequest = in.readLine();
                System.out.println(openRequest);
                Object decodedOpenRequest = Message.DecodeMessage(openRequest);
                this.currentChannel = (String) decodedOpenRequest;

                Object successResponseObj = Message.SuccessResponse("CHANNEL CREATED USING USERNAME!");
                System.out.println(successResponseObj);

                Object obj = successResponseObj;
                String str = String.valueOf(obj);

                out.write(str);
                out.newLine();
                out.flush();

                // Take String of subscribedChannels
                subscribedChannelsListStr = in.readLine();
                subscribedChannelsListStr = subscribedChannelsListStr.replaceAll("\\[", "").replaceAll("\\]","");
                // Split channels by ", " and add to list individually
                String[] array;
                array=subscribedChannelsListStr.split(", ");
                Collections.addAll(subscribedChannels, array);

                clientHandlers.put(this.clientUsername, this);
                notifyUserJoinedChannel();

                // Disconnect client if any exceptions thrown
            } catch (IOException e) {
                e.printStackTrace();
                closeEverything(clientSocket, in, out);
            }
        }
        public ClientHandler(){
        }
        public String viewCommands() {
            return ("<|USER COMMANDS|> \n" +
                    "/channel [channel to switch to] - Changes users current channel to channel entered.\n" +
                    "/subscribe [channel to subscribe to] - Subscribes user to channel entered, changes current channel to newly subscribed channel.\n" +
                    "/unsubscribe [channel to unsubscribe from] - Unsubscribes user from channel entered, changes current channel to default (username) channel.\n" +
                    "/publish [message to publish to channel] - Publishes message to users current channel.\n" +
                    "/viewclients - Lists all clients currently connected to the server.\n" +
                    "/viewsubscribed - Lists all user's subscribed channels.\n" +
                    "/viewcurrent - Shows user's current channel.");
        }
        public String changeCurrentChannel(String newCurrentChannel, String username) throws IOException {

            ClientHandler currentClient = clientHandlers.get(username);
            if (currentClient.subscribedChannels.contains(newCurrentChannel)) {
                currentClient.currentChannel = newCurrentChannel;

                return ("NEW CURRENT CHANNEL: " + newCurrentChannel);
            }
            else {
                return ("ERROR (REASON: NOT SUBSCRIBED TO CHANNEL)");
            }
        }
        public Object getMessages(String previousMessages, String username) throws FileNotFoundException {
            ArrayList<String> previousMessageArray = Message.getMessageListFormatted(Integer.parseInt(previousMessages), username);
            ArrayList<String> messagesToReturn = new ArrayList<>();
            for (int i = 0; i < previousMessageArray.size(); i++) {
                String message = previousMessageArray.get(i);
                if (message.contains("[" + username + "]")) {
                    messagesToReturn.add(message);
                }
            }
            return Message.MessageListResponse(messagesToReturn);
        }
        public Object setTo(Object messageToModify, String broadcastChannel) throws IOException {
            Object decodedMessageToModify = Message.DecodeMessage(messageToModify);
            String messageToSend = (String) decodedMessageToModify;
            String modifiedMessageToSend = messageToSend.substring(messageToSend.lastIndexOf(":") +2);

            return Message.EncodeMessage(modifiedMessageToSend, broadcastChannel, "");
        }
        public String viewCurrentChannel(String username) {
            ClientHandler currentClient = clientHandlers.get(username);
            return ("CURRENT CHANNEL: " + currentClient.currentChannel);
        }
        public String viewAllClientUsernames() {
            return ("CURRENT CONNECTED CLIENTS: " + clientHandlers.keySet());
        }
        public String viewSubscribed(String username) {
            // View client's subscribed channels
            return ("CURRENT SUBSCRIBED CHANNELS: " + getClientSubscribedChannels(username));
        }
        public ArrayList<String> getClientSubscribedChannels(String username) {
            ClientHandler currentClient = clientHandlers.get(username);
            return currentClient.subscribedChannels;
        }

        public Object isPublishRequestValid(String username, String channel) {
            ClientHandler currentClient = clientHandlers.get(username);
            if (currentClient.subscribedChannels.contains(channel)) {
                return Message.SuccessResponse("PUBLISHABLE");
            }
            else {
                return Message.ErrorResponse("NOT PUBLISHABLE (REASON: CHANNEL DOESN'T EXIST)");
            }
        }
        public Object unsubscribeFromChannel(String oldChannel, String username) throws IOException {
            // Unsubscribe from a channel, will switch client to first channel in their subscribed ArrayList
            // If no channels to switch to, "default" channel is added and user is entered into that
            // Can no longer receive messages from the unsubscribed channel
            if (oldChannel.equals(clientUsername)) {
                return Message.ErrorResponse("UNABLE TO UNSUBSCRIBE (REASON: CANNOT UNSUBSCRIBE FROM HOST CHANNEL)");
            }

            ClientHandler currentClient = clientHandlers.get(username);

            if (currentClient.subscribedChannels.contains(oldChannel)) {
                currentClient.subscribedChannels.remove(oldChannel);
                currentClient.currentChannel = currentClient.subscribedChannels.get(0);

                notifyUserLeftChannel(currentClient.clientUsername, oldChannel);

                return Message.SuccessResponse("UNSUBSCRIBED FROM: '" + oldChannel + "' NEW CURRENT CHANNEL: '" + currentClient.currentChannel + "'");
            }
            else {
                return Message.ErrorResponse(" UNABLE TO UNSUBSCRIBE (REASON: NOT SUBSCRIBED)");
            }
        }
        public Object subscribeToChannel(String newChannel, String username) throws IOException {
            // Subscribe to another channel, switches client to newly subscribed channel
            // Can still receive messages from other subscribed channel whilst not "in" them
            Boolean channelExists = false;

            if (clientHandlers.containsKey(newChannel)) {
                channelExists = true;
            }

            if (channelExists) {
                ClientHandler currentClient = clientHandlers.get(username);
                currentClient.currentChannel = newChannel;
                currentClient.subscribedChannels.add(newChannel);

                notifyUserJoinedNewChannel(currentClient.clientUsername, currentClient.currentChannel);

                return Message.SuccessResponse("JOINED: " + newChannel);
            }
            else {
                return Message.ErrorResponse("UNABLE TO SUBSCRIBE (REASON: CHANNEL DOESN'T EXIST)");
            }
        }

        private void notifyUserJoinedChannel() throws IOException {
            String connectMsgStr = (clientUsername + " has entered the chat!");
            Object connectMsgObj = Message.EncodeMessage(connectMsgStr, currentChannel, "Message");
            broadcastMessage(connectMsgObj, currentChannel);
            hasChannelChanged = false;
        }
        private void notifyUserJoinedNewChannel(String username, String channel) throws IOException {
            String connectMsgStr = (username + " has entered the chat!");
            Object connectMsgObj = Message.EncodeMessage(connectMsgStr, channel, "Message");
            broadcastMessage(connectMsgObj, channel);
            hasChannelChanged = false;
        }
        private void notifyUserLeftChannel(String username, String channel) throws IOException {
            String leaveMsgStr = (username + " has left the chat!");
            Object leaveMsgObj = Message.EncodeMessage(leaveMsgStr, channel, "Message");
            broadcastMessage(leaveMsgObj, channel);
            hasChannelChanged = false;
        }

        @Override
        public void run() {

            String messageStr;
            Object messageObj = "";

            Object _class;
            Object command;

            while (clientSocket.isConnected()) {
                try {

                    while (true) {
                        try {
                            messageStr = in.readLine();
                            messageObj = messageStr;
                        } catch (SocketException e) {
                            System.out.println("[SERVER] User: '" + this.clientUsername + "' has disconnected!");
                            closeEverything(clientSocket, in, out);
                            break;
                        }

                        // Outputting received request to Server console
                        System.out.println(messageObj.toString());

                        _class = Message.getObjectClass(messageObj);

                        try {
                            switch (_class.toString()) {

                                case("GetRequest"):
                                    command = Message.DecodeMessage(messageObj.toString());
                                    if (command.toString().startsWith("/")) {
                                        messageObj = Commands.executeCommand(command.toString());
                                    }
                                    else {
                                        messageObj = command.toString();
                                    }
                                    System.out.println(messageObj);
                                    // Writing to client
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;

                                case("SubscribeRequest"):
                                    command = Message.DecodeMessage(messageObj.toString());
                                    if (command.toString().startsWith("/")) {
                                        messageObj = Commands.executeCommand(command.toString());
                                    }
                                    else {
                                        messageObj = command.toString();
                                    }
                                    // Writing request to Server & Console
                                    System.out.println(messageObj);
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;

                                case("UnsubscribeRequest"):
                                    command = Message.DecodeMessage(messageObj.toString());
                                    if (command.toString().startsWith("/")) {
                                        messageObj = Commands.executeCommand(command.toString());
                                    }
                                    else {
                                        messageObj = command.toString();
                                    }
                                    // Writing request to Server & Console
                                    System.out.println(messageObj);
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;

                                case("Message"):
                                    Object newMessageObj = setTo(messageObj, this.currentChannel);
                                    // Writing request to Server & broadcasting to Clients
                                    System.out.println(newMessageObj);
                                    broadcastMessage(newMessageObj, this.currentChannel);
                                    break;

                                case("Command"):
                                    command = Message.DecodeMessage(messageObj);
                                    messageObj = Commands.executeCommand(command.toString());

                                    // Writing request to client
                                    out.write(messageObj.toString());
                                    out.newLine();
                                    out.flush();
                                    break;

                                default:
                                    messageObj = ("INVALID _class!");
                                    System.out.println(messageObj);
                                    broadcastMessage(messageObj, this.currentChannel);
                                    break;
                            }
                        } catch (NullPointerException e) {
                            break;
                        }
                    }

                    // Disconnect client if any exceptions thrown
                } catch (IOException e) {
                    try {
                        closeEverything(clientSocket, in, out);
                        break;
                    } catch (IOException ex) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }

        private void broadcastMessage (Object messageObj, String broadcastChannel) throws IOException {

            List<ClientHandler> handlers = new ArrayList<>(clientHandlers.values());

            for (ClientHandler aClient : handlers) {
                try {
                    if (aClient != null && aClient.subscribedChannels.contains(broadcastChannel)) {
                        {
                            aClient.out.write(messageObj.toString());
                            aClient.out.newLine();
                            aClient.out.flush();
                        }
                    }
                    // Disconnect client if any exceptions thrown
                } catch (IOException e) {
                    closeEverything(clientSocket, in, out);
                    break;
                }
            }
        }

        private void removeClientHandler() throws IOException {
            clientHandlers.remove(this.clientUsername);
            notifyUserLeftChannel(this.clientUsername, this.currentChannel);
        }
        public void closeEverything (Socket clientSocket, BufferedReader in,  BufferedWriter out) throws IOException {
            removeClientHandler();
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
