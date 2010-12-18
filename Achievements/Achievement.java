import java.util.logging.Logger;
import java.util.logging.Level;

public class Achievement
{
	public boolean modified = false;
	private int count;
	static final Logger log	= Logger.getLogger("Minecraft");

	Achievement()
	{
		count = 0;
		modified = false;
	}

	public int getCount()
	{
		return count;
	}

	public void put(int value)
	{
		count = value;
		modified = true;
	}
	
	public void incrementCount()
	{
		count++;
		modified = true;
	}
}