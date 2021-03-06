import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.elements.AppSink;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

/**
 *
 * @author Ashwin Reddy
 * @author Ryan Adolf
 *
 */
public class Main {
	private static final String VISION = "Vision";
	private static final String DRIVER = "Driver";
	private static final String GEAR = "Gear";

	public static RaspiNetworker raspinetDriver;
	public static RaspiNetworker raspinetVision;
	public static RaspiNetworker raspinetGear;

	private static CameraStream driverStream;
	private static CameraStream visionStream;
	private static CameraStream gearStream;

	private static Config c;
	private static Process p;

	private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) throws InterruptedException {
		logger.setLevel(Level.ALL);

		try {
			FileHandler handler = new FileHandler("sillydashboard-log.txt", true);
			handler.setLevel(Level.ALL);
			handler.setFormatter(new BunyanLogger());
			logger.addHandler(handler);
		} catch (SecurityException | IOException e1) {
			e1.printStackTrace();
		}

		logger.fine("Starting main program");
		Gst.init("Stream viewer", args);

		try {
			c = new Config("shared/config.yml");

			raspinetVision = new RaspiNetworker(c.ip(VISION), c.ctrlPort(VISION), c.strmPort(VISION), true);
			raspinetVision.setDaemon(true);
			raspinetVision.start();

			raspinetDriver = new RaspiNetworker(c.ip(DRIVER), c.ctrlPort(DRIVER), c.strmPort(DRIVER), false);
			raspinetDriver.setDaemon(true);
			raspinetDriver.start();

			raspinetGear = new RaspiNetworker(c.ip(GEAR), c.ctrlPort(GEAR), c.strmPort(GEAR), false);
			raspinetGear.setDaemon(true);
			raspinetGear.start();
			initializeMonitor();
			initializeFrame();
			pollLogitech();
			doOtherRandomStuff();
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Could not load config file", e);
		}
	}

	public static void initializeFrame() {
		EventQueue.invokeLater(new Runnable() {

			@SuppressWarnings("serial")
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error setting look and feel", e);
				}

				final JFrame f = new JFrame("SillyDashboard");
				f.setIconImage(new ImageIcon("icon.png").getImage());
				f.getContentPane().setLayout(new BoxLayout(f.getContentPane(), BoxLayout.PAGE_AXIS));

				visionStream = new CameraStream("Vision camera", raspinetVision, c.strmPort(VISION), 640, 480, true, false);
				f.add(visionStream);
				visionStream.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) visionStream.getPreferredSize().getHeight()));

				f.add(Box.createVerticalGlue()); // Padding

				driverStream = new CameraStream("Driver camera", raspinetDriver, c.strmPort(DRIVER), 1296, 972, false, true);
				f.add(driverStream);
				driverStream.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) driverStream.getPreferredSize().getHeight()));

				f.add(Box.createVerticalGlue()); // Padding

				gearStream = new CameraStream("Gear camera", raspinetGear, c.strmPort(GEAR), 640, 480, false, false);
				f.add(gearStream);
				gearStream.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) gearStream.getPreferredSize().getHeight()));

				f.add(new BandwidthUsage());

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

				f.getRootPane().getInputMap().put(
			            KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "r");
		        f.getRootPane().getActionMap().put("r", new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						visionStream.disconnect();
				    	driverStream.disconnect();
				    	gearStream.disconnect();

				    	try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							logger.log(Level.WARNING, "Thread interrupted", e);
						}

//				    	visionStream.connect();
				    	driverStream.connect();
				    	gearStream.connect();
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
			logger.log(Level.SEVERE, "Could not launch python", e);
		}
	}

	public static void pollLogitech() throws InterruptedException  {
		Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
		Controller logitech = null;
		for (Controller c : ca) {
			if (c.getName().startsWith("Logitech Dual Action"))
				logitech = c;
		}
		if (logitech != null) {
			logger.fine("Logitech controller found");
			while (true) {
				 logitech.poll();
		         for (Component c : logitech.getComponents()) {
		        	if(c.getName().equals("Button 0") && c.getPollData() == 1.0f) {
		        		logger.finer("Turning driver stream on");
		        		driverStream.connect();
		        		Thread.sleep(500);
		        	}
		        	if(c.getName().equals("Button 1") && c.getPollData() == 1.0f) {
		        		logger.finer("Turning driver stream off");
		        		driverStream.disconnect();
		        		Thread.sleep(500);
		        	}
		        	if(c.getName().equals("Button 2") && c.getPollData() == 1.0f) {
		        		logger.finer("Turning gear stream on");
		        		gearStream.connect();
		        		Thread.sleep(500);
		        	}
		        	if(c.getName().equals("Button 3") && c.getPollData() == 1.0f) {
		        		logger.finer("Turning gear stream off");
		        		gearStream.disconnect();
		        		Thread.sleep(500);
		        	}
		         }
		         Thread.sleep(100);
			}
		} else {
			logger.log(Level.WARNING, "Logitech controller not found");
		}
	}

	private static void createOtherStream(final String name, final FrameMonitor fm) {
		final org.freedesktop.gstreamer.Pipeline pipe = new org.freedesktop.gstreamer.Pipeline();
		PipelineDebugger p = new PipelineDebugger(pipe, name, new PipelineDebugger.Restarter() {
			public void restart() {
				pipe.stop();
				createOtherStream(name, fm);
			}
		});
		p.setDaemon(true);
		p.start();

        pipe.add(org.freedesktop.gstreamer.Bin.launch(
			"souphttpsrc location=http://" + c.ip(DRIVER) + ":5807 ! jpegdec name=dec ! tee name=t t. ! queue ! autovideosink name=sink t. ! queue ! appsink name=appsink", false
		));
		fm.setAppsink((AppSink) pipe.getElementByName("appsink"));
		pipe.play();
	}

	public static void doOtherRandomStuff() {
		String name = "mjpeg stream";
		FrameMonitor mon = new FrameMonitor(name);
		mon.setDaemon(true);
		mon.start();
		createOtherStream(name, mon);
	}
}
