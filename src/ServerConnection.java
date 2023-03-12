import java.io.*;
import java.net.Socket;

public class ServerConnection implements Runnable{
    static private Socket server;
    static private BufferedReader in;
    static private BufferedWriter out;
    private String clientUsername;

    public ServerConnection(Socket s) throws IOException {
        server = s;
        in = new BufferedReader(new InputStreamReader(server.getInputStream()));
    }

    @Override
    public void run() {
        while (true) {
            try {
                out.write(clientUsername + " has connected to the server!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
