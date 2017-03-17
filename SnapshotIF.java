/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package snapshotwiththread;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Aditya Advani
 */
public interface SnapshotIF extends Remote {
    public void send(int amt, String Node) throws RemoteException;
    public void sendSnapshot(String state, String Node) throws RemoteException;
}
