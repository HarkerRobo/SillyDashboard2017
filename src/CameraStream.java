import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.State;

@SuppressWarnings("serial")
public class CameraStream extends JPanel {
	private JLayeredPane layeredPane = null;
	private JLabel statusLabel;

	private JPanel controlPanel;

	private Bin pipe;

	public CameraStream(String name, final RaspiNetworker networker, int streamPort, int width, int height, boolean showCorners) {
		createStream(streamPort, width, height);

		networker.addStatusReceiver(new RaspiNetworker.StatusReceiver() {

			@Override
			public void receiveStatus(String status) {
				statusLabel.setText("<html>" + status + "</html>");
			}
		});

		setLayout(new BorderLayout());

		if (showCorners) {
			layeredPane = new JLayeredPane();

			CornerViewer cv = new CornerViewer(networker, width);
			layeredPane.add(cv, new Integer(1));

			add(layeredPane, BorderLayout.CENTER);
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
		controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());
		controlPanel.add(buttonPanel, BorderLayout.NORTH);
		if (showCorners) {
			controlPanel.add(shutterIsoPanel, BorderLayout.SOUTH);
		}

		// Status
		statusLabel = new JLabel();
		statusLabel.setPreferredSize(new Dimension(0, 48));

		setBorder(BorderFactory.createTitledBorder(name));

		setLayout(new BorderLayout());
		add(controlPanel, BorderLayout.NORTH);
		add(statusLabel, BorderLayout.CENTER);

		networker.reconnect(RaspiNetworker.ISO, RaspiNetworker.SHUTTER);
	}

	private void createStream(int port, int width, int height) {
        pipe = Bin.launch("udpsrc port=" + port + " ! application/x-rtp, payload=96 ! rtph264depay ! avdec_h264 ! videoconvert ! autovideosink", false);
        pipe.play();
	}

	public void close() {
		pipe.setState(State.NULL);
	}
}
