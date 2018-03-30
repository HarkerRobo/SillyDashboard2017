import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeSender extends Thread {

    public static final int TIME_PORT = 5806;

	private static final Logger logger = Logger.getLogger(Main.class.getName() + "." + TimeSender.class.getName());

    private String ip;

    public TimeSender(String ip) {
        this.ip = ip;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket sock = new Socket();
                sock.connect(new InetSocketAddress(ip, TIME_PORT), 3000);
                logger.info("Connected to server");
                OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
                out.write(new Date().getTime() + "\n");
                out.flush();
                sock.close();
                logger.info("Time sent to raspi");
                break;
            } catch (Exception e) {
                // FIXME: This limits the amount of errors theoretically logged but does not handle
                // cases where the camera server is on localhost. Should be more elegant.
                if (ip.startsWith("10")) {
                    logger.log(Level.FINER, "Error sending time", e);
                }
                try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
            }
        }
    }

}
