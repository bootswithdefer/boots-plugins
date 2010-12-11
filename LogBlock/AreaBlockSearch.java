import java.util.HashSet;
import java.util.HashMap;
import java.text.SimpleDateFormat;

import java.util.logging.*;
import java.sql.*;

public class AreaBlockSearch implements Runnable
{
	static final Logger log = Logger.getLogger("Minecraft");
	private Player player;
	private Location location;
	private int type;
	private int size;
	private Connection conn = null;
	
	AreaBlockSearch(Connection conn, Player player, int type, int size)
	{
		this.player = player;
		this.location = player.getLocation();
		this.type = type;
		this.size = size;
		this.conn = conn;
	}
	public void run()
	{
		boolean hist = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Timestamp date;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd hh:mm:ss");
		
		try {
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * from blocks where (type = ? or replaced = ?) and y > ? and y < ? and x > ? and x < ? and z > ? and z < ? order by date desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, type);
			ps.setInt(2, type);
			ps.setInt(3, (int)(location.y) - size);
			ps.setInt(4, (int)(location.y) + size);
			ps.setInt(5, (int)(location.x) - size);
			ps.setInt(6, (int)(location.x) + size);
			ps.setInt(7, (int)(location.z) - size);
			ps.setInt(8, (int)(location.z) + size);
			rs = ps.executeQuery();

			player.sendMessage(Colors.Blue + "Block history within " + size + " blocks of  " + (int)(location.x) + ", " + (int)(location.y) + ", " + (int)(location.z) + ": ");

			while (rs.next())
			{
				date = rs.getTimestamp("date");
				String datestr = formatter.format(date);
				String msg = datestr + " " + rs.getString("player") + " (" + rs.getInt("x") + ", " + rs.getInt("y") + ", " + rs.getInt("z") + ") ";
				if (rs.getInt("type") == 0)
					msg = msg + "destroyed " + etc.getDataSource().getItem(rs.getInt("replaced"));
				else if (rs.getInt("replaced") == 0)
					msg = msg + "created " + etc.getDataSource().getItem(rs.getInt("type"));
				else
					msg = msg + "replaced " + etc.getDataSource().getItem(rs.getInt("replaced")) + " with " + etc.getDataSource().getItem(rs.getInt("type"));
				player.sendMessage(Colors.Gold + msg);
				hist = true;
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
		if (!hist)
			player.sendMessage(Colors.Blue + "None.");
	}
}
