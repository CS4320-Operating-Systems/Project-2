import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OSThreadProcesses {


    static class ProcessThread extends Thread {
        private final int pid;
        private final int arrivalTime;
        private final int burstTime;
        private final char role;

        public ProcessThread(int pid, int arrivalTime, int burstTime, char role) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.role = role;
        }

        public void run() {
            log("Starting execution for " + pid);
            try {
                Thread.sleep(arrivalTime * 1000L);
            } catch (InterruptedException e) {
                log("Interrupted while waiting to arrive");
                return;
            }
            System.out.println("Process " + pid + " finished.");

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
