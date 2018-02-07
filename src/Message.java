import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONObject;

public class Message {
	public static JSONObject createStartStreamMessage(int iso, int shutterspeed, int port)
	{
		try {
			JSONObject message = new JSONObject();
			message.put("type", "start");
			message.put("port", port);
			message.put("host", InetAddress.getLocalHost().getHostAddress());
			message.put("iso", iso);
			message.put("shutterspeed", shutterspeed);
			return message;
		} catch (UnknownHostException e) {
			System.out.println(e);
			return null;
		}
	}
	
	public static JSONObject createStopStreamMessage()
	{
		JSONObject message = new JSONObject();
		message.put("type", "stop");
		return message;
	}

	public static JSONObject createCntMessage()
	{
		JSONObject message = new JSONObject();
		message.put("type", "cnt");
		return message;
	}
}
