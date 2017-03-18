import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.json.JSONObject;

public class BunyanLogger extends Formatter {
	public static final String HOSTNAME;
	private static final int PID;
	private static final Map<Level, Integer> LEVELS = new HashMap<Level, Integer>();
	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
	
	static {
		// The method below is expected to be in the form pid@hostname. If it is not, things will fail.
		String[] hostnameAndPid = ManagementFactory.getRuntimeMXBean().getName().split("@");
		PID = Integer.parseInt(hostnameAndPid[0]);
		HOSTNAME = hostnameAndPid[1];
		
		LEVELS.put(Level.SEVERE, 50);
		LEVELS.put(Level.WARNING, 40);
		LEVELS.put(Level.INFO, 30);
		LEVELS.put(Level.FINE, 20);
		LEVELS.put(Level.FINER, 10);
	}
	
    // this method is called for every log records
    public String format(LogRecord rec) {
		JSONObject message = new JSONObject();
		message.put("name", rec.getSourceClassName());
		message.put("hostname", HOSTNAME);
		message.put("pid", PID);
		message.put("level", LEVELS.get(rec.getLevel()));

		if (rec.getThrown() != null) {
			String stack = rec.getThrown().toString();
			for (StackTraceElement el : rec.getThrown().getStackTrace())
				stack += "\n    at " + el.toString();

			JSONObject error = new JSONObject();
			error.put("message", rec.getThrown().getMessage());
			error.put("name", rec.getThrown().getClass().getName());
			error.put("stack", stack);
			message.put("err", error);
		}
		
		message.put("method", rec.getSourceMethodName());
		
		message.put("msg", rec.getMessage());
		message.put("time", FORMATTER.format(Instant.ofEpochMilli(rec.getMillis())));
		message.put("v", 0);
		return message.toString() + "\n";
    }
}