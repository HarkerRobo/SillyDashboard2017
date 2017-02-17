import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Config {
	
	@SuppressWarnings("rawtypes")
	private Map<String, Map> config = new HashMap<String, Map>();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Config(String filename) throws FileNotFoundException {
		InputStream is = new FileInputStream(filename);
		
		Yaml yaml = new Yaml();
		List<Map> rawConfig = (List<Map>) yaml.load(is);
		for (Map m : rawConfig) {
			config.put((String) m.get("name"), m);
		}
	}
	
	public String ip(String name) {
		return (String) config.get(name).get("ip");
	}
	
	@SuppressWarnings("unchecked")
	public int ctrlPort(String name) {
		return ((Map<String, Integer>) config.get(name).get("ports")).get("control");
	}
	
	@SuppressWarnings("unchecked")
	public int strmPort(String name) {
		return ((Map<String, Integer>) config.get(name).get("ports")).get("stream");
	}
}
