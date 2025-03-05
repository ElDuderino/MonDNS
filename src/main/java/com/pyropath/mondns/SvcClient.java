package com.pyropath.mondns;

import java.net.*;
import java.io.*;

public class SvcClient extends Thread{
    
    SvcListener svcL = null;
    Socket cSocket = null;
    
    public static final int SOCK_TIMEOUT = 5000;
    public static final int BUF_SZ = 2048;
    public static final int INTERVAL = 1000; //send new data every second
    
    private byte buf[];
    
    /** Creates a new instance of SvcClient */
    public SvcClient(SvcListener svcL, Socket cSocket){
        super();
        this.svcL = svcL;
        this.cSocket = cSocket;
    }
    
    public synchronized void setSocket(Socket socket){
        this.cSocket = socket;
    }
    
    public synchronized Socket getSocket(){
        return this.cSocket;
    }
    
    public void run(){
        
        buf = new byte[BUF_SZ];
        
        while(true) {
            if (cSocket == null) {
                /* nothing to do */
                synchronized(this){
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        /* should not happen */
                        continue;
                    }
                }
            }
            
            try {
                //process the client request
                System.out.println("Got a connection");
                serviceRequest();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            /* go back in wait queue if there's fewer
             * than numHandler connections.
             */
            cSocket = null;
        }
        
    }
    
    public void serviceRequest() throws IOException {
        
        Socket s = this.cSocket;
        
        InputStream in = new BufferedInputStream(s.getInputStream());
        PrintStream out = new PrintStream(s.getOutputStream());
        
        s.setSoTimeout(SOCK_TIMEOUT);
        s.setTcpNoDelay(true);
        
        for(int i = 0; i < BUF_SZ; i++){
            buf[i] = 0;
        }
        
        
        try{

            int numread = 0;
            int res = 0;

    dance:
            while(numread < BUF_SZ){
                
                res = in.read(buf, numread, BUF_SZ - numread);
                
                if(res == -1){
                    /* EOF */
                    return;
                }
                
                int i = numread;
                numread += res;
                
                for(; i<numread; i++){
                    if((buf[i] == '\r') || (buf[i] == '\n')){
                        break dance;
                    }
                }
                
            }
            //decode the buffer
            if((buf[0] == 'G') && (buf[1] == 'E') && (buf[2] == 'T')){
                    
                do{
                 
                    synchronized(svcL){
                        
                        try{
                            svcL.wait();                    
                        }catch(InterruptedException e){
                        }
                        
                        String message = svcL.getMessage();

                        byte outbuf[] = new byte[message.length()];
                        message = message + "\r\n";
                        outbuf = message.getBytes();
                        out.write(outbuf);
                    }
                    //try{this.sleep(INTERVAL);}catch(InterruptedException e){}

                }while(s.isConnected());
                
                s.close();
                
            }else{
                out.print("unsupported method\r\n");
                out.flush();
                
                s.close();
            }
            
        
        }finally{
            s.close();
        }
    }
    
}

