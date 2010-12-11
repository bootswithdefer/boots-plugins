import java.util.HashSet;
import java.util.HashMap;

import java.util.logging.*;
import java.sql.*;

public class PlayerAreaStats implements Runnable
{
	static final Logger log = Logger.getLogger("Minecraft");
	private Player player;
	private String name;
	private int size;
	private Connection conn = null;
	
	PlayerAreaStats(Connection conn, Player player, String name, int size)
	{
		this.player = player;
		this.name = name;
		this.size = size;
		this.conn = conn;
	}
	public void run()
	{
		HashSet<String> types = new HashSet<String>();
		HashMap<String, Integer> created = new HashMap<String, Integer>();
		HashMap<String, Integer> destroyed = new HashMap<String, Integer>();
		
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT type, count(type) as num from blocks where type > 0 and player = ? and y > 0 and x > ? and x < ? and z > ? and z < ? group by type order by count(replaced) desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setInt(2, (int)player.getX()-size);
			ps.setInt(3, (int)player.getX()+size);
			ps.setInt(4, (int)player.getZ()-size);
			ps.setInt(5, (int)player.getZ()+size);
			rs = ps.executeQuery();
			while (rs.next())
			{
				types.add(etc.getDataSource().getItem(rs.getInt("type")));
				created.put(etc.getDataSource().getItem(rs.getInt("type")), rs.getInt("num"));
			}
			rs.close();
			ps.close();
			
			ps = conn.prepareStatement("SELECT replaced, count(replaced) as num from blocks where replaced > 0 and player = ? and y > 0 and x > ? and x < ? and z > ? and z < ? group by replaced order by count(replaced) desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setInt(2, (int)player.getX()-size);
			ps.setInt(3, (int)player.getX()+size);
			ps.setInt(4, (int)player.getZ()-size);
			ps.setInt(5, (int)player.getZ()+size);
			rs = ps.executeQuery();
			while (rs.next())
			{
				types.add(etc.getDataSource().getItem(rs.getInt("replaced")));
				destroyed.put(etc.getDataSource().getItem(rs.getInt("replaced")), rs.getInt("num"));
			}
			
		} catch (SQLException ex) {
			log.log(Level.SEVERE, this.getClass().getName() + " SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, this.getClass().getName() + " SQL exception on close", ex);
			}
		}

		player.sendMessage(Colors.Blue + "Player " + name + " within " + size + " blocks of you: ");
		if (types.size() == 0)
		{
			player.sendMessage(Colors.Blue + "No results found.");
			return;
		}
		
		player.sendMessage(Colors.Gold + String.format("%-6s %-6s %s", "Creat", "Destr", "Block"));
		for (String t: types)
		{
			Integer c = created.get(t);
			Integer d = destroyed.get(t);
			if (c == null)
				c = 0;
			if (d == null)
				d = 0;
			player.sendMessage(Colors.Gold + String.format("%-6d %-6d %s", c, d, t));
		}
	}
}
