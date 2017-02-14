import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
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

		raspinetVision = new RaspiNetworker(IP_VISION, CONTROL_PORT);
		raspinetVision.start();

		raspinetDriver = new RaspiNetworker(IP_DRIVER, CONTROL_PORT);
		raspinetDriver.start();
		initializeFrame();
	}

	public static void initializeFrame() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				final JFrame f = new JFrame("Camera Test");
				f.setLayout(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.HORIZONTAL;
				c.gridy = 0;

				c.weightx = 640;
				c.gridx = 0;
				final CameraStream visionStream = new CameraStream(raspinetVision, PORT_VISION, 640, 480, true);
				f.add(visionStream, c);

				c.weightx = 1296;
				c.gridx = 1;
				final CameraStream driverStream = new CameraStream(raspinetDriver, PORT_DRIVER, 1296, 972, false);
				f.add(driverStream, c);

				f.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentResized(ComponentEvent e) {
						visionStream.resize(visionStream.getWidth());
						driverStream.resize(driverStream.getWidth());
					}
				});

				f.pack();
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				f.setVisible(true);
			}
		});
	}
}
