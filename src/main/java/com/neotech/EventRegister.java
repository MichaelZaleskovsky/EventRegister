package com.neotech;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class EventRegister {

	private static Config config;
	private static Queue<Long> queue = new ConcurrentLinkedQueue<Long>();

	public static void main(String[] args) {
		
		config = getConfigFromFile();
		if (config == null) {
			System.out.println("File config.xml not found or has wrong format!");
			return;
		}
		
		if (args.length == 0) {
			dbWriteProcess();
			return;
		} else if (args[0].equals("-p") && args.length >= 2) {
			try {
				dbPrint(Integer.parseInt(args[1]));
				return;
			} catch (NumberFormatException e) {
				System.out.println("Second parameter must be digit!");
			}
		} else if (args[0].equals("-u") && args.length >= 2) {
			dbFileUpdate(args[1]);
			return;
		} else if (args[0].equals("-c")) {
			if (dbСontinuity()) {
				System.out.println("Database continuous!");
			} else {
				System.out.println("Database NOT continuous!");
			}
			return;
		}
			
		System.out.println("Invalid parameter " + args[0] + " is detected!");
		System.out.println("Please use:");
		System.out.println("eventregister                 // for start register process");
		System.out.println("eventregister -p serverNum    // for print database to console, serverNum = 0 for base server, 1, 2, 3... for reserve");
		System.out.println("eventregister -u filename     // for update database from file 'filename'");
		System.out.println("eventregister -c     		  // to check is the database continuous or not");
		return;
	}
	
	public static Queue<Long> getQueue() {
		return queue;
	}
	
	public static Config getConfig() {
		return config;
	}

	private static Config getConfigFromFile() {
		File file = new File("config.xml");
		StringBuilder sb = new StringBuilder();
	    XmlMapper mapper = new XmlMapper(); 
	    String configStr;
	    
		try (Scanner scanner = new Scanner(file)) 
		{
			System.out.println("Reading configuration from config.xml");
			
			while (scanner.hasNextLine()) {
				sb.append(scanner.next());
			}
			configStr = sb.toString();
			
			Config config = mapper.readValue(configStr, Config.class);
			return config;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void dbFileUpdate(String fileName) {
		File file = new File(fileName);
		StringBuilder sb = new StringBuilder();
	    String sqlQuery;
		String sqlInsert = "INSERT INTO " + config.getTableName() + " VALUES "; 
		Server server = config.getServers().get(0);
	    
		try (
				Connection connection = DriverManager.getConnection(server.getUrl(), server.getUser(), server.getPassword());
				Statement statement = connection.createStatement(); 
				Scanner scanner = new Scanner(file); ) 
		{
			sb.append(sqlInsert);
			while (scanner.hasNextLine()) {
				sb.append("(");
				sb.append(scanner.next());
				sb.append("), ");
			}
			sqlQuery = sb.toString().substring(0, sb.length()-2);
			statement.executeUpdate(sqlQuery);
			System.out.println("Base database server updated successfully");
		} catch (IOException | SQLException e ) {
			e.printStackTrace();
		}
	}

	private static void dbPrint(int serverNum) {
		System.out.println("Print all data from database.");
		Server server = config.getServers().get(serverNum);
		String sqlSelect = "SELECT * FROM " + config.getTableName();
		try (
			Connection connection = DriverManager.getConnection(server.getUrl(), server.getUser(), server.getPassword());
			Statement statement = connection.createStatement(); 
			ResultSet result = statement.executeQuery(sqlSelect);)
		{
			while(result.next())
			{
			    System.out.println(result.getString(1));    
			}
		} catch (SQLException e) {
			System.out.println("Connection lost or database is incompatible.");
		}
	}

	private static void dbWriteProcess() {
		new Thread(new EventEmitter()).start();
		new Thread(new DatabaseWriter()).start();
	}

	private static boolean dbСontinuity() {
		System.out.println("Check the continuity of database.");
		Server server = config.getServers().get(0);
		String sqlSelect = "SELECT * FROM " + config.getTableName();
		try (
			Connection connection = DriverManager.getConnection(server.getUrl(), server.getUser(), server.getPassword());
			Statement statement = connection.createStatement(); 
			ResultSet result = statement.executeQuery(sqlSelect);)
		{
			long prev = 0;
			long curr; 
			while(result.next())
			{
			    curr = Long.parseLong(result.getString(1)); 
			    if (prev > 0 && ((curr - prev) < 0 || (curr - prev) > 1500)) {
			    	return false;
			    }
			}
		} catch (SQLException e) {
			System.out.println("Connection lost or database is incompatible.");
			return false;
		}
		return true;
	}
}
