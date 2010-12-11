import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class VersionCheck
{
	static final Logger log	= Logger.getLogger("Minecraft");
	private boolean debug = false;

	VersionCheck(String name, int version)
	{
		boolean check = true;
		
		PropertiesFile properties = new PropertiesFile("server.properties");
		try {
			check = properties.getBoolean("boots-version-check", true);
			debug = properties.getBoolean("boots-debug", false);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while reading from server.properties", ex);
		}

		if (!check)
			return;

		new Thread(new Runner(name, version)).start();
	}
	
	private class Runner implements Runnable
	{
		private String name = "none";
		private int version = 0;
		
		Runner(String name, int version)
		{
			this.name = name;
			this.version = version;
		}

		public void run()
		{
			try {
				Thread.sleep(5000);
				URL url = new URL("http://www.bootswithdefer.com/minecraft/version.php?plugin=" + name + "&version=" + version + "&hmod=" + etc.getInstance().getVersion());
				URLConnection urlc = url.openConnection();
				
				BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
				String verline = in.readLine();
				String hModline = in.readLine();
				in.close();

				int serverVersion = 0;
				int hModVersion = 0;
				try {
					serverVersion = Integer.parseInt(verline);
					hModVersion = Integer.parseInt(hModline);
				} catch (NumberFormatException ex) {
				}

				if (serverVersion > version)
					log.info(name + " new version v" + serverVersion + " available (you have v" + version + ")");
				if (debug)
				{
					if (serverVersion < version)
						log.info(name + " v" + serverVersion + " is latest reported, you have newer v" + version);
					else
						log.info(name + " matches latest version (v" + serverVersion + ")");
				}
				
				if (version == serverVersion && !etc.getInstance().getTainted() && etc.getInstance().getVersion() < hModVersion)
					log.log(Level.SEVERE, name + " v" + version + " is not supported on your version of hMod (" + etc.getInstance().getVersion() + "), upgrade to at least hMod " + hModVersion);
			} catch (Exception ex) {
				log.log(Level.SEVERE, "VersionCheck exception", ex);
			}
		}
	}
}
