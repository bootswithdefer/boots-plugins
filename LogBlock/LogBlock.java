import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.logging.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.text.SimpleDateFormat;

import java.sql.*;
import java.io.*;

import net.minecraft.server.MinecraftServer;

public class LogBlock extends Plugin
{
	private static String name = "LogBlock";
	private static int version = 8;
	private boolean debug = false;
	private String dbDriver = "com.mysql.jdbc.Driver";
	private String dbUrl = "";
	private String dbUsername = "";
	private String dbPassword = "";
	private int delay = 10;
	private int defaultDist = 20;
	private int toolID = 270; // 270 is wood pick
	private Consumer consumer = null;
	
	private LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	static final Logger lblog = Logger.getLogger(name);
	
	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("logblock.properties");
		try {
			debug = properties.getBoolean("debug", false);
			dbDriver = properties.getString("driver", "com.mysql.jdbc.Driver");
			dbUrl = properties.getString("url", "jdbc:mysql://localhost:3306/db");
			dbUsername = properties.getString("username", "user");
			dbPassword = properties.getString("password", "pass");
			delay = properties.getInt("delay", 10);
			toolID = properties.getInt("tool-id", 270);
			defaultDist = properties.getInt("default-distance", 20);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception	while	reading from logblock.properties",	ex);
		}		
		try {
			new JDCConnectionDriver(dbDriver, dbUrl, dbUsername, dbPassword);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception while creation database connection pool", ex);
			return;
		}

