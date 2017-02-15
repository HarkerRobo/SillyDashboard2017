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
	public static final int ISO = 500;
	public static final int SHUTTER = 200000;
	
	private List<RaspiListener> listeners = new LinkedList<RaspiListener>();
	private List<StatusReceiver> statusReceivers = new LinkedList<StatusReceiver>();
	
	// Socket configuration
	private Socket socket;
	private String ip;
	private int controlPort;
	private int streamPort;
	
	private int iso = ISO;
	private int shutter = SHUTTER;
	
	// Streams
	private BufferedReader in;
	private PrintWriter out;
	
	private boolean openSocket = false; // Whether to reopen socket in the "event loop"
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
	
	public void reconnect(int newIso, int newShutter) {
		iso = newIso;
		shutter = newShutter;
		openSocket = true;
	}
	
	public void disconnect() {
		closeSocket = true;
	}

	public void run() {
		boolean didSomething = false;
		
		while (true) {
			didSomething = false;
			
			if (openSocket) {
				didSomething = true;
				openSocket = false;
				System.out.println("Connecting...");
				for (StatusReceiver receiver : statusReceivers)
					receiver.receiveStatus(CONNECTING_STRING);
				try {
					if (socket != null) socket.close();
					connect();
					socketOpened = true;
					send(Message.createStartStreamMessage(iso, shutter, streamPort));
					for (StatusReceiver receiver : statusReceivers)
						receiver.receiveStatus("Socket successfully opened");
				} catch (Exception e) {
					System.out.println("Error connecting to " + ip + ":" + controlPort);
					socketOpened = false;
					for (StatusReceiver receiver : statusReceivers)
						receiver.receiveStatus("Encountered error while openning socket: " + e);
				}
			}
			
			if (closeSocket) {
				didSomething = true;
				closeSocket = false;
				System.out.println("Going to disconnect...");
				if (socketOpened) {
					try {
						System.out.println("Disconnecting...");
						send(Message.createStopStreamMessage());
						socket.close();
						socket = null;
						socketOpened = false;
						for (StatusReceiver receiver : statusReceivers)
							receiver.receiveStatus("Socket successfully closed");
					} catch (Exception e) {
						for (StatusReceiver receiver : statusReceivers)
							receiver.receiveStatus("Encountered error while closing socket: " + e);
					}
				}
			}
			
			if (socketOpened) {
				didSomething = true;
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
			
			if (!didSomething) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
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
