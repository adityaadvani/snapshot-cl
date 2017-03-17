/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package processes;

import java.io.Serializable;
import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
public class Processes extends UnicastRemoteObject implements processIF, Serializable, Runnable {

    static String NodeName = "";
    static String NodeIP = "";
    static int port = 7394;
    static final Object o = new Object();
    static int bal = 1000;
    static String clock = "0 0 0";
    static ArrayList<String> process = new ArrayList<>();

    public static void createList() {
        process.add("129.21.37.23");
        process.add("129.21.22.196");
        process.add("129.21.37.49");
    }

    public Processes() throws RemoteException {
        super();
    }

    //make remote
    public String getClock() throws RemoteException {
        return clock;
    }

    //make remote
    @Override
    public void sendAmount(int amt, String senderClock, String Node) throws RemoteException {
        int process_id, clk_temp;
        String MyClock[], SenderClock[];

        synchronized (o) {
            
            //balance += transferred amount
            bal += amt;

            SenderClock = senderClock.split(" ");
            MyClock = clock.split(" ");

            for (int k = 0; k < 3; k++) {
                if (Integer.parseInt(MyClock[k]) < Integer.parseInt(SenderClock[k])) {
                    MyClock[k] = SenderClock[k];
                }
            }
            clock = MyClock[0] + " " + MyClock[1] + " " + MyClock[2];

            //clock++;
            if (process.get(0).equals(NodeIP)) {
                process_id = 0;
                MyClock = clock.split(" ");
                clk_temp = Integer.parseInt(MyClock[process_id]);
                clk_temp++;
                clock = clk_temp + " " + MyClock[1] + " " + MyClock[2];
            } else if (process.get(1).equals(NodeIP)) {
                process_id = 1;
                MyClock = clock.split(" ");
                clk_temp = Integer.parseInt(MyClock[process_id]);
                clk_temp++;
                clock = MyClock[0] + " " + clk_temp + " " + MyClock[2];
            } else {
                process_id = 2;
                MyClock = clock.split(" ");
                clk_temp = Integer.parseInt(MyClock[process_id]);
                clk_temp++;
                clock = MyClock[0] + " " + MyClock[1] + " " + clk_temp;
            }
            
            System.out.println("\n\n\nReceived a transfer of $" + amt + " from " + Node);
            System.out.println("after receiving the transfer," + NodeName + "'s balance is: $" + bal);
            System.out.print("Current vector clock at " + NodeName + " is: " + clock);
        }
    }

    public int getEvent() {
        int event_id = -1;

        int i = (int) (Math.random() * 100);
        if (i >= 0 && i <= 33) {
            event_id = 1;
        } else if (i >= 34 && i <= 66) {
            event_id = 2;
        } else if (i >= 67 && i <= 100) {
            event_id = 3;
        }
        return event_id;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RemoteException {
        try {
            // TODO code application logic here
            createList();
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
            System.out.println("Process " + i + ", current vector clock is: " + clock);

            Registry r = LocateRegistry.createRegistry(port);
            r.rebind("process", new Processes());

            Thread t = new Thread(new Processes());
            t.start();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Processes.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        int event, amt, process_id, temp_process_id, clk_temp, receiverBal;
        String op = "", temp_process_ip = "", MyClock[];
        Registry r;
        while (true) {
            try {
                synchronized (o) {
                    //updating clock
                    if (process.get(0).equals(NodeIP)) {
                        process_id = 0;
                        MyClock = clock.split(" ");
                        clk_temp = Integer.parseInt(MyClock[process_id]);
                        clk_temp++;
                        clock = clk_temp + " " + MyClock[1] + " " + MyClock[2];
                    } else if (process.get(1).equals(NodeIP)) {
                        process_id = 1;
                        MyClock = clock.split(" ");
                        clk_temp = Integer.parseInt(MyClock[process_id]);
                        clk_temp++;
                        clock = MyClock[0] + " " + clk_temp + " " + MyClock[2];
                    } else {
                        process_id = 2;
                        MyClock = clock.split(" ");
                        clk_temp = Integer.parseInt(MyClock[process_id]);
                        clk_temp++;
                        clock = MyClock[0] + " " + MyClock[1] + " " + clk_temp;
                    }

                    System.out.println("\n\n");

                    //get random event
                    event = getEvent();

                    //if event = withdraw
                    if (event == 1) {
                        op = "withraw";
                        //get random amount to withdraw between 0 and 100
                        amt = (int) (Math.random() * 100);
                        if ((bal - amt) >= 0) {
                            bal -= amt;
                            System.out.println("withdrew $" + amt + " from " + NodeName);
                        } else {
                            System.out.println("Cannot complete transaction, not enough funds.");
                        }

                    }

                    //if event = deposit
                    if (event == 2) {
                        op = "deposit";
                        //get random amount to deposit between 0 and 100
                        amt = (int) (Math.random() * 100);
                        bal += amt;
                        System.out.println("deposited $" + amt + " to " + NodeName);
                    }

                    //if event = transfer
                    if (event == 3) {
                        op = "transfer";
                        //get random amount to transfer between 0 and 100
                        amt = (int) (Math.random() * 100);

                        //get random transfer recepient other than self
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
                            if((bal - amt)>=0){
                            r = LocateRegistry.getRegistry(temp_process_ip, port);
                            processIF pif = (processIF) r.lookup("process");
                            pif.sendAmount(amt, clock, NodeIP);
                            bal -= amt;
                            System.out.println("transferred $" + amt + " from " + NodeName + " to " + temp_process_ip);
                            } else {
                                System.out.println("Cannot complete transaction, not enough funds.");
                            }
                        } catch (RemoteException | NotBoundException e) {
                            System.out.println("cannot make transfer between " + NodeName + " and " + temp_process_ip);
                        }

                    }
                    System.out.println("after " + op + " " + NodeName + "'s balance is: $" + bal);
                    System.out.print("Current vector clock at " + NodeName + " is: " + clock);

                }
                sleep(5000);
            } catch (NumberFormatException | InterruptedException ex) {
                Logger.getLogger(Processes.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
