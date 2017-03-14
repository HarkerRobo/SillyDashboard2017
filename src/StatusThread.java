import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JLabel;

public class StatusThread extends Thread {
	private static final String YES = "\u2714";
	private static final String NO = "\u274C";
	
	
	private InetAddress ip;
	private JLabel ping;
	private JLabel ssh;
	
	
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
		
		while (true) {
			ping.setText(getPingStatus() ? YES : NO);
			ssh.setText(getSSHStatus() ? YES : NO);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
