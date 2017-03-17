/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package snapshotwiththread;

import java.io.Serializable;
import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aditya Advani
 */
public class SnapshotWithThread extends UnicastRemoteObject implements SnapshotIF, Serializable, Runnable {

// Global variables
    static String NodeName = ""; // Name of current node
    static String NodeIP = ""; // IP address of current node
    static int port = 7394; // Port to be used at current node
    static int bal = 1000; // Starting balance (total money in system = this*3) 
    static int s_bal; // Balance at snapshot
    static int channel_from_1 = 0, channel_from_2 = 0, channel_from_3 = 0; // Incomming channel buffers
    static int timeout = 3; // Allowed no. steps before snapshot timeout
    static int snap_time; // Steps since snapshot started
    static boolean just_started = true; //first run
    static int step = 0; //steps since last snapshot
    static int steps_for_snapshot = 2; // Waits steps_for_snapshot steps between two snapshots
    static boolean snap = false; // Is snapshot active?
    static boolean received_from_p1 = false; // Received marker from p1?
    static boolean received_from_p2 = false; // Received marker from p2?
    static boolean received_from_p3 = false; // Received marker from p3?

    static boolean received_response_from_p2 = false;
    static boolean received_response_from_p3 = false;
    static String response_from_p2 = "";
    static String response_from_p3 = "";
    static String response_from_p1 = "";

    static final Object o = new Object(); // Synchronization object
    static ArrayList<String> process = new ArrayList<>(); // List of all processes in the system
    static Registry r; // Registry reference

    static double time_multiplier = 1.0; // time_multiplier times 1 second = 1 step 
    static double timeframe = 0.0; // current time in seconds since starting process

    // Updates the list of all processes in the system
    public static void createList() {
        //california (process 1)
        process.add("129.21.37.23");
        //glados (process 2)
        process.add("129.21.22.196");
        //rhea (process 3)
        process.add("129.21.37.49");
    }

    @Override
    public void sendSnapshot(String state, String Node) throws RemoteException {
        if (Node.equals(process.get(1))) {
            response_from_p2 = state;
            received_response_from_p2 = true;
        }
        if (Node.equals(process.get(2))) {
            response_from_p3 = state;
            received_response_from_p3 = true;
        }
        if (received_response_from_p2 && received_response_from_p3) {
            System.out.println("\n\nGLOBAL STATE after snapshot:\n");
            System.out.println(response_from_p1 + response_from_p2 + response_from_p3);
        }
    }

    // Used by process 1, Initiates the snapshot process in the system
    public static void takeSnapshot() throws RemoteException {
        try {
            System.out.println("\n\n\nsnapshot process initiated");
            snap = true;
            s_bal = bal;
            snap_time = 0;

            // Send out a marker to process p2 and p3
            System.out.println("sending a marker to p2 and p3");
            r = LocateRegistry.getRegistry(process.get(1), port);
            SnapshotIF sif1 = (SnapshotIF) r.lookup("process");
            sif1.send(-1, NodeIP);

            r = LocateRegistry.getRegistry(process.get(2), port);
            SnapshotIF sif2 = (SnapshotIF) r.lookup("process");
            sif2.send(-1, NodeIP);

        } catch (NotBoundException | AccessException ex) {
            System.out.println("Cannot Initiate Snapshot, Other two process are not live.");
        }
    }

    // Construtor
    public SnapshotWithThread() throws RemoteException {
        super();
    }

    public static class TakeSnapshot implements Runnable {

