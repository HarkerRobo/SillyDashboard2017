import java.util.logging.Level;
import java.util.logging.Logger;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Event;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadProbeReturn;
import org.freedesktop.gstreamer.Pad.EVENT_PROBE;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.lowlevel.GstPadAPI;

/**
 * Debugs the number of frames that go through a pipeline
 */
public class FrameMonitor extends Thread {
    public static final int FRAME_COUNT_PRINT_INTERVAL = 3000;

    private Element pipe;
    private String name;
    private volatile int numFrames = 0;

	private static final Logger logger = Logger.getLogger(Main.class.getName() + "." + FrameMonitor.class.getName());

    public FrameMonitor(String name) {
        this.name = name;
    }

    public FrameMonitor(Bin pipeline, String name) {
        this.pipe = pipeline.getElementByName("sink");
        this.name = name;
        setupMonitor();
    }

    private void setupMonitor() {
        pipe.getSinkPads().get(0).addEventProbe(new EVENT_PROBE(){

            @Override
            public PadProbeReturn eventReceived(Pad pad, Event event) {
                numFrames++;
                return PadProbeReturn.PASS;
            }
        }, GstPadAPI.GST_PAD_PROBE_TYPE_BLOCK_UPSTREAM);
    }

    public void setPipeline(Bin pipeline) {
        this.pipe = pipeline.getElementByName("sink");
        setupMonitor();
    }

    public void setAppsink(AppSink sink) {
        sink.set("emit-signals", true);
        sink.set("wait-on-eos", false);
        AppSinkListener listener = new AppSinkListener();
        sink.connect((AppSink.NEW_SAMPLE) listener);
    }

    private class AppSinkListener implements AppSink.NEW_SAMPLE {
        @Override
        public FlowReturn newSample(AppSink sink) {
            numFrames++;
            return FlowReturn.OK;
		}
    }

    @Override
    public void run() {
        while (true) {
            try {
                int beforeFrames = numFrames;
                Thread.sleep(FRAME_COUNT_PRINT_INTERVAL);
                logger.info((numFrames - beforeFrames) + " new frames from " + name);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not retrieve frame count from buffer", e);
            }
        }
    }

}
