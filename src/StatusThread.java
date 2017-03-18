import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.swing.JLabel;

public class StatusThread extends Thread {
	private static final String YES = "\u2714";
	private static final String NO = "\u274C";
	
	private InetAddress ip;
	private JLabel ping;
	private JLabel ssh;
	
	private boolean pingStatus;
	private boolean sshStatus;

	private static final Logger logger = Logger.getLogger(Main.class.getName() + "." + StatusThread.class.getName());
	
	public StatusThread(String ipAddress, JLabel pingLabel, JLabel sshLabel) {
		ping = pingLabel;
		ssh = sshLabel;
		try {
			ip = InetAddress.getAllByName(ipAddress)[0];
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	private boolean getPingStatus()
	{
		try {
			return ip.isReachable(2000);
		} catch (IOException e) {
			return false;
		}
	}
	
	private boolean getSSHStatus() {
		Socket socket = null;
	    try {
	    	socket = new Socket();
	    	socket.connect(new InetSocketAddress(ip, 22), 2000);
	        return true;
	    } catch (IOException ex) {
	        return false;
	    } finally {
	    	if (socket != null)
				try {
					socket.close();
				} catch (IOException e) {
					// Oh well we tried
					e.printStackTrace();
				}
	    }
	}
	
	public void run() {
		ping.setText(NO);
		ssh.setText(NO);
		
		pingStatus = getPingStatus();
		sshStatus = getSSHStatus();
		
		logger.finer("Ping status of " + ip.getHostAddress() + " initially " + pingStatus);
		logger.finer("SSH status of " + ip.getHostAddress() + " initially " + sshStatus);
		
		while (true) {
			ping.setText(pingStatus ? YES : NO);
			ssh.setText(sshStatus ? YES : NO);
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			boolean newPingStatus = getPingStatus();
			boolean newSSHStatus = getSSHStatus();
			
			if (newPingStatus != pingStatus) {
				pingStatus = newPingStatus;
				logger.finer("Ping status of " + ip.getHostAddress() + " changed to " + pingStatus);
			}
			if (newSSHStatus != sshStatus) {
				sshStatus = newSSHStatus;
				logger.finer("SSH status of " + ip.getHostAddress() + " changed to " + sshStatus);
			}
		}
	}
	
}
