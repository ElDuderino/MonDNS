package com.pyropath.mondns;

import java.lang.*;
import java.net.*;
import java.io.*;

public class SvcListener extends Thread{
    
    Thread me = null;
    private static final int SLEEP_INTERVAL = 100;
    
    private static final int MAX_CONNECTIONS = 5;
    private int num_connections;
    
    private ServerSocket server;
    private Socket clientConn;
    private Service svc;
    private MonDNS mondns;
    
    private String message;
    
    /** Creates a new instance of SvcListener */
    public SvcListener(MonDNS mondns, Service svc) {
        super();
        this.mondns = mondns;
        this.svc = svc;
        this.message = " ";
    }
    
    public String getMessage(){
        return this.message;
    }
    
    public synchronized void setMessage(String message){
        this.message = message;        
        this.notifyAll();
    }
    
    public synchronized void setNumConnections(int num_connections){
        this.num_connections = num_connections;
    }
    
    public synchronized int getNumConnections(){
        return this.num_connections;
    }
    
    public void run(){
        
        Thread me = this.currentThread();
        
        SvcClient clients[] = new SvcClient[MAX_CONNECTIONS];
        boolean gotHandler = false; 
        
        //create a pool of service threads
        for(int i = 0; i < MAX_CONNECTIONS; i++){
            clients[i] = new SvcClient(this, null);
            clients[i].start();
        }
                
        //try and bind the socket
        try{
            server = new ServerSocket(svc.getPort());
            
        }catch(IOException e){
            
            System.out.println("Could not bind to:" + svc.getPort() + 
                               "Reason:" + e.getMessage());
            return;
        }
        do{
            try{
                Socket s = server.accept();
                
                //check if any threads are available
                for(int i = 0; i < MAX_CONNECTIONS; i++){
                    
                    gotHandler = false;
                    
                    if(clients[i].getSocket() == null){
                        
                        synchronized(clients[i]){
                            clients[i].setSocket(s);
                            clients[i].notify();
                            
                        }
                        gotHandler = true;
                        break;
                    }
                }
                if(gotHandler == false){
                    try{s.close();}catch(IOException e){}
                }
            }catch(IOException e){
                //waiting
            }
            
            try{
                sleep(SLEEP_INTERVAL);
            }catch(InterruptedException e){
                System.out.println(e.getMessage());
            }
        }while(me != null);
    }
    
}
