import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class VersionCheck
{
	private String name = "none";
	private int version = 0;
	private boolean versionCheck = true;
	static final Logger log	= Logger.getLogger("Minecraft");

	VersionCheck(String name, int version, boolean check)
	{
		this.name = name;
		this.version = version;
		versionCheck = check;
		new Thread(new Runner()).start();
	}
	
	private class Runner implements Runnable
	{
		public void run()
		{
			if (!versionCheck)
				return;
	
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
