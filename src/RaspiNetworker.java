import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author joelmanning
 * @author Ryan Adolf
 *
 */
public class RaspiNetworker extends Thread {
	public static final String CONNECTING_STRING = "Conecting...";
	
	private List<RaspiListener> listeners = new LinkedList<RaspiListener>();
	private List<StatusReceiver> statusReceivers = new LinkedList<StatusReceiver>();
	
	// Socket configuration
	private Socket socket;
	private String ip;
	private int controlPort;
	private int streamPort;
	
	// Streams
	private BufferedReader in;
	private PrintWriter out;
	
	private boolean openSocket = true; // Whether to reopen socket in the "event loop"
	private boolean closeSocket = false;
	private boolean socketOpened = false;

	public RaspiNetworker(String ipaddress, int controlPortNum, int streamPortNum) {
		ip = ipaddress;
		controlPort = controlPortNum;
		streamPort = streamPortNum;
	}
	
	private void connect() throws UnknownHostException, IOException {
		socket = new Socket(ip, controlPort);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
	}
	
	public void reconnect() {
		openSocket = true;
	}
	
	public void disconnect() {
		closeSocket = false;
	}

	public void run() {
		while (true) {
			if (openSocket == true) {
				for (StatusReceiver receiver : statusReceivers)
					receiver.receiveStatus(CONNECTING_STRING);
				openSocket = false;
				try {
					connect();
					socketOpened = true;
					for (StatusReceiver receiver : statusReceivers)
						receiver.receiveStatus("Socket successfully opened");
					send(Message.createStartStreamMessage(500, 2000, streamPort));
				} catch (Exception e) {
					System.out.println("Error connecting to " + ip + ":" + controlPort);
					socketOpened = false;
					for (StatusReceiver receiver : statusReceivers)
						receiver.receiveStatus("Encountered error while openning socket: " + e);
				}
			}
			
			if (closeSocket == true) {
				closeSocket = false;
				if (socketOpened) {
					send(Message.createStopStreamMessage());
				}
			}
			
			if (socketOpened) {
				try {
					JSONObject obj = new JSONObject(in.readLine());
					for (RaspiListener l : listeners) {
						l.recieve(obj);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void send(JSONObject obj) {
		out.println(obj);
		out.flush();
	}

	public void addMessageListener(RaspiListener raspiListener) {
		listeners.add(raspiListener);
	}
	
	public void addStatusReceiver(StatusReceiver receiver) {
		statusReceivers.add(receiver);
	}
	
	public interface StatusReceiver {
		public void receiveStatus(String status);
	}
	
	/**
	 * Class RaspiListener represents essentially a callback function for what to do
	 * with received JSONObject messages
	 * @author ashwinreddy
	 * @version Feb 5 2017
	 */
	public interface RaspiListener {

	    /**
	     * Receives JSON and processes it as a 3D array
	     * @param JSONObject obj JSONObject from RasPi
	     */
		public void recieve(JSONObject obj);
	}
}