		consumer = new Consumer();
		new Thread(consumer).start();
		log.info(name + " v" + version + " Plugin Enabled.");
	}

	public void disable()
	{
		consumer.stop();
		consumer = null;
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		try {
			FileHandler lbfh = new FileHandler(name + ".log", true);
			lbfh.setFormatter(new LogFormatter());
			lblog.addHandler(lbfh);
		} catch (IOException ex) {
			log.info(name + " unable to create logger");
		}

		new VersionCheck(name, version);
		
		LBListener listener = new LBListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_BROKEN, listener, this, PluginListener.Priority.LOW);
	}
	
	private Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection("jdbc:jdc:jdcpool");
	}
	
	private void showBlockHistory(Player player, Block b)
	{
		player.sendMessage(Colors.Blue + "Block history (" + b.getX() + ", " + b.getY() + ", " + b.getZ() + "): ");
		boolean hist = false;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Timestamp date;
		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd hh:mm:ss");
		
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			ps = conn.prepareStatement("SELECT * from blocks where x = ? and y = ? and z = ? order by date desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, b.getX());
			ps.setInt(2, b.getY());
			ps.setInt(3, b.getZ());
			rs = ps.executeQuery();
			while (rs.next())
			{
				date = rs.getTimestamp("date");
				String datestr = formatter.format(date);
				String msg = datestr + " " + rs.getString("player") + " ";
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
			log.log(Level.SEVERE, name + " SQL exception", ex);
		} finally {
			try {
				rs.close();
				ps.close();
				conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, name + " SQL exception on close", ex);
			}
		}
		if (!hist)
			player.sendMessage(Colors.Blue + "None.");
	}

	private class AreaStats implements Runnable
	{
		private Player player;
		private int size;
		AreaStats(Player player, int size)
		{
			this.player = player;
			this.size = size;
		}
		public void run()
		{
			HashSet<String> players = new HashSet<String>();
			HashMap<String, Integer> created = new HashMap<String, Integer>();
			HashMap<String, Integer> destroyed = new HashMap<String, Integer>();
			
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
	
			try {
				conn = getConnection();
				conn.setAutoCommit(false);
				ps = conn.prepareStatement("SELECT player, count(player) as num from blocks where type > 0 and x > ? and x < ? and z > ? and z < ? group by player order by count(player) desc limit 10", Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, (int)player.getX()-size);
				ps.setInt(2, (int)player.getX()+size);
				ps.setInt(3, (int)player.getZ()-size);
				ps.setInt(4, (int)player.getZ()+size);
				rs = ps.executeQuery();
				while (rs.next())
				{
					players.add(rs.getString("player"));
					created.put(rs.getString("player"), rs.getInt("num"));
				}
				rs.close();
				ps.close();
				
				ps = conn.prepareStatement("SELECT player, count(player) as num from blocks where replaced > 0 and x > ? and x < ? and z > ? and z < ? group by player order by count(player) desc limit 10", Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, (int)player.getX()-size);
				ps.setInt(2, (int)player.getX()+size);
				ps.setInt(3, (int)player.getZ()-size);
				ps.setInt(4, (int)player.getZ()+size);
				rs = ps.executeQuery();
				while (rs.next())
				{
					players.add(rs.getString("player"));
					destroyed.put(rs.getString("player"), rs.getInt("num"));
				}
				
			} catch (SQLException ex) {
				log.log(Level.SEVERE, name + " SQL exception", ex);
			} finally {
				try {
					rs.close();
					ps.close();
					conn.close();
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception on close", ex);
				}
			}
	
			player.sendMessage(Colors.Blue + "Within " + size + " blocks of you: ");
			if (players.size() == 0)
			{
				player.sendMessage(Colors.Blue + "No results found.");
				return;
			}
			
			player.sendMessage(Colors.Gold + String.format("%-6s %-6s %s", "Creat", "Destr", "Player"));
			for (String p: players)
			{
				Integer c = created.get(p);
				Integer d = destroyed.get(p);
				if (c == null)
					c = 0;
				if (d == null)
					d = 0;
				player.sendMessage(Colors.Gold + String.format("%-6d %-6d %s", c, d, p));
			}
		}
	}

	private class PlayerAreaStats implements Runnable
	{
		private Player player;
		private String name;
		private int size;
		PlayerAreaStats(Player player, String name, int size)
		{
			this.player = player;
			this.name = name;
			this.size = size;
		}
		public void run()
		{
			HashSet<String> types = new HashSet<String>();
			HashMap<String, Integer> created = new HashMap<String, Integer>();
			HashMap<String, Integer> destroyed = new HashMap<String, Integer>();
			
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
	
			try {
				conn = getConnection();
				conn.setAutoCommit(false);
				ps = conn.prepareStatement("SELECT type, count(type) as num from blocks where type > 0 and player = ? and x > ? and x < ? and z > ? and z < ? group by type order by count(replaced) desc limit 10", Statement.RETURN_GENERATED_KEYS);
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
				
				ps = conn.prepareStatement("SELECT replaced, count(replaced) as num from blocks where replaced > 0 and player = ? and x > ? and x < ? and z > ? and z < ? group by replaced order by count(replaced) desc limit 10", Statement.RETURN_GENERATED_KEYS);
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
				log.log(Level.SEVERE, name + " SQL exception", ex);
			} finally {
				try {
					rs.close();
					ps.close();
					conn.close();
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception on close", ex);
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

	private class PlayerWorldStats implements Runnable
	{
		private Player player;
		PlayerWorldStats(Player player)
		{
			this.player = player;
		}
		public void run()
		{
			HashSet<String> players = new HashSet<String>();
			HashMap<String, Integer> created = new HashMap<String, Integer>();
			HashMap<String, Integer> destroyed = new HashMap<String, Integer>();
			
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
	
			try {
				conn = getConnection();
				conn.setAutoCommit(false);
				ps = conn.prepareStatement("SELECT player, count(player) as num from blocks where type > 0 group by player order by count(player) desc limit 5", Statement.RETURN_GENERATED_KEYS);
				rs = ps.executeQuery();
				while (rs.next())
				{
					players.add(rs.getString("player"));
					created.put(rs.getString("player"), rs.getInt("num"));
				}
				rs.close();
				ps.close();
				
				ps = conn.prepareStatement("SELECT player, count(player) as num from blocks where replaced > 0 group by player order by count(player) desc limit 5", Statement.RETURN_GENERATED_KEYS);
				rs = ps.executeQuery();
				while (rs.next())
				{
					players.add(rs.getString("player"));
					destroyed.put(rs.getString("player"), rs.getInt("num"));
				}
				
			} catch (SQLException ex) {
				log.log(Level.SEVERE, name + " SQL exception", ex);
			} finally {
				try {
					rs.close();
					ps.close();
					conn.close();
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception on close", ex);
				}
			}
	
			player.sendMessage(Colors.Blue + "Within entire world:");
			if (players.size() == 0)
			{
				player.sendMessage(Colors.Blue + "No results found.");
				return;
			}
			
			player.sendMessage(Colors.Gold + String.format("%-6s %-6s %s", "Creat", "Destr", "Player"));
			for (String p: players)
			{
				Integer c = created.get(p);
				Integer d = destroyed.get(p);
				if (c == null)
					c = 0;
				if (d == null)
					d = 0;
				player.sendMessage(Colors.Gold + String.format("%-6d %-6d %s", c, d, p));
			}
		}
	}

	private void queueBlock(Player player, Block before, Block after)
	{
	   int type = 0;
	   if (after != null) {
			type = after.getType();
			if (type < 0)
				return;
		}
		if (before.getType() < 0)
			return;
		BlockRow row = new BlockRow(player.getName(), before.getType(), type, before.getX(), before.getY(), before.getZ());
		boolean result = bqueue.offer(row);
		if (debug)
			lblog.info(row.toString());
		if (!result)
			log.info(name + " failed to queue block for " + player.getName());
	}

	public class LBListener extends PluginListener // start
	{
	   public boolean onCommand(Player player, String[] split)
	   {
			if (!player.canUseCommand(split[0]))
				return false;
				
	      if (split[0].equalsIgnoreCase("/lb")) {
				if (split.length == 1) {
					AreaStats th = new AreaStats(player, defaultDist);
					new Thread(th).start();
					return true;
				}
				if (split.length == 2) {
					if (split[1].equalsIgnoreCase("world")) {
						PlayerWorldStats th = new PlayerWorldStats(player);
						new Thread(th).start();
					} else
						player.sendMessage(Colors.Rose + "Incorrect usage.");
					return true;
				}
				if (split[1].equalsIgnoreCase("player")) {
					PlayerAreaStats th = new PlayerAreaStats(player, split[2], defaultDist);
					new Thread(th).start();
				}
				else if (split[1].equalsIgnoreCase("area")) {
					AreaStats th = new AreaStats(player, Integer.parseInt(split[2]));
					new Thread(th).start();
				}
				else
					player.sendMessage(Colors.Rose + "Incorrect usage.");
				return true;
			}
			return false;
		}
	
		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand)
		{
			if (itemInHand == toolID && player.canUseCommand("/blockhistory"))
			{
				showBlockHistory(player, blockClicked);
				return true;
			}
		
			Block before = new Block(etc.getServer().getBlockIdAt(blockPlaced.getX(), blockPlaced.getY(), blockPlaced.getZ()), blockPlaced.getX(), blockPlaced.getY(), blockPlaced.getZ());
			
			if (before.getType() == blockPlaced.getType())
				return false;
				
			queueBlock(player, before, blockPlaced);
			return false;
		}
		
		public boolean onBlockBreak(Player player, Block block)
		{
			queueBlock(player, block, null);
			return false;
		}
	} // end LBListener

	private class Consumer implements Runnable // start
	{
		private boolean stop = false;
		Consumer() { stop = false; }
		public void stop() { stop = true; }
		public void run()
		{
			PreparedStatement ps = null;
			Connection conn = null;
			BlockRow b;
			
			while (!stop)
			{
			   long start = System.currentTimeMillis();
				int count = 0;
				
				if (bqueue.size() > 100)
					log.info(name + " queue size " + bqueue.size());
				
				try {
					conn = getConnection();
					conn.setAutoCommit(false);
					ps = conn.prepareStatement("INSERT INTO blocks (date, player, replaced, type, x, y, z) VALUES (now(),?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

					while (count < 100 && start+delay > (System.currentTimeMillis()/1000F))
					{
						b = bqueue.poll(1L, TimeUnit.SECONDS);
						if (b == null)
							continue;
						//b.log();
						ps.setString(1, b.name);
						ps.setInt(2, b.replaced);
						ps.setInt(3, b.type);
						ps.setInt(4, b.x);
						ps.setInt(5, b.y);
						ps.setInt(6, b.z);
						ps.executeUpdate();
						count++;
					}
					
					conn.commit();
				} catch (InterruptedException ex) {
					log.log(Level.SEVERE, name + " interrupted exception", ex);
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception", ex);
				} finally {
					try {
						ps.close();
						conn.close();
					} catch (SQLException ex) {
						log.log(Level.SEVERE, name + " SQL exception on close", ex);
					}
				}
			}
		}
	} // end LBDB
	
	private class BlockRow // start
	{
		public String name;
		public int replaced, type;
		public int x, y, z;
		
		BlockRow(String name, int replaced, int type, int x, int y, int z)
		{
			this.name = name;
			this.replaced = replaced;
			this.type = type;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public String toString()
		{
			return("name: " + name + " before type: " + replaced + " type: " + type + " x: " + x + " y: " + y + " z: " + z);
		}
	} // end BlockRow
	
	private class Result // start
	{
		public String player;
		public int created;
		public int destroyed;
		
		Result(String player, int c, int d)
		{
			this.player = player;
			this.created = c;
			this.destroyed = d;
		}
		
		public String toString()
		{
			return(String.format("%-6d %-6d %s", created, destroyed, player));
		}
	} // end Result

	private class LogFormatter extends Formatter //start
	{
		public String format(LogRecord rec)
		{
			return formatMessage(rec) + "\n";
		}
	} // end LogFormatter	
} // end LogBlock
