import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

	public static void main(String[] args) {
		Gst.init("Stream viewer", args);

		raspinetVision = new RaspiNetworker(IP_VISION, CONTROL_PORT, PORT_VISION);
		raspinetVision.setDaemon(true);
		raspinetVision.start();

		raspinetDriver = new RaspiNetworker(IP_DRIVER, CONTROL_PORT, PORT_DRIVER);
		raspinetDriver.setDaemon(true);
		raspinetDriver.start();
		
		raspinetGear = new RaspiNetworker(IP_DRIVER, 6001, PORT_GEAR);
		raspinetGear.setDaemon(true);
		raspinetGear.start();
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
				f.setLayout(null);

				final CameraStream visionStream = new CameraStream(raspinetVision, PORT_VISION, 640, 480, true);
				f.add(visionStream);

				final CameraStream driverStream = new CameraStream(raspinetDriver, PORT_DRIVER, 1296, 972, false);
				f.add(driverStream);
				
				final CameraStream gearStream = new CameraStream(raspinetGear, PORT_GEAR, 640, 480, false);
				f.add(gearStream);

				f.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentResized(ComponentEvent e) {
						visionStream.resize((int)(f.getContentPane().getWidth()*640.0/(640+1296)));
						driverStream.resize((int)(f.getContentPane().getWidth()*1296.0/(640+1296)));
						gearStream.resize((int)(f.getContentPane().getWidth()*640.0/(640+1296)));
						
						visionStream.setBounds(0, 0, (int) visionStream.getPreferredSize().getWidth(),
													 (int) visionStream.getPreferredSize().getHeight());
						driverStream.setBounds((int) visionStream.getPreferredSize().getWidth(), 0,
											   (int) driverStream.getPreferredSize().getWidth(),
											   (int) driverStream.getPreferredSize().getHeight());
						gearStream.setBounds(0, (int) visionStream.getPreferredSize().getHeight(),
												(int) gearStream.getPreferredSize().getWidth(),
												(int) gearStream.getPreferredSize().getHeight());
					}
				});

				f.setSize(1776, 1200);
				f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				f.addWindowListener(new WindowAdapter() {
					@Override
				    public void windowClosing(WindowEvent e) {
				    	f.dispose();
				    	
				    	// TODO: Calling these prevents Java from fully exiting. WHY????
//				    	visionStream.close();
//				    	driverStream.close();
//				    	gearStream.close();
				        System.exit(0);
				    }
				});

				f.setVisible(true);
			}
		});
	}
}
