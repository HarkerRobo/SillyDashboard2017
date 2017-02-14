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
	private List<RaspiListener> listeners = new LinkedList<RaspiListener>();
	private List<StatusReceiver> statusReceivers = new LinkedList<StatusReceiver>();
	
	// Socket configuration
	private Socket socket;
	private String ip;
	private int port;
	
	// Streams
	private BufferedReader in;
	private PrintWriter out;
	
	private boolean openSocket = true; // Whether to reopen socket in the "event loop"
	private boolean socketOpened = false;

	public RaspiNetworker(String ipaddress, int portnumber) {
		ip = ipaddress;
		port = portnumber;
	}
	
	private void connect() throws UnknownHostException, IOException {
		socket = new Socket(ip, port);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
	}
	
	public void reconnect() {
		openSocket = true;
	}

	public void run() {
		while (true) {
			if (openSocket == true) {
				openSocket = false;
				try {
					connect();
					socketOpened = true;
					for (StatusReceiver receiver : statusReceivers)
						receiver.receiveStatus("Socket successfully opened");
				} catch (Exception e) {
					System.out.println("Error connecting to " + ip + ":" + port);
					socketOpened = false;
					for (StatusReceiver receiver : statusReceivers)
						receiver.receiveStatus("Encountered error while openning socket: " + e);
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

	public void send(JSONObject obj) {
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
