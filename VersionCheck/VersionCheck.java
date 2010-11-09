import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class VersionCheck
{
	private String name = "none";
	private int version = 0;
	static final Logger log	= Logger.getLogger("Minecraft");

	VersionCheck(String name, int version)
	{
		boolean check = true;
		
		PropertiesFile properties = new PropertiesFile("server.properties");
		try {
			check = properties.getBoolean("boots-version-check", true);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while reading from server.properties", ex);
		}

		if (!check)
			return;

		this.name = name;
		this.version = version;
		new Thread(new Runner()).start();
	}
	
	private class Runner implements Runnable
	{
		public void run()
		{
			try {
				URL url = new URL("http://www.bootswithdefer.com/minecraft/version.php?plugin=" + name + "&version=" + version + "&hmod=" + etc.getInstance().getVersion());
				URLConnection urlc = url.openConnection();
				
				BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
				String line = in.readLine();
				in.close();

				int ver = Integer.parseInt(line);
				if (ver > version)
					log.info(name + " v" + ver + " available (you have v" + version + ")");
			} catch (Exception ex) {
			}
		}
	}
}