        @Override
        public void run() {

            // loop indefinitely
            while (true) {
                try {

                    System.out.println("\n\n");

                    // If current process is process 1
                    if (NodeIP.equals(process.get(0))) {

                        // If not yet time to initiate snapshot
                        if (step < steps_for_snapshot) {
                            step++;

                            // If snapshot is active    
                        } else if (snap == true) {
                            snap_time++;

                            // If timeout reached
                            if (timeout == snap_time) {
                                // Deactivate snapshot
                                snap = false;

                                // reset buffers and flags 
                                channel_from_2 = 0;
                                channel_from_3 = 0;
                                received_from_p2 = false;
                                received_from_p3 = false;
                                System.out.println("Snapshot Failed, Timeout occured.");
                            }

                            // If snapshot is to be initiated    
                        } else if (step == steps_for_snapshot && snap == false) {
                            System.out.println("Initiating new snapshot");

                            try {
                                takeSnapshot();
                                step = 1;
                                snap_time = 0;
                            } catch (RemoteException ex) {

                            }
                        }
                        // If current process is p2 or p3    
                    } else {
                        // If snapshot is active
                        if (snap == true) {
                            snap_time++;
                            // If timeout is reached
                            if (timeout == snap_time) {
                                // Deactivate snapshot
                                snap = false;

                                // reset buffers and flags
                                channel_from_2 = 0;
                                channel_from_3 = 0;
                                received_from_p1 = false;
                                received_from_p2 = false;
                                received_from_p3 = false;
                                System.out.println("Snapshot Failed, Timeout occured.");
                            }
                        }
                    }
                    // Sleep thread for (1.0*time_multiplier) seconds
                    sleep((int) (1000 * time_multiplier));
                    // Update current time since process started
                    timeframe += time_multiplier;
                    // Uncomment to check time frame progress
                    //System.out.println("\n******\ntimeframe: " + timeframe + " sec\n******");
                } catch (NumberFormatException | InterruptedException ex) {
                    Logger.getLogger(SnapshotWithThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    @Override
    // Remote method used to receive markers and money from other processes asynchronously
    public void send(int amt, String Node) throws RemoteException {

        // If current process is process 1
        if (NodeIP.equals(process.get(0))) {
            // If marker is received
            if (amt == -1) {
                // If snapshot is active
                if (snap == true) {
                    // If marker received from process 2
                    if (Node.equals(process.get(1))) {
                        received_from_p2 = true;
                        System.out.println("Received a marker from p2");
                        // If marker received from process 3
                    } else if (Node.equals(process.get(2))) {
                        received_from_p3 = true;
                        System.out.println("Received a marker from p3");
                    }

                    // If both markers have been received
                    if (received_from_p2 && received_from_p3) {
                        snap = false;
                        System.out.println("Snapshot ended successfully");
                        response_from_p1 = "\nSnapshot at " + NodeName + ":\nbalance: $" + s_bal + "\namount in channel from process 2: $" + channel_from_2 + "\namount in channel from process 3: $" + channel_from_3;
                        System.out.println(received_from_p1);
                        //reset buffers and flags
                        channel_from_2 = 0;
                        channel_from_3 = 0;
                        received_from_p2 = false;
                        received_from_p3 = false;
                    }
                    // If snapshot timed out
                } else {
                    System.out.println("Snapshot already timed out, cannot process marker from " + Node);
                }
                // If money is received
            } else {
                // If nsnapshot is active
                if (snap == true) {
                    // Add to buffer of process 2
                    if (Node.equals(process.get(1))) {
                        channel_from_2 += amt;
                        System.out.println("updated incomming channel from p2");
                        // Add to buffer of process 3    
                    } else if (Node.equals(process.get(2))) {
                        channel_from_3 += amt;
                        System.out.println("updated incomming channel from p3");
                    }
                }

                // Enter synchronized block to update balance
                synchronized (o) {
                    bal += amt;
                }
                System.out.println("\n\n\nReceived a transfer of $" + amt + " from " + Node);
                System.out.println("after receiving the transfer," + NodeName + "'s balance is: $" + bal);
            }

            //If current process is process 2
        } else if (NodeIP.equals(process.get(1))) {
            // If marker is received
            if (amt == -1) {
                // If snapshot is not active, activate snapshot 
                if (snap == false) {
                    snap = true;
                    // If first marker is received from process 1
                    if (Node.equals(process.get(0))) {
                        received_from_p1 = true;
                        channel_from_1 = 0;
                        System.out.println("\n\n\nreceived first marker from p1");
                        System.out.println("starting snapshot process");
                        // If first marker is received from process 3
                    } else if (Node.equals(process.get(2))) {
                        received_from_p3 = true;
                        channel_from_3 = 0;
                        System.out.println("\n\n\nreceived first marker from p3");
                        System.out.println("starting snapshot process");
                    }
                    try {
                        s_bal = bal;
                        snap_time = 0;

                        // Send out a marker to process p1 and p3
                        System.out.println("sending a marker to p1 and p3");
                        r = LocateRegistry.getRegistry(process.get(0), port);
                        SnapshotIF sif0 = (SnapshotIF) r.lookup("process");
                        sif0.send(-1, NodeIP);

                        r = LocateRegistry.getRegistry(process.get(2), port);
                        SnapshotIF sif2 = (SnapshotIF) r.lookup("process");
                        sif2.send(-1, NodeIP);

                    } catch (NotBoundException | AccessException ex) {
                        Logger.getLogger(SnapshotWithThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    // If snapshot is active    
                } else if (snap == true) {

                    // If received second marker from p1
                    if (Node.equals(process.get(0))) {
                        received_from_p1 = true;
                        System.out.println("received second marker from p1");
                        // If received second marker from p3
                    } else if (Node.equals(process.get(2))) {
                        received_from_p3 = true;
                        System.out.println("received second marker from p3");
                    }

                    // If received markers from both processes, p1 and p3
                    if (received_from_p1 && received_from_p3) {
                        try {
                            System.out.println("Snapshot ended successfully");
                            
                            response_from_p2 = "\nSnapshot at " + NodeName + ":\nbalance: $" + s_bal+"\namount in channel from process 1: $" + channel_from_1+"\namount in channel from process 3: $" + channel_from_3;
                            r = LocateRegistry.getRegistry(process.get(0), port);
                            SnapshotIF sif00 = (SnapshotIF) r.lookup("process");
                            sif00.sendSnapshot(response_from_p2, NodeIP);
                            System.out.println(received_from_p2);
                            snap = false;
                            
                            //reset buffers and flags
                            channel_from_1 = 0;
                            channel_from_3 = 0;
                            received_from_p1 = false;
                            received_from_p3 = false;
                        } catch (NotBoundException | AccessException ex) {
                            Logger.getLogger(SnapshotWithThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                // If received money    
            } else {
                // If snapshot is active
                if (snap == true) {
                    // Add to buffer of process 1
                    if (Node.equals(process.get(0))) {
                        channel_from_1 += amt;
                        System.out.println("updated channel from p1");
                        // Add to buffer of process 3
                    } else if (Node.equals(process.get(2))) {
                        channel_from_3 += amt;
                        System.out.println("updated channel from p3");
                    }
                }
                // Enter synchronized block to update balancce
                synchronized (o) {
                    bal += amt;
                }
                System.out.println("\n\n\nReceived a transfer of $" + amt + " from " + Node);
                System.out.println("after receiving the transfer," + NodeName + "'s balance is: $" + bal);
            }

            // If current process is p3
        } else if (NodeIP.equals(process.get(2))) {
            // If marker is received
            if (amt == -1) {
                // If snapshot is not active, activate snapshot
                if (snap == false) {
                    snap = true;
                    // If first marker is received from process 1
                    if (Node.equals(process.get(0))) {
                        received_from_p1 = true;
                        System.out.println("\n\n\nreceived first marker from p1");
                        System.out.println("starting snapshot process");
                        // If first marker is received from process 2
                    } else if (Node.equals(process.get(1))) {
                        received_from_p2 = true;
                        System.out.println("\n\n\nreceived first marker from p2");
                        System.out.println("starting snapshot process");
                    }
                    try {
                        s_bal = bal;
                        snap_time = 0;

                        // Send out a marker to process p1 nad p2 
                        System.out.println("sending a marker to p1 and p2");
                        r = LocateRegistry.getRegistry(process.get(0), port);
                        SnapshotIF sif0 = (SnapshotIF) r.lookup("process");
                        sif0.send(-1, NodeIP);

                        r = LocateRegistry.getRegistry(process.get(1), port);
                        SnapshotIF sif1 = (SnapshotIF) r.lookup("process");
                        sif1.send(-1, NodeIP);
                    } catch (NotBoundException | AccessException ex) {
                        Logger.getLogger(SnapshotWithThread.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    // If snapshot is active
                } else if (snap == true) {

                    // If received second marker from p1
                    if (Node.equals(process.get(0))) {
                        received_from_p1 = true;
                        System.out.println("received second marker from p1");
                        // If received second marker from p2
                    } else if (Node.equals(process.get(1))) {
                        received_from_p2 = true;
                        System.out.println("received second marker from p2");
                    }

                    // If received markers from both processes, p1 and p2
                    if (received_from_p1 && received_from_p2) {
                        try {
                            System.out.println("Snapshot ended successfully");
                            
                            response_from_p3 = "\nSnapshot at " + NodeName + ":\nbalance: $" + s_bal+"\namount in channel from process 1: $" + channel_from_1+"\namount in channel from process 2: $" + channel_from_2;
                            r = LocateRegistry.getRegistry(process.get(0), port);
                            SnapshotIF sif00 = (SnapshotIF) r.lookup("process");
                            sif00.sendSnapshot(response_from_p3, NodeIP);
                            System.out.println(received_from_p3);
                            snap = false;
                            
                            // reset buffers and flags
                            channel_from_1 = 0;
                            channel_from_2 = 0;
                            received_from_p1 = false;
                            received_from_p2 = false;
                        } catch (NotBoundException | AccessException ex) {
                            Logger.getLogger(SnapshotWithThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                // If money is received    
            } else {
                // If snapshot is active
                if (snap == true) {
                    // Add to buffer of process 1
                    if (Node.equals(process.get(0))) {
                        channel_from_1 += amt;
                        System.out.println("updated channel from p1");
                        // Add to buffer of process 2
                    } else if (Node.equals(process.get(1))) {
                        channel_from_2 += amt;
                        System.out.println("updated channel from p2");
                    }
                }

                // Enter synchronized block to update balance
                synchronized (o) {
                    bal += amt;
                }
                System.out.println("\n\n\nReceived a transfer of $" + amt + " from " + Node);
                System.out.println("after receiving the transfer," + NodeName + "'s balance is: $" + bal);
            }
        }

    }

    /**
     * Main method initializes list of processes, gives identity to the current
     * process, starts the registry for remote method invocation and starts a
     * thread to perform fund transfers and take system snapshots.
     *
     * @param args the command line arguments
     * @throws java.rmi.RemoteException
     */
    public static void main(String[] args) throws RemoteException {
        try {
            // Initialize list of processes
            createList();

            // ID the process
            InetAddress IP = InetAddress.getLocalHost();
            NodeName = IP.getHostName();
            NodeIP = IP.getHostAddress();

            int i;
            if (process.get(0).equals(NodeIP)) {
                i = 1;
            } else if (process.get(1).equals(NodeIP)) {
                i = 2;
            } else {
                i = 3;
            }
            System.out.println("Process " + i);

            // Start registry 
            r = LocateRegistry.createRegistry(port);
            r.rebind("process", new SnapshotWithThread());

            // Start thread
            Thread t1 = new Thread(new SnapshotWithThread());
            t1.start();
            Thread t2 = new Thread(new TakeSnapshot());
            t2.start();
        } catch (UnknownHostException ex) {
            Logger.getLogger(SnapshotWithThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {

        //System.out.println("timeframe :" + timeframe + " sec");
        // Initializing variables
        int amt, process_id, temp_process_id;
        String temp_process_ip = "";

        // loop indefinitely
        while (true) {
            try {

                System.out.println("\n\n");
                // Get random amount between 0 and 100 to transfer to other processes
                amt = (int) (Math.random() * 100);

                // Get process ID
                if (process.get(0).equals(NodeIP)) {
                    process_id = 0;
                } else if (process.get(1).equals(NodeIP)) {
                    process_id = 1;
                } else {
                    process_id = 2;
                }

                // Get random transfer recepient other than self
                do {
                    temp_process_id = (int) (Math.random() * 100);
                    if (temp_process_id >= 0 && temp_process_id <= 33) {
                        temp_process_id = 0;
                        temp_process_ip = process.get(temp_process_id);
                    } else if (temp_process_id >= 34 && temp_process_id <= 66) {
                        temp_process_id = 1;
                        temp_process_ip = process.get(temp_process_id);
                    } else if (temp_process_id >= 67 && temp_process_id <= 100) {
                        temp_process_id = 2;
                        temp_process_ip = process.get(temp_process_id);
                    }
                } while (temp_process_id == process_id);

                try {
                    // If transfer is possible (sufficient funds available)
                    if ((bal - amt) >= 0) {
                        // Get remote object
                        r = LocateRegistry.getRegistry(temp_process_ip, port);
                        SnapshotIF sif = (SnapshotIF) r.lookup("process");
                        // remote method invocation to transfer money
                        sif.send(amt, NodeIP);

                        // Enter synchronized block to update balance
                        synchronized (o) {
                            bal -= amt;
                        }
                        System.out.println("transferred $" + amt + " from " + NodeName + " to " + temp_process_ip);

                        // If transfer is not possible (sufficient funds not available)    
                    } else {
                        System.out.println("Cannot complete transaction, not enough funds.");
                    }
                } catch (RemoteException | NotBoundException e) {
                    System.out.println("cannot make transfer between " + NodeName + " and " + temp_process_ip);
                }

                System.out.println("after transfer, " + NodeName + "'s balance is: $" + bal);

                // Sleep thread for (1.0*time_multiplier) seconds
                sleep((int) (1000 * time_multiplier));
                // Update current time since process started
                timeframe += time_multiplier;
                // Uncomment to check time frame progress
                //System.out.println("\n******\ntimeframe: " + timeframe + " sec\n******");
            } catch (NumberFormatException | InterruptedException ex) {
                Logger.getLogger(SnapshotWithThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
