import com.sun.net.httpserver.Authenticator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Message implements Serializable {
    private static String identity;
    private static String channel;
    private static String body;
    private static String from;
    private static String to;
    private static String when;
    private static JSONArray messages;
    private static String message;
    private static String previousMessages;
    private static String error;
    private static String success;
    public static String currentFileChannel;
    private static BufferedWriter file;
    private static Scanner reader;
    private static ArrayList<String> messageList = new ArrayList<String>();
    private static ArrayList<String> formattedMessageList = new ArrayList<>();
    private static String _class = Message.class.getSimpleName();
    public void setBody(String message) {
        body = message;
    }
    public void setFrom(String username) {
        from = username;
    }
    public void setClass(String message) { _class = message; }

    public static ArrayList<String> getMessageList(int numberOfMessages, String username) throws FileNotFoundException {
        ArrayList<String> messageList = new ArrayList<>();
        Scanner reader = new Scanner(new File("messagelog.json"));
        for (int i = 0; i < numberOfMessages; i++) {
            if (reader.hasNextLine()) {
                messageList.add(reader.nextLine());
            }
        }
        reader.close();
        if (messageList.isEmpty()) {
            Object errorResponseObj = Message.SuccessResponse("MESSAGES UNABLE TO BE RETRIEVED (REASON: MESSAGE LIST EMPTY)");
            System.out.println(errorResponseObj);
        }
        return messageList;
    }
    public static ArrayList<String> getMessageListFormatted(int numberOfMessages, String username) throws FileNotFoundException {
        messageList = getMessageList(numberOfMessages, username);
        formattedMessageList = new ArrayList<>();
        for (String message : messageList) {
            String from = message.substring(message.indexOf("from") + 7, message.indexOf("_class") - 3);
            String body = message.substring(message.indexOf("body") + 7, message.indexOf("when") - 3);
            String when = message.substring(message.indexOf("when") + 7, message.indexOf("}") - 1);
            String to = message.substring(message.indexOf("to") + 5, message.indexOf("body") - 3);
            currentFileChannel = channel;

            String formattedMessage = "[" + to + "] " + when + " " + from + " : " + body;
            formattedMessageList.add(formattedMessage);
        }
        return formattedMessageList;
    }
    public static Object GetRequest(String username, String currentChannel, String numberOfMessages) {
        JSONObject obj = new JSONObject();
        _class = "GetRequest";
        identity = username;
        from = currentChannel;
        previousMessages = numberOfMessages;

        obj.put("_class", _class);
        obj.put("identity", identity);
        obj.put("from", from);
        obj.put("previousMessages", previousMessages);

        return (obj);
    }
    public static Object OpenRequest(String currentChannel) {
        JSONObject obj = new JSONObject();
        _class = "OpenRequest";
        identity = currentChannel;

        obj.put("_class", _class);
        obj.put("identity", identity);

        return(obj);
    }
    public static Object SubscribeRequest(String clientUsername, String channelToSubscribeTo) throws IOException {
        JSONObject obj = new JSONObject();
        _class = "SubscribeRequest";
        identity = clientUsername;
        channel = channelToSubscribeTo;

        obj.put("_class",_class);
        obj.put("identity",identity);
        obj.put("channel",channel);

        // Outputs encoded request to client
        //System.out.println(obj);

        return (obj);
    }
    public static Object UnsubscribeRequest(String clientUsername, String channelToUnsubscribeFrom) throws IOException {
        JSONObject obj = new JSONObject();
        _class = "UnsubscribeRequest";
        identity = clientUsername;
        channel =  channelToUnsubscribeFrom;

        obj.put("_class",_class);
        obj.put("identity",identity);
        obj.put("channel",channel);

        // Outputs encoded request to client
        //System.out.println(obj);

        return (obj);
    }
    public static Object PublishRequest(String clientChannel, String messageToPublish) throws IOException {
        JSONObject obj = new JSONObject();

        _class = "PublishRequest";
        identity = "identity";
        message = "message";

        obj.put("_class",_class);
        obj.put("identity",identity);
        obj.put("message",messageToPublish);

        // Outputs encoded request to client
        //System.out.println(obj);

        return (obj);
    }

    // SERVER RESPONSES
    public static Object SuccessResponse(String successMessage) {
        JSONObject obj = new JSONObject();
        _class = "SuccessResponse";
        success = successMessage;

        obj.put("_class",_class);
        obj.put("success",success);

        return obj;
    }
    public static Object ErrorResponse(String errorMessage) {
        JSONObject obj = new JSONObject();
        _class = "ErrorResponse";
        error = errorMessage;

        obj.put("_class",_class);
        obj.put("error",errorMessage);

        return obj;
    }
    public static Object MessageListResponse(ArrayList<String> channelMessages) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(channelMessages);
        JSONObject obj = new JSONObject();
        _class = "MessageListResponse";
        messages = jsonArray;

        obj.put("_class", _class);
        obj.put("messages", messages);

        return (obj);
    }

    public static Object EncodeCommand(String command, String body, String username) {
        JSONObject obj = new JSONObject();
        _class = "Command";
        if (body.equals(null)) {
            body = command;
        }
        else {
            body = command + " " + body;
        }
        identity = username;

        obj.put("_class", _class);
        obj.put("body", body);
        obj.put("identity", identity);

        return (obj);
    }
    public static Object EncodeMessage(String sentMessageToEncode, String currentChannel, String newClass) throws IOException {
        JSONObject obj = new JSONObject();
        SimpleDateFormat formatter= new SimpleDateFormat("[HH:mm:ss]");
        Date date = new Date(System.currentTimeMillis());

        if (!newClass.equals("")) {
            _class = newClass;
        }

        if (from == null) {
            from = "SERVER";
        }

        obj.put("_class",_class);
        obj.put("from", from);
        obj.put("to", currentChannel);
        obj.put("when", formatter.format(date));
        obj.put("body", sentMessageToEncode);

        if (from != "SERVER") {
            try {
                file = new BufferedWriter (new FileWriter("messagelog.json", true));
                file.write(obj.toJSONString());
                file.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                file.flush();
                file.close();
            }
        }
        return (obj);
    }

    public static Object DecodeMessage(Object receivedMessageToDecode) {
        try {

            // Outputs Encoded message to Decode, to client
            //System.out.println("[DECODE MESSAGE] " + receivedMessageToDecode.toString());

            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse((String) receivedMessageToDecode);

            _class = (String) getObjectClass(receivedMessageToDecode);

            switch(_class) {

                case("SuccessResponse"): {
                    _class = (String) obj.get("_class");
                    success = (String) obj.get("success");

                    return ("[SUCCESS] " + success);
                }

                case ("ErrorResponse"): {
                    _class = (String) obj.get("_class");
                    error = (String) obj.get("error");

                    return ("[ERROR]" + error);
                }

                case ("MessageListResponse"): {
                    _class = (String) obj.get("_class");
                    messages = (JSONArray) obj.get("messages");

                    return ("[MESSAGELIST] " + messages);
                }

                case("SubscribeRequest"): {
                    _class = (String) obj.get("_class");
                    identity = (String) obj.get("identity");
                    channel = (String) obj.get("channel");

                    return ("/subscribe " + channel + " " + identity);
                }

                case("UnsubscribeRequest"): {
                    _class = (String) obj.get("_class");
                    identity = (String) obj.get("identity");
                    channel = (String) obj.get("channel");

                    return ("/unsubscribe " + channel + " " + identity);
                }

                case("OpenRequest"): {
                    identity = (String) obj.get("identity");

                    return identity;
                }

                case("GetRequest"): {
                    previousMessages = (String) obj.get("previousMessages");
                    from = (String) obj.get("from");

                    return ("/messages " + previousMessages + " " + from);
                }

                case("Message"): {
                    _class = (String) obj.get("_class");
                    from = (String) obj.get("from");
                    to = (String) obj.get("to");
                    when = (String) obj.get("when");
                    body = (String) obj.get("body");

                    return ("[MESSAGE]" + " [" + to + "] " + when + " " + from + ": " + body);
                }

                case("Command"): {
                    _class = (String) obj.get("_class");
                    body = (String) obj.get("body");
                    identity = (String) obj.get("identity");

                    return (body + " " + identity);
                }

                default:
                    return null;
            }
        }
        catch (ClassCastException | NullPointerException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static Object getObjectClass(Object receivedMessageToDecode) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(receivedMessageToDecode.toString());

            _class = (String) obj.get("_class");
            return _class;

        } catch (ParseException e) {
            System.out.println("Error parsing JSON input: " + e.getMessage());
        }
        return null;
    }
}
