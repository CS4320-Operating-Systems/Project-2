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
	//Create semaphore for shared resource with one permit only and reentrant lock to protect readCount variable
	private static final Semaphore resourceLock = new Semaphore(1);	
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
			this.pid = pid;	//Set instance pid to parameter pid
			this.arrivalTime = arrivalTime;	//Set instance arrivalTime to parameter arrivalTime
			this.burstTime = burstTime;	//Set instance burstTime to parameter burstTime
			this.role = role; //Set instance role to parameter role
		}

		//Override run method for thread execution
		public void run() {
			//Thread waits for its own arrival time by sleeping for arrivalTime seconds converted to milliseconds
			try {
				//Sleep for arrivalTime times one thousand milliseconds
				Thread.sleep(arrivalTime * 1000L);
			} catch (InterruptedException e) {
				//Log interruption message for waiting to arrive
				log("Interrupted while waiting to arrive");
				return;
			}

			//Log start of execution
			log("Starting execution");

			//If role equals R then process is a reader
			if (role == 'R') {
				//Begin readers-writers protocol for reader
				try {
					//Lock readCountLock to update readCount
					readCountLock.lock();
					//If no active readers then acquire resource lock for reading
					if (readCount == 0) {
						//Log message waiting to acquire resource lock and then aquire and log message
						log("Waiting to acquire resource lock");
						resourceLock.acquire();
						log("Acquired resource lock");
					}
					//Increment count of active readers
					readCount++;
				} catch (InterruptedException e) {
					//Log interruption while acquiring resource lock
					log("Interrupted while acquiring resource lock");
					return;
				} finally {
					//Unlock readCountLock
					readCountLock.unlock();
				}
				//Log reading operation with CPU burst time
				log(String.format("Reading (CPU burst %d sec)", burstTime));
				try {
					//Sleep for burstTime multiplied by one thousand milliseconds to simulate reading
					Thread.sleep(burstTime * 1000L);
				} catch (InterruptedException e) {
					//Log interruption during reading
					log("Interrupted during reading");
				}
				//Begin reader exit section to decrement active reader count
				try {
					//Lock readCountLock to update readCount and degrement count
					readCountLock.lock();
					readCount--;
					//If no more active readers then release resource lock
					if (readCount == 0) {
						//Release resource lock semaphore and log message
						resourceLock.release();
						log("Released resource lock");
					}
				} finally {
                    readCountLock.unlock();
                }
            } else if (role == 'W') {
                log("Waiting to acquire resource lock");
                try {
                    resourceLock.acquire();
                } catch (InterruptedException e) {
                    log("Interrupted while waiting to acquire resource lock");
                    return;
                }
                log("Acquired resource lock");
                log(String.format("Writing (CPU burst %d sec)", burstTime));
                try {
                    Thread.sleep(burstTime * 1000L);
                } catch (InterruptedException e) {
                    log("Interrupted during writing");
                }
                resourceLock.release();
                log("Released resource lock");
            }

            log("Finished execution");
        }

        private void log(String message) {
            System.out.println(String.format(">>> [%s] %s", getLabel(), message));
        }

        private String getLabel() {
            return (role == 'R' ? "Reader " : "Writer ") + pid;
        }
    }

    public static void main(String[] args) {
        List<ProcessThread> processList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("Thread-Based Process Simulation and Synchronization/processes.txt"))) {
            String line = br.readLine();
            if (line != null && line.trim().startsWith("PID")) {
                line = br.readLine();
            }
            while (line != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 5) {
                    line = br.readLine();
                    continue;
                }
                try {
                    int pid = Integer.parseInt(parts[0]);
                    int arrival = Integer.parseInt(parts[1]);
                    int burst = Integer.parseInt(parts[2]);
                    char role = parts[4].charAt(0);
                    ProcessThread pt = new ProcessThread(pid, arrival, burst, role);
                    processList.add(pt);
                } catch (NumberFormatException e) {
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.err.println("Error reading processes file " + e.getMessage());
            return;
        }

        Collections.sort(processList, Comparator.comparingInt(p -> p.arrivalTime));

        for (ProcessThread pt : processList) {
            pt.start();
        }

        for (ProcessThread pt : processList) {
            try {
                pt.join();
            } catch (InterruptedException e) {
            }
        }
    }
}