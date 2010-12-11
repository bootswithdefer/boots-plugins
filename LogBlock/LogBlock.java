import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.text.SimpleDateFormat;

import java.util.logging.*;
import java.sql.*;
import java.io.*;

import net.minecraft.server.MinecraftServer;

public class LogBlock extends Plugin
{
	private static String name = "LogBlock";
	private static int version = 12;
	private boolean debug = false;
	private String dbDriver = "com.mysql.jdbc.Driver";
	private String dbUrl = "";
	private String dbUsername = "";
	private String dbPassword = "";
	private boolean usehModDb = false;
	private int delay = 10;
	private int defaultDist = 6;
	private int toolID = 270; // 270 is wood pick axe
	private int toolblockID = 7; // 78 is adminium
	private boolean toolblockRemove = true;
	private Consumer consumer = null;
	private Block lastface = null;
	
	private LinkedBlockingQueue<BlockRow> bqueue = new LinkedBlockingQueue<BlockRow>();
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	static final Logger lblog = Logger.getLogger(name);
	
	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("logblock.properties");
		try {
			debug = properties.getBoolean("debug", false);
			usehModDb = properties.getBoolean("use-hmod-db", false);
			dbDriver = properties.getString("driver", "com.mysql.jdbc.Driver");
			dbUrl = properties.getString("url", "jdbc:mysql://localhost:3306/db");
			dbUsername = properties.getString("username", "user");
			dbPassword = properties.getString("password", "pass");
			delay = properties.getInt("delay", 6);
			toolID = properties.getInt("tool-id", 270);
			toolblockID = properties.getInt("tool-block-id", 7);
			toolblockRemove = properties.getBoolean("tool-block-remove", true);
			defaultDist = properties.getInt("default-distance", 10);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Exception	while	reading from logblock.properties",	ex);
		}		
		try {
			if (!usehModDb)
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
		if (consumer != null)
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
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_PLACE, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_BROKEN, listener, this, PluginListener.Priority.LOW);
	}
	
	private Connection getConnection() throws SQLException
	{
		if (usehModDb)
			return etc.getSQLConnection();
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
			ps = conn.prepareStatement("SELECT * from blocks where y = ? and x = ? and z = ? order by date desc limit 10", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, b.getY());
			ps.setInt(2, b.getX());
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
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, name + " SQL exception on close", ex);
			}
		}
		if (!hist)
			player.sendMessage(Colors.Blue + "None.");
	}

	private void queueBlock(Player player, Block before, Block after)
	{
		Block b = null;
		int typeA = 0;
		int typeB = 0;
		if (after != null)
		{
			typeA = after.getType();
			b = after;
		}
		if (before != null)
		{
			typeB = before.getType();
			b = before;
		}

		if (b == null || typeA < 0 || typeB < 0)
			return;
			
		BlockRow row = new BlockRow(player.getName(), typeB, typeA, b.getX(), b.getY(), b.getZ());
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
				
	      if (split[0].equalsIgnoreCase("/lb"))
			{
				Connection conn;
				try {
					conn = getConnection();
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception", ex);
					player.sendMessage(Colors.Rose + "Error, check server logs.");
					return true;
				}
				
				if (split.length == 1) {
					AreaStats th = new AreaStats(conn, player, defaultDist);
					new Thread(th).start();
					return true;
				}
				
				if (split.length == 2) {
					if (split[1].equalsIgnoreCase("world")) {
						PlayerWorldStats th = new PlayerWorldStats(conn, player);
						new Thread(th).start();
						return true;
					}
					player.sendMessage(Colors.Rose + "Incorrect usage.");
					return true;
				}
				
				if (split[1].equalsIgnoreCase("player")) {
					PlayerAreaStats th = new PlayerAreaStats(conn, player, split[2], defaultDist);
					new Thread(th).start();
					return true;
				}
				
				if (split[1].equalsIgnoreCase("area")) {
					AreaStats th = new AreaStats(conn, player, Integer.parseInt(split[2]));
					new Thread(th).start();
					return true;
				}

				if (split[1].equalsIgnoreCase("block")) {
					int type = etc.getDataSource().getItem(split[2]);
					AreaBlockSearch th = new AreaBlockSearch(conn, player, type, defaultDist);
					new Thread(th).start();
					return true;
				}

				player.sendMessage(Colors.Rose + "Incorrect usage.");
				return true;
			}
			return false;
		}

		public void onBlockRightClicked(Player player, Block blockClicked, Item item)
		{
			if (item.getItemId() == toolID && player.canUseCommand("/blockhistory"))
			{
				showBlockHistory(player, blockClicked);
				return;
			}
				
			lastface = blockClicked.getFace(blockClicked.getFaceClicked());
//			if (debug)
//				lblog.info("onBlockRightClicked: clicked " + blockClicked.getType() + " item " + item.getItemId() + " face " + blockClicked.getFace(blockClicked.getFaceClicked()).getType());
		}		

		public boolean onBlockPlace(Player player, Block blockPlaced, Block blockClicked, Item itemInHand)
		{
			if (itemInHand.getItemId() == toolblockID && player.canUseCommand("/blockhistory"))
			{
				showBlockHistory(player, blockPlaced);
				if (toolblockRemove)
					return true;
				return false;
			}

//			if (debug)
//				lblog.info("onBlockPlace: placed " + blockPlaced.getType() + " clicked " + blockClicked.getType() + " item " + itemInHand.getItemId());

			queueBlock(player, lastface, blockPlaced);
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
			   long start = System.currentTimeMillis()/1000L;
				int count = 0;
				
				if (bqueue.size() > 100)
					log.info(name + " queue size " + bqueue.size());
					
//				if (debug)
//					lblog.info("Running DB thread at " + start);
					
				try {
					conn = getConnection();
					conn.setAutoCommit(false);
					ps = conn.prepareStatement("INSERT INTO blocks (date, player, replaced, type, x, y, z) VALUES (now(),?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

					while (count < 100 && start+delay > (System.currentTimeMillis()/1000L))
					{
//						if (debug)
//							lblog.info("Loop DB thread at " + (System.currentTimeMillis()/1000L));
						
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
					if (debug && count > 0)
						lblog.info("Commiting " + count + " inserts.");
					conn.commit();
				} catch (InterruptedException ex) {
					log.log(Level.SEVERE, name + " interrupted exception", ex);
				} catch (SQLException ex) {
					log.log(Level.SEVERE, name + " SQL exception", ex);
				} finally {
					try {
						if (ps != null)
							ps.close();
						if (conn != null)
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
