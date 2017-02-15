import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Pipeline;

@SuppressWarnings("serial")
public class CameraStream extends JComponent {
	private int w;
	private int h;
	
	private JLayeredPane layeredPane = null;
	private JLabel statusLabel;
	
	public CameraStream(final RaspiNetworker networker, int streamPort, int width, int height, boolean showCorners) {
		w = width;
		h = height;
		
		networker.addStatusReceiver(new RaspiNetworker.StatusReceiver() {
			
			@Override
			public void receiveStatus(String status) {
				statusLabel.setText("<html>" + status + "</html>");
			}
		});
		
		setLayout(new BorderLayout());
		
		SimpleVideoComponent vc = createVideoComponent(streamPort, width, height);
		
		if (showCorners) {
			layeredPane = new JLayeredPane();
			layeredPane.setPreferredSize(new Dimension(width, height));
			
			layeredPane.add(vc, new Integer(0));
			
			CornerViewer cv = new CornerViewer(networker, width);
			layeredPane.add(cv, new Integer(1));
			
			add(layeredPane, BorderLayout.CENTER);
		} else {
			add(vc, BorderLayout.CENTER);
		}

		// ISO
		final JSpinner isoField = new JSpinner(new SpinnerNumberModel(RaspiNetworker.ISO, 100, 800, 1));
		JLabel isoLabel = new JLabel("ISO");
		
		JPanel isoPanel = new JPanel();
		isoPanel.setLayout(new BorderLayout());
		isoPanel.add(isoLabel, BorderLayout.WEST);
		isoPanel.add(isoField, BorderLayout.CENTER);
		
		// Shutter speed
		final JSpinner shutterField = new JSpinner(new SpinnerNumberModel(RaspiNetworker.SHUTTER, 100, 6000000, 100));
		JLabel shutterLabel = new JLabel("Shutter speed");
		
		JPanel shutterPanel = new JPanel();
		shutterPanel.setLayout(new BorderLayout());
		shutterPanel.add(shutterLabel, BorderLayout.WEST);
		shutterPanel.add(shutterField, BorderLayout.CENTER);
		
		// ISO + Shutter speed panel
		JPanel shutterIsoPanel = new JPanel();
		shutterIsoPanel.setLayout(new BorderLayout());
		shutterIsoPanel.add(isoPanel, BorderLayout.EAST);
		shutterIsoPanel.add(shutterPanel, BorderLayout.WEST);
		
		// Start button
		JButton startButton = new JButton("Start");
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				networker.reconnect((Integer) isoField.getValue(), (Integer) shutterField.getValue());
			}
		});
		
		// Stop button
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				networker.disconnect();
			}
		});
		
		// Button panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));
		buttonPanel.add(startButton);
		buttonPanel.add(stopButton);
		
		// ISO + Shutter speed + button panel
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());
		controlPanel.add(buttonPanel, BorderLayout.NORTH);
		if (showCorners) {
			controlPanel.add(shutterIsoPanel, BorderLayout.SOUTH);
		}

		// Status
		statusLabel = new JLabel();
		
		// More stuff in a another panel
		JPanel anotherPanel = new JPanel();
		anotherPanel.setLayout(new BorderLayout());
		anotherPanel.add(controlPanel, BorderLayout.NORTH);
		anotherPanel.add(statusLabel, BorderLayout.CENTER);
		
		// Set up everything
		setLayout(new BorderLayout());
		add(vc, BorderLayout.CENTER);
		add(anotherPanel, BorderLayout.SOUTH);
		
		networker.reconnect(RaspiNetworker.ISO, RaspiNetworker.SHUTTER);
		
		setPreferredSize(new Dimension(width, height));
	}
	
	private static SimpleVideoComponent createVideoComponent(int port, int width, int height) {
		SimpleVideoComponent vc = new SimpleVideoComponent();
        Bin bin = Bin.launch("udpsrc port=" + port + " ! application/x-rtp, payload=96 ! rtph264depay ! avdec_h264 ! videoconvert", true);
        Pipeline pipe = new Pipeline();
        pipe.addMany(bin, vc.getElement());
        Pipeline.linkMany(bin, vc.getElement()); 
        
        pipe.play();
        vc.setPreferredSize(new Dimension(100, 100));
        
        return vc;
	}
	
	public void resize(int width) {
		int newHeight = (int) ((double) width / w * h);
		setPreferredSize(new Dimension(width, newHeight));
		if (layeredPane != null) {
			for (Component p : layeredPane.getComponents()) {
				p.setBounds(0, 0, width, newHeight);
			}
		}
		revalidate();
	}
}
