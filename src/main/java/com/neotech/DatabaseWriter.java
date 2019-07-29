package com.neotech;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DatabaseWriter implements Runnable {

	ThreadLocal<Queue<Long>> localQue = new ThreadLocal<>();
	
	public void run() {
		
		System.out.println("Data saving process started");
		
		Config config = EventRegister.getConfig();
		Queue<Long> que = EventRegister.getQueue();
		
        try
        {
            Class.forName(config.getDriver());
        }
        catch (ClassNotFoundException e) {
            System.out.println(config.getDriver() + " not found !!");
            return;
        }

		String sqlQuery;
		String sqlInsert = "INSERT INTO " + config.getTableName() + " VALUES "; 
		String sqlCreateTable = "CREATE TABLE IF NOT EXISTS `events`.`" + config.getTableName() + "` (`data` BIGINT(20) NOT NULL)";
		int size;
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
					System.out.println("All reserve servers done or extremally slow. \n "
							+ "Data temporary saved to file '" + fileName + "'\n"
							+ "Restore servers accessibility and use 'eventregister -u " + fileName + "' to update database. \n"
							+ "Then type CONTINUE to resume procees of data saving. \n"
							+ "All data for waiting period will be saved.");
					while (!cont.toLowerCase().equals("continue")) {
						cont = sc.next();
					}
					currentServer = 0;
					cont = "";
					System.out.println("Resume saving process to server " + servers.get(currentServer).getUrl());
				} else {
					System.out.println("Switched to reserve server " + servers.get(currentServer).getUrl() + " at time " + que.peek());
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
					localQue.set(que);
					size = localQue.get().size();
					if (size > 0) {
						sqlQuery = localQue.get().stream()
								.map((t) -> "("+t.toString()+")")
								.collect(Collectors.joining(", ", sqlInsert, ""));
						statement.executeUpdate(sqlQuery);
						IntStream.range(0, size).forEach(i -> que.poll());
					}
				}
			} catch (SQLException e) {
				System.out.println("Connection lost! Try to reconnect in " + config.getDelay() + " seconds.");
				try {
					Thread.sleep(config.getDelay()*1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} 
		}
	}

	private String writeBufferToFile(Queue<Long> que) {
		File file = new File("register" + que.peek() + ".data");
		BufferedWriter writer;
		localQue.set(que);
		int size = localQue.get().size();
		String data = localQue.get().stream()
				.map((t) -> t.toString())
				.collect(Collectors.joining("\n"));
		try {
			file.createNewFile();
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(data);
			writer.flush();
			writer.close();
			IntStream.range(0, size).forEach(i -> que.poll());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return file.getName();
	}

}
