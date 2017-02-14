import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Pipeline;

@SuppressWarnings("serial")
public class CameraStream extends JComponent {
	private int w;
	private int h;
	
	private JLayeredPane layeredPane = null;
	
	public CameraStream(RaspiNetworker networker, int streamPort, int width, int height, boolean showCorners) {
		w = width;
		h = height;
		
		networker.addStatusReceiver(new RaspiNetworker.StatusReceiver() {
			
			@Override
			public void receiveStatus(String status) {
				System.out.println(status);
			}
		});
		
		setLayout(new GridLayout(1,1));
		
		SimpleVideoComponent vc = createVideoComponent(streamPort, width, height);
		
		if (showCorners) {
			layeredPane = new JLayeredPane();
			layeredPane.setPreferredSize(new Dimension(width, height));
			
			layeredPane.add(vc, new Integer(0));
			
			CornerViewer cv = new CornerViewer(networker, width);
			layeredPane.add(cv, new Integer(1));
			
			add(layeredPane);
		} else {
			add(vc);
		}
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
