import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.UIManager;
import org.freedesktop.gstreamer.Gst;

/**
 *
 * @author Ashwin Reddy
 * @author Ryan Adolf
 *
 */
public class Main {
	private static final int PORT_VISION = 5001;
	private static final int PORT_DRIVER = 5002;
	private static final int PORT_GEAR = 5003;
	private static final int CONTROL_PORT = 6000;
	private static final String IP_VISION = "127.0.0.1"; //"192.168.1.28";
	private static final String IP_DRIVER = "192.168.1.29";

	public static RaspiNetworker raspinetDriver;
	public static RaspiNetworker raspinetVision;
	public static RaspiNetworker raspinetGear;
	
	private static Process p;

	public static void main(String[] args) {
		Gst.init("Stream viewer", args);

		raspinetVision = new RaspiNetworker(IP_VISION, CONTROL_PORT, PORT_VISION, false);
		raspinetVision.setDaemon(true);
		raspinetVision.start();

		raspinetDriver = new RaspiNetworker(IP_DRIVER, CONTROL_PORT, PORT_DRIVER, false);
		raspinetDriver.setDaemon(true);
		raspinetDriver.start();
		
		raspinetGear = new RaspiNetworker(IP_DRIVER, 6001, PORT_GEAR, false);
		raspinetGear.setDaemon(true);
		raspinetGear.start();
		initializeMonitor();
		initializeFrame();
	}

	public static void initializeFrame() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					System.out.println("Error setting look and feel");
				}
				
				final JFrame f = new JFrame("Camera Test");
				f.getContentPane().setLayout(new BoxLayout(f.getContentPane(), BoxLayout.PAGE_AXIS));

				final CameraStream visionStream = new CameraStream("Vision camera", raspinetVision, PORT_VISION, 640, 480, true);
				f.add(visionStream);
				visionStream.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) visionStream.getPreferredSize().getHeight()));
				
				f.add(Box.createVerticalGlue()); // Padding

				final CameraStream driverStream = new CameraStream("Driver camera", raspinetDriver, PORT_DRIVER, 1296, 972, false);
				f.add(driverStream);
				driverStream.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) driverStream.getPreferredSize().getHeight()));

				f.add(Box.createVerticalGlue()); // Padding
				
				final CameraStream gearStream = new CameraStream("Gear camera", raspinetGear, PORT_GEAR, 640, 480, false);
				f.add(gearStream);
				gearStream.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) gearStream.getPreferredSize().getHeight()));

				f.pack();
				f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				f.addWindowListener(new WindowAdapter() {
					@Override
				    public void windowClosing(WindowEvent e) {
				    	f.dispose();
				    	
				    	visionStream.close();
				    	driverStream.close();
				    	gearStream.close();
				    	
				    	if (p != null) {
				    		p.destroy();
				    	}
				        System.exit(0);
				    }
				});

				f.setVisible(true);
			}
		});
	}
	
	public static void initializeMonitor()
	{
		try {
			p = Runtime.getRuntime().exec("python src/window_monitor.py");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
