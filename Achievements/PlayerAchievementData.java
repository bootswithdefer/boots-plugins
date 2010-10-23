
public class PlayerAchievementData
{
	private String name;
	private int count;
	private boolean notified;
	
	PlayerAchievementData(String name, int count)
	{
		this.name = name;
		this.count = count;
		this.notified = false;
	}
	
	public int getCount()
	{
		return this.count;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public void incrementCount()
	{
		this.count++;
	}
	
	public void notified()
	{
		this.notified = true;
	}
	
	public String toString()
	{
		return name + ":" + count;
	}
}