import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import org.freedesktop.gstreamer.Gst;

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

	private static Config c;
	private static Process p;

	public static void main(String[] args) {
		Gst.init("Stream viewer", args);
		
		try {
			c = new Config("shared/config.yml");

			raspinetVision = new RaspiNetworker(c.ip(VISION), c.ctrlPort(VISION), c.strmPort(VISION), false);
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
		} catch (FileNotFoundException e) {
			System.out.println("Could not load config file");
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
					System.out.println("Error setting look and feel");
				}
				
				final JFrame f = new JFrame("Camera Test");
				f.getContentPane().setLayout(new BoxLayout(f.getContentPane(), BoxLayout.PAGE_AXIS));

				final CameraStream visionStream = new CameraStream("Vision camera", raspinetVision, c.strmPort(VISION), 640, 480, true);
				f.add(visionStream);
				visionStream.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) visionStream.getPreferredSize().getHeight()));
				
				f.add(Box.createVerticalGlue()); // Padding

				final CameraStream driverStream = new CameraStream("Driver camera", raspinetDriver, c.strmPort(DRIVER), 1296, 972, false);
				f.add(driverStream);
				driverStream.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) driverStream.getPreferredSize().getHeight()));

				f.add(Box.createVerticalGlue()); // Padding
				
				final CameraStream gearStream = new CameraStream("Gear camera", raspinetGear, c.strmPort(GEAR), 640, 480, false);
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
							e.printStackTrace();
						}
				    	
				    	visionStream.connect();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void pollLogitech() {
		Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
		Controller logitech = null;
		for (Controller c : ca) {
			if (c.getName().equals("Logitech Dual Action"))
				logitech = c;
		}
		
		if (logitech != null) {
			while (true) {
				 logitech.poll();
		         for (Component c : logitech.getComponents()) {
		            System.out.println(c.getName());
		         }
			}
		}
	}
}
