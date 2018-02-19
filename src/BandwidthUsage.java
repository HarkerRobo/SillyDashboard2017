import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class BandwidthUsage extends JPanel {
	private static final long serialVersionUID = 8978664781329173499L;

	private JLabel usage;
	
	private static final Logger logger = Logger.getLogger(Main.class.getName() + "." + BandwidthUsage.class.getName());
	
	public BandwidthUsage() {
		usage = new JLabel(renderUsageLabel("??.??"));
		add(usage);
		
		Thread t = new WindowsMonitorThread();
		t.start();
	}
	
	private String renderUsageLabel(String usage) {
		return "Network usage: " + usage;
	}
	
	private String usageUnits(double usage) {
		if (usage < 1e3) return String.format("%.2f bps", usage);
		if (usage < 1e6) return String.format("%.2f Kbps", usage/1e3);
		if (usage < 1e9) return String.format("%.2f Mbps", usage/1e6);
		else             return String.format("%.2f Gbps", usage/1e9);
	}

	private class WindowsMonitorThread extends Thread {

		@Override
		public void run() {
			try {
				Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\typeperf.exe \"\\Network Interface(*)\\Bytes Total/sec\"");
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				input.readLine(); // Ignore blank line
				input.readLine(); // Ignore line with titles
				while (true) {
					parseLine(input.readLine());
				}
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Could not launch python", e);
			}
		}
		
		private void parseLine(String line) {
			String[] split = line.split(",");
			for (int i = 0; i < split.length; i++) {
				split[i] = split[i].substring(1, split[i].length() - 1);
			}
			
			String time = split[0];
			double totalUsage = 0;
			for (int i = 1; i < split.length; i++) {
				totalUsage += Double.parseDouble(split[i]); 
			}
			
			usage.setText(renderUsageLabel(usageUnits(totalUsage * 8)));
		}

	}

}
