import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
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
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.State;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class CameraStream extends JPanel {
	private static final int SCALE = 2;
	
	private JLayeredPane layeredPane = null;
	private JLabel statusLabel;

	private JPanel controlPanel;

	private Pipeline pipe;
	private RaspiNetworker nwkr;
	private JSpinner isoField;
	private JSpinner shutterField;

	private String fromCnt(JSONArray contour) {
		String ret = "<polyline points=\"";
		for (Object pointWrapper : contour) {
			JSONArray point = ((JSONArray) pointWrapper).getJSONArray(0);
			ret += (point.getInt(0)*SCALE) + "," + (point.getInt(1)*SCALE) + " ";
		}
		JSONArray fPoint = contour.getJSONArray(0).getJSONArray(0);
		ret += (fPoint.getInt(0)*SCALE) + "," + (fPoint.getInt(1)*SCALE);
		return ret + "\" style=\"fill: none; stroke: yellow; stroke-width: 8;\" />";
	}
	
	private String fromCnts(JSONArray contours) {
		String ret = "";
		for (Object contour : contours) {
			ret += fromCnt((JSONArray) contour);
		}
		return ret;
	}
	
	public CameraStream(String name, final RaspiNetworker networker, int streamPort, int width, int height, boolean showCorners, boolean showCenterDivider) {
		nwkr = networker;
		createStream(name, streamPort, width, height, showCorners, showCenterDivider);

		networker.addStatusReceiver(new RaspiNetworker.StatusReceiver() {

			@Override
			public void receiveStatus(String status) {
				statusLabel.setText("<html>" + status + "</html>");
			}
		});
		
		if (showCorners) {
			networker.addMessageListener(new RaspiNetworker.RaspiListener() {
				@Override
				public void recieve(JSONObject obj) {
					if (pipe != null && pipe.isPlaying()) {
						if (obj.isNull("corners")) {
							pipe.getElementByName("overlay").set("data", String.format(
									"<svg>" +fromCnts(obj.getJSONArray("valid")) + "</svg>"));
						} else {
							JSONArray corners = obj.getJSONArray("corners");
							pipe.getElementByName("overlay").set("data", String.format(
									"<svg>%s" +
									"<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" style=\"fill: none; stroke: blue; stroke-width:8\"/>" +
									"</svg>", fromCnts(obj.getJSONArray("valid")), corners.getInt(0)*SCALE, corners.getInt(1)*SCALE, corners.getInt(2)*SCALE, corners.getInt(3)*SCALE));
						}
					}
				}
			});
		}

		setLayout(new BorderLayout());

//		if (showCorners) {
//			layeredPane = new JLayeredPane();
//
//			CornerViewer cv = new CornerViewer(networker, width);
//			layeredPane.add(cv, new Integer(1));
//
//			add(layeredPane, BorderLayout.CENTER);
//		}

		// ISO
		isoField = new JSpinner(new SpinnerNumberModel(RaspiNetworker.ISO, 100, 800, 1));
		JLabel isoLabel = new JLabel("ISO");

		JPanel isoPanel = new JPanel();
		isoPanel.setLayout(new BorderLayout());
		isoPanel.add(isoLabel, BorderLayout.WEST);
		isoPanel.add(isoField, BorderLayout.CENTER);

		// Shutter speed
		shutterField = new JSpinner(new SpinnerNumberModel(RaspiNetworker.SHUTTER, 100, 6000000, 100));
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
				connect();
			}
		});

		// Stop button
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				disconnect();
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
//			pipe.getElementByName("overlay").connect("draw", new Closure() {
//				@SuppressWarnings("unused")
//				public void invoke(Element overlay) {
//					System.out.println("hello");
//					System.out.println(overlay);
//				}
//			});
		}

		// Status
		statusLabel = new JLabel();
		statusLabel.setPreferredSize(new Dimension(0, 48));

		// Availability
		JPanel availPanel = new JPanel();
		
		JPanel pingPanel = new JPanel();
		JLabel pingLabel = new JLabel();
		pingLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, pingLabel.getFont().getSize()));
		pingPanel.add(pingLabel);
		pingPanel.add(new JLabel("Pingable"));
		availPanel.add(pingPanel);
		
		JPanel sshPanel = new JPanel();
		JLabel sshLabel = new JLabel();
		sshLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, pingLabel.getFont().getSize()));
		sshPanel.add(sshLabel);
		sshPanel.add(new JLabel("SSHable"));
		availPanel.add(sshPanel);
		
		setBorder(BorderFactory.createTitledBorder(name));

		setLayout(new BorderLayout());
		add(controlPanel, BorderLayout.NORTH);
		add(statusLabel, BorderLayout.CENTER);
		add(availPanel, BorderLayout.SOUTH);

		StatusThread t = new StatusThread(networker.getIp(), pingLabel, sshLabel);
		t.setDaemon(true);
		t.start();
		
		networker.reconnect(RaspiNetworker.ISO, RaspiNetworker.SHUTTER);
	}

	private void createStream(final String name, final int port, final int width, final int height, final boolean showCorners, final boolean showCenterDivider) {
		String cdString = "";
		if (showCorners)
//			cdString += "cairooverlay name=overlay ! ";
			cdString += "rsvgoverlay name=overlay ! ";
		if (showCenterDivider)
				cdString += "gdkpixbufoverlay location=line.png offset-x=" + (width / 2 - 2) + " overlay-height=" + height + " ! ";
		if (name.equals("Gear camera"))
			cdString += "videoflip method=counterclockwise ! ";
		
		pipe = new Pipeline();
		PipelineDebugger p = new PipelineDebugger(pipe, name, new PipelineDebugger.Restarter() {
			public void restart() {
				pipe.stop();
				createStream(name, port, width, height, showCorners, showCenterDivider);
			}
		});
		p.setDaemon(true);
		p.start();
        pipe.add(Bin.launch("udpsrc port=" + port + " timeout=5000000000 ! application/x-rtp, payload=96 ! rtph264depay ! avdec_h264 ! videoconvert ! " + cdString + "autovideosink name=sink", false));
//        pipe.add(Bin.launch("udpsrc port=" + port + " timeout=5000000000 ! application/x-rtp, payload=96 ! rtpmp2tdepay ! tsdemux name=demuxer demuxer ! queue ! avdec_h264 ! videoconvert ! autovideosink demuxer ! queue ! avdec_h264 ! videoconvert ! autovideosink name=sink ", false));
		
        pipe.play();
	}

	public void close() {
		pipe.setState(State.NULL);
	}
	
	public void disconnect() {
		nwkr.disconnect();
	}
	
	public void connect() {
		nwkr.reconnect((Integer) isoField.getValue(), (Integer) shutterField.getValue());
	}
}
