import java.io.IOException;

public class Commands {

    public static Object executeCommand(String commandToExecute) throws IOException {

        String[] parts = commandToExecute.split(" ");
        String command = parts[0];
        String body = "";
        String username = "";

        if (parts.length == 2) {
            username = parts[1];
        }
        else if (parts.length == 3) {
            body = parts[1];
            username = parts[2];
        }

        ClientHandler clientHandler = new ClientHandler();

        switch(command) {

            case("/commands"):

                return clientHandler.viewCommands();

            case ("/channel"):

                return clientHandler.changeCurrentChannel(body, username);

            case ("/subscribe"):

                return clientHandler.subscribeToChannel(body, username);

            case ("/unsubscribe"):

                return clientHandler.unsubscribeFromChannel(body, username);

            case("/messages"):

                return clientHandler.getMessages(body, username);

            case("/publish"):

                return clientHandler.isPublishRequestValid(body, username);

            case ("/viewclients"):

                return clientHandler.viewAllClientUsernames();

            case ("/viewsubscribed"):

                return clientHandler.viewSubscribed(username);

            case ("/viewcurrent"):

                return clientHandler.viewCurrentChannel(username);

            default:
                return ("COMMAND NOT RECOGNISED!");
        }
    }
}
