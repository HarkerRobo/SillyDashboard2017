import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

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
	private static final int CONTROL_PORT = 6000;
	private static final String IP_VISION = "192.168.1.28";
	private static final String IP_DRIVER = "192.168.1.29";

	public static RaspiNetworker raspinetDriver;
	public static RaspiNetworker raspinetVision;

	public static void main(String[] args) {
		Gst.init("Stream viewer", args);

		raspinetVision = new RaspiNetworker(IP_VISION, CONTROL_PORT, PORT_VISION);
		raspinetVision.start();

		raspinetDriver = new RaspiNetworker(IP_DRIVER, CONTROL_PORT, PORT_DRIVER);
		raspinetDriver.start();
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
				f.setLayout(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.HORIZONTAL;
				c.gridy = 0;

				c.weightx = 640/(640+1296);
				c.gridx = 0;
				final CameraStream visionStream = new CameraStream(raspinetVision, PORT_VISION, 640, 480, true);
				f.add(visionStream, c);

				c.weightx = 1296/(640+1296);
				c.gridx = 1;
				final CameraStream driverStream = new CameraStream(raspinetDriver, PORT_DRIVER, 1296, 972, false);
				f.add(driverStream, c);

				f.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentResized(ComponentEvent e) {
						visionStream.resize((int)(f.getContentPane().getWidth()*640.0/(640+1296)));
						driverStream.resize((int)(f.getContentPane().getWidth()*1296.0/(640+1296)));
					}
				});

				f.pack();
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				f.setVisible(true);
			}
		});
	}
}
