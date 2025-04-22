//Required imports for the program
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class OSThreadProcesses {
	private static final Semaphore resourceLock = new Semaphore(1);    //Create semaphore for shared resource with one permit only and reentrant lock to protect readCount variable
	private static final ReentrantLock readCountLock = new ReentrantLock();    
	private static int readCount = 0; // Initialize active reader count to zero

	//Declare static inner class ProcessThread that extends Thread
	static class ProcessThread extends Thread {
		private final int pid; //process identifier pid
		private final int arrivalTime; //arrival time in seconds
		private final int burstTime; //burst time in seconds
		private final char role; //process role character R for reader and W for writer

		//Constructor for ProcessThread receiving pid, arrivalTime, burstTime, and role
		public ProcessThread(int pid, int arrivalTime, int burstTime, char role) {
			this.pid = pid;    //Set instance pid to parameter pid
			this.arrivalTime = arrivalTime;    //Set instance arrivalTime to parameter arrivalTime
			this.burstTime = burstTime;    //Set instance burstTime to parameter burstTime
			this.role = role; //Set instance role to parameter role
		}

		//Override run method for thread execution
		public void run() {
			//Thread waits for its own arrival time by sleeping for arrivalTime seconds converted to milliseconds
			try {
				Thread.sleep(arrivalTime * 1000L);
			} catch (InterruptedException e) {
				log("Interrupted while waiting to arrive");
				return;
			}

			//Log start of execution
			log("Starting execution");

			if (role == 'R') {
				//Begin readers-writers protocol for reader
				try {
					readCountLock.lock();
					if (readCount == 0) {
						log("Waiting to acquire resource lock");
						resourceLock.acquire();
						log("Acquired resource lock");
					}
					readCount++;
				} catch (InterruptedException e) {
					log("Interrupted while acquiring resource lock");
					return;
				} finally {
					readCountLock.unlock();
				}
				//Log reading operation with CPU burst time
				log(String.format("Reading (CPU burst %d sec)", burstTime));
				try {
					Thread.sleep(burstTime * 1000L);
				} catch (InterruptedException e) {
					log("Interrupted during reading");
				}
				//Begin reader exit section to decrement active reader count
				try {
					readCountLock.lock();
					readCount--;
					if (readCount == 0) {
						resourceLock.release();
						log("Released resource lock");
					}
				} finally {
					readCountLock.unlock();
				}
			} else if (role == 'W') {
				//Log message waiting to acquire resource lock for writer
				log("Waiting to acquire resource lock");
				try {
					resourceLock.acquire();
				} catch (InterruptedException e) {
					log("Interrupted while waiting to acquire resource lock");
					return;
				}
				//Log message acquired resource lock for writer
				log("Acquired resource lock");
				//Log writing operation with CPU burst time
				log(String.format("Writing (CPU burst %d sec)", burstTime));
				try {
					Thread.sleep(burstTime * 1000L);
				} catch (InterruptedException e) {
					log("Interrupted during writing");
				}
				//Release resource lock after writing and log message
				resourceLock.release();
				log("Released resource lock");
			}

			//Log finished execution of thread
			log("Finished execution");
		}

		//Helper method to log messages with thread label
		private void log(String message) {
			System.out.println(String.format(">>> [%s] %s", getLabel(), message));
		}

		//Helper method to generate thread label based on role and pid
		private String getLabel() {
			return (role == 'R' ? "Reader PID: " : "Writer PID: ") + pid;
		}
	}

	//Main method entry point of the program
	public static void main(String[] args) {
		//Create list to store ProcessThread objects
		List<ProcessThread> processList = new ArrayList<>();

		//Try with resources to create BufferedReader to read processes.txt file
		try (BufferedReader br = new BufferedReader(new FileReader("Thread-Based Process Simulation and Synchronization/processes.txt"))) {
			String line = br.readLine();
			if (line != null && line.trim().startsWith("PID")) {
				line = br.readLine();
			}
			while (line != null) {
				String[] parts = line.trim().split("\\s+");
				if (parts.length < 4) {
					line = br.readLine();
					continue;
				}
				try {
					int pid = Integer.parseInt(parts[0]);//Parse first element as process id
					int arrival = Integer.parseInt(parts[1]);//Parse second element as arrival time
					int burst = Integer.parseInt(parts[2]);//Parse third element as burst time
					char role = parts[3].charAt(0);//Parse fourth element as role
					ProcessThread pt = new ProcessThread(pid, arrival, burst, role);//Create new ProcessThread with parsed parameters
					processList.add(pt);//Add ProcessThread to processList
				} catch (NumberFormatException e) {
				}
				line = br.readLine();
			}
		} catch (IOException e) {
			//Print error reading processes file to error output
			System.err.println("Error reading processes file " + e.getMessage());
			return;
		}

		//Sort processList by arrival time for easier scheduling observation
		Collections.sort(processList, Comparator.comparingInt(p -> p.arrivalTime));

		//Show arrival order: Arrival_Time, PID, Burst_Time, and Role
		System.out.printf("%-15s%-5s%-12s%s%n", "Arrival_Time", "PID", "Burst_Time", "Role");
		for (ProcessThread pt : processList) {
			System.out.printf("%-15d%-5d%-12d%c%n", pt.arrivalTime, pt.pid, pt.burstTime, pt.role);
		}

		//Start all process threads concurrently
		for (ProcessThread pt : processList) {
			pt.start();
		}

		//Wait for all threads to complete execution
		for (ProcessThread pt : processList) {
			try {
				pt.join();//Join thread pt and wait for its termination
			} catch (InterruptedException e) {
			}
		}
	}
}