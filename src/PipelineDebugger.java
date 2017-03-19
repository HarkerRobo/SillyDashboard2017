import java.util.AbstractMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.GstObject;

public class PipelineDebugger extends Thread {
	private Bus bus;
	private String streamName;
	
	private static final Logger logger = Logger.getLogger(Main.class.getName() + "." + PipelineDebugger.class.getName());
	
	public PipelineDebugger(Bin pipe, String strmName) {
		bus = pipe.getBus();
		streamName = strmName;
	}
	
	private void log(Level level, String message) {
		logger.log(level, message, new AbstractMap.SimpleImmutableEntry<String, Object>("stream", streamName));
	}
	
	public void run() {
		bus.connect(new Bus.INFO() {
			@Override
			public void infoMessage(GstObject source, int code, String message) {
				log(Level.INFO, message);
			}
	   	});
	   	
	   	bus.connect(new Bus.WARNING() {
			@Override
			public void warningMessage(GstObject source, int code, String message) {
				log(Level.WARNING, message);
			}
	   	});
	   	
	   	bus.connect(new Bus.ERROR() {
			@Override
			public void errorMessage(GstObject source, int code, String message) {
				log(Level.SEVERE, message);
			}    		
	   	});
	   	
	   	bus.connect(new Bus.STATE_CHANGED() {
	   		private org.freedesktop.gstreamer.State oldFromState;
	   		private org.freedesktop.gstreamer.State oldToState;
	   		
			@Override
			public void stateChanged(GstObject source, org.freedesktop.gstreamer.State old,
					org.freedesktop.gstreamer.State current, org.freedesktop.gstreamer.State pending) {
				if (!old.equals(oldFromState) || !current.equals(oldToState)) {
					log(Level.FINER, "Pipeline state changed from " + old + " to " + current);
					oldFromState = old;
					oldToState = current;
				}
			}
		});
	   	
	   	bus.connect(new Bus.EOS() {
            public void endOfStream(GstObject source) {
                log(Level.FINER, "Pipeline quit");
            }
        });
	}
	
}
