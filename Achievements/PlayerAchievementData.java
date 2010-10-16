
public class PlayerAchievementData
{
	private String name;
	private int count;
	
	PlayerAchievementData(String name, int count)
	{
		this.name = name;
		this.count = count;
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
	
	public String toString()
	{
		return name + ":" + count;
	}
}