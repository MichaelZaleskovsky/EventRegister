package com.neotech;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DatabaseWriter implements Runnable {

	private Counter counter = new Counter();
	
	private Config config = EventRegister.getConfig();
	private Queue<Long> que = EventRegister.getQueue();
	private Logger log = EventRegister.getLog();
	
	public void run() {
		
        try
        {
            Class.forName(config.getDriver());
        } catch (ClassNotFoundException e) {
            System.out.println(config.getDriver() + " is not found !!");
            return;
        }

		log.info("Data saving process started at " + new Date().getTime());
		
		String sqlQuery;
		String sqlInsert = "INSERT INTO " + EventRegister.TABLE + " VALUES "; 
		String sqlCreateTable = "CREATE TABLE IF NOT EXISTS `events`.`" + EventRegister.TABLE + "` (`data` BIGINT(20) NOT NULL)";
		int currentServer = 0;
		List<Server> servers = config.getServers();
		int numberOfServers = servers.size();
		Server server = servers.get(currentServer);
		Scanner sc = new Scanner(System.in);
		String cont = "";
		
		while(true) {
			
			// If buffer size is too big change server to reserve or write data to file
			if (que.size() > config.getMaxBufferSize()) {
				currentServer++;
				if (currentServer >= numberOfServers) {
					String fileName = writeBufferToFile(que);
					log.info("All reserved servers failed or extremally slow. \n "
							+ "Data temporary is saved to file '" + fileName + "'\n"
							+ "Restore servers accessibility and use 'eventregister -u " + fileName + "' to update database. \n"
							+ "Then type CONTINUE to resume process of data saving. \n"
							+ "All data for waiting period will be saved.");
					
					// By typing CONTINUE the customer confirms, that working condition of SQL server is restored and database is  updated
					// Afterwards the data from buffer will be saved to database and the process will be continued
					while (!cont.toLowerCase().equals("continue")) {
						cont = sc.next();
					}
					currentServer = 0;
					cont = "";
					log.info("Saving process to server has been resumed" + servers.get(currentServer).getUrl());
				} else {
					log.info("Output stream has been switched to reserve server " + servers.get(currentServer).getUrl() + " at time " + que.peek());
				}
				server = servers.get(currentServer);
			}
			
			//Try to setup connection and start circle of saving data to database
			try (
					Connection connection = DriverManager.getConnection(server.getUrl(), server.getUser(), server.getPassword());
					Statement statement = connection.createStatement();) 
			{
				statement.executeUpdate(sqlCreateTable);
				while (true) {
					synchronized (que) {
						try {
							que.wait();
						} catch (InterruptedException e) {}
					}
					
					counter.set(0);
					sqlQuery = que.stream()
							.map(t -> {
								counter.plus();
								return "("+t.toString()+")";
							})
							.collect(Collectors.joining(", ", sqlInsert, ""));
					statement.executeUpdate(sqlQuery);
					IntStream.range(0, counter.get()).forEach(i -> que.poll());
				}
			} catch (SQLException e) {
				log.info("Connection is lost! Try to reconnect in " + config.getDelay() + " seconds.");
				try {
					Thread.sleep(config.getDelay()*1000);
				} catch (InterruptedException e1) {
					log.info(e1.toString());
				}
			} 
		} 
	}

	private String writeBufferToFile(Queue<Long> que) {
		File file = new File("register" + que.peek() + ".data");
		BufferedWriter writer;
		counter.set(0);
		String data = que.stream()
				.map(t -> {
					counter.plus();
					return t.toString();
				})
				.collect(Collectors.joining("\n"));
		try {
			file.createNewFile();
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(data);
			writer.flush();
			writer.close();
			IntStream.range(0, counter.get()).forEach(i -> que.poll());
		} catch (IOException e) {
			log.info("Unable to write file!");
		}
		
		return file.getName();
	}

}

class Counter {
	private int i;

	public int get() {
		return i;
	}

	public void set(int i) {
		this.i = i;
	}
	
	public void plus() {
		i++;
	}
	
}

