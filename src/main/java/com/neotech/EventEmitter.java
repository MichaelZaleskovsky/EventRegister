package com.neotech;

import java.util.Date;
import java.util.Queue;

public class EventEmitter implements Runnable {

	public void run() {
		Queue<Long> que = EventRegister.getQueue();
		long timestamp;
		
		while(true) {
			timestamp = new Date().getTime();
			que.offer(timestamp);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
