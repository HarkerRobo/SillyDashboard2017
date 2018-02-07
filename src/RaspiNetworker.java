import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	public static final int SOCKET_TIMEOUT = 5000;
	
	private List<RaspiListener> listeners = new LinkedList<RaspiListener>();
	private List<StatusReceiver> statusReceivers = new LinkedList<StatusReceiver>();
	
	// Socket configuration
	private Socket socket;
	private String ip;
	private int controlPort;
	private int streamPort;
	private boolean shouldReceiveCorners;
	
	private int iso = ISO;
	private int shutter = SHUTTER;
	
	// Streams
	private BufferedReader in;
	private PrintWriter out;
	
	private boolean openSocket = false; // Whether to reopen socket in the "event loop"
	private boolean closeSocket = false;
	private boolean socketOpened = false;

	private static final Logger logger = Logger.getLogger(Main.class.getName() + "." + RaspiNetworker.class.getName());
	
	public RaspiNetworker(String ipaddress, int controlPortNum, int streamPortNum, boolean receiveCorners) {
		ip = ipaddress;
		controlPort = controlPortNum;
		streamPort = streamPortNum;
		shouldReceiveCorners = receiveCorners;
	}
	
	public String getIp() {
		return ip;
	}
	
	private void connect() throws UnknownHostException, IOException {
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip, controlPort), SOCKET_TIMEOUT);
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
				logger.finer("Connecting to " + ip + " on port " + controlPort);
				for (StatusReceiver receiver : statusReceivers)
					receiver.receiveStatus(CONNECTING_STRING);
				try {
					if (socket != null) socket.close();
					connect();
					socketOpened = true;
					send(Message.createStartStreamMessage(iso, shutter, streamPort));
					send(Message.createCntMessage());
					logger.fine("Successfully connected to " + ip + ":" + controlPort);
					for (StatusReceiver receiver : statusReceivers)
						receiver.receiveStatus("Socket successfully opened");
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error connecting to " + ip + ":" + controlPort, e);
					socketOpened = false;
					for (StatusReceiver receiver : statusReceivers)
						receiver.receiveStatus("Encountered error while openning socket: " + e);
				}
			}
			
			if (closeSocket) {
				didSomething = true;
				closeSocket = false;
				logger.finer("Going to disconnect from " + ip + ":" + controlPort + "...");
				if (socketOpened) {
					try {
						send(Message.createStopStreamMessage());
						socket.close();
						socket = null;
						socketOpened = false;
						for (StatusReceiver receiver : statusReceivers)
							receiver.receiveStatus("Socket successfully closed");
						logger.finer("Disconnected from " + ip + ":" + controlPort + "...");
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Error when closing socket for " + ip + ":" + controlPort, e);
						for (StatusReceiver receiver : statusReceivers)
							receiver.receiveStatus("Encountered error while closing socket: " + e);
					}
				}
			}
			
			if (socketOpened && shouldReceiveCorners) {
				didSomething = true;
				try {
					JSONObject obj = new JSONObject(in.readLine());
					for (RaspiListener l : listeners) {
						l.recieve(obj);
					}
				} catch (JSONException e) {
					logger.log(Level.WARNING, "Received invalid JSON", e);
				} catch (IOException e) {
					logger.log(Level.WARNING, "Error receiving JSON", e);
				} catch (NullPointerException e) {
					logger.log(Level.WARNING, "Random NullPointerException", e);
				}
			}
			
			if (!didSomething) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logger.log(Level.WARNING, "Thread interrupted", e);
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
