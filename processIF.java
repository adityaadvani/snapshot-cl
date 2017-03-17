/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package processes;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Aditya Advani
 */
public interface processIF extends Remote {
    
    public void sendAmount(int amt, String SenderClock, String NodeIP) throws RemoteException;
    
}
