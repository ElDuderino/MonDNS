package com.pyropath.mondns;

import java.util.Vector;
import java.io.*;
import java.net.*;
import java.lang.Runtime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MonDNS {
    
    private static Properties config = new Properties();
    
    //private static final String TINY_DNS_CONFIG = "/var/dns/namedb/root/data"; //tiny dns config file
    private static String TINY_DNS_CONFIG;
    //private static final String TINY_DNS_BASE_DIR = "/var/dns/namedb/root";
    private static String TINY_DNS_BASE_DIR;
    
    private static String TINY_DNS_EXEC_CMD = "make";
    private static String[] TINY_DNS_ENV_VARS = null;
    
    public static String SERVICES_CONFIG = "/etc/services";
    private static boolean DEBUG = true;
    
    private static int INTERVAL = 10000; //loop interval in ms
    
    private String status;
    private String message;
    
    private SvcListener listener;
    
    private volatile boolean running = true;
    
    private void loadConfig() {
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);

            TINY_DNS_CONFIG = config.getProperty("tiny_dns_config", "/etc/dns/config");
            TINY_DNS_BASE_DIR = config.getProperty("tiny_dns_base_dir", "/etc/dns");
            TINY_DNS_EXEC_CMD = config.getProperty("tiny_dns_exec_cmd", "make");
            SERVICES_CONFIG = config.getProperty("services_config", "/etc/services");
            INTERVAL = Integer.parseInt(config.getProperty("interval", "10000"));
            DEBUG = Boolean.parseBoolean(config.getProperty("debug", "true"));

        } catch (IOException e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
        }
    }
    
    /** Creates a new instance of MonDNS */
    public MonDNS() {
        loadConfig();
        message = "";
    }
    
    public synchronized String getStatus(){
        return this.status;
    }
    
    public synchronized void setStatus(String status){
        this.status = status;
    }
    
    private static boolean parseServerLine(char buf[], MonDNS.hHost host){
        
        boolean st = false; //have we started parsing yet?
        char addrTmp[] = new char[buf.length];
        
        host.setStatus(buf[0]);
        int z = 0;
        for(int i = 1; i < buf.length; i++){
            
            if(st){
                //check for the next semicolon
                if(buf[i] == ':'){
                    break;
                }
                //else copy it over 
                addrTmp[z] = buf[i];
                z++;
            }
            
            if(buf[i] == ':'){ st = true; }  
        }
        
        //copy and resize the addrTmp
        char tmpBuf[] = new char[z];
        
        for(int i = 0; i < z; i++){
            tmpBuf[i] = addrTmp[i];
        }
        
        host.setAddr(new String(tmpBuf));
        if(DEBUG){
            System.out.println(tmpBuf);
        }
        return st;
    }
    
    private static boolean loadServers(List<hHost> servers){
        
        BufferedReader in;
        String sbuf;
        char buf[];
        hHost tmpHost;
        
        try{
            
            in = new BufferedReader(new FileReader(TINY_DNS_CONFIG));
            
            while((sbuf = in.readLine()) != null){
                
                buf = sbuf.toCharArray();
                if((buf[0] == '+') || (buf[0] == '-')){
                    //it is a server, parse it
                    tmpHost = new hHost();
                    //only add it if we successfully parsed it
                    if(parseServerLine(buf, tmpHost)){
                        servers.add(tmpHost);
                    }
                }
            }
            
            //did we get any servers?
            if(servers.size() != 0){
                return true;
            }else{
                return false;
            }
            
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        
    }
    
    private boolean testConnect(Service svc, hHost host){
        
        InetSocketAddress isad = new InetSocketAddress(host.getAddr(), svc.getPort());
        Socket socket = new Socket();
        
        try{
            
            socket.connect(isad, 100);
            socket.close();

            String msg = "Connected to:" + host.getAddr() + 
                          " with status:" + host.getStatus()  + 
                          " on port:" + svc.getPort() + 
                          "at:" + System.currentTimeMillis() + "\r\n";
           
            this.message = this.message + msg;
                
            if(DEBUG){
                System.out.print(msg);              
            }
            return true;
            
        }catch(IOException e){
            //System.out.println("Host:" + host.getAddr() + ":" + svc.getPort() + e.getMessage());
            return false;
        }
        
        
    }
    
    private boolean testStatus(hHost host){
    
        //return true if the server is good
        //false if the status is bad
        
        
        //try and connect to the server using one of the services in /etc/services
        try{                                            
            //setup a service class based on the servicename and protocol
            BufferedReader reader = new BufferedReader(new FileReader(SERVICES_CONFIG));
            String line;
            boolean done = false;
            String strSvc = "";
            
            while(((line = reader.readLine()) != null) && !done){
                
                strSvc = "";
                
                if(line.length() <= 0) continue;
                
                char buf[] = line.toCharArray();
                
                //comment or empty line??
                if(buf.length <= 0) continue;
                if(buf[0] == '#') continue;
                
                //else it is probably a service, get the name and then get the service info by name
                    if ((strSvc = Service.getServiceNameFromLine(buf)) != null) {
                        // Populate service info based on name and TCP protocol
                        Service svc = new Service();
                        if (Service.getServiceByNameProto(svc, strSvc, "TCP")) {
                            int port = svc.getPort();
                            // Only test well-known ports (<1024)
                            if (port > 0 && port < 1024) {
                                if (testConnect(svc, host)) {
                                    reader.close();
                                    return true;
                                }
                            } else if (DEBUG) {
                                System.out.println("Skipping service '" + strSvc + "' on port " + port);
                            }
                        } else {
                            if (DEBUG) {
                                System.out.println("Could not get Service by name: " + strSvc);
                            }
                        }
                }else{
                    if(DEBUG){
                        System.out.println("Could not get Service Name from line:" + (new String(buf)));
                    }
                }
            }//if we did not return yet we could not connect to ANY service on that machine
            
            reader.close();
            return false;
            
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        
    }
    
    public static boolean rewriteConfig(List<hHost> servers){
    
        BufferedReader in;
        BufferedWriter out;
        
        String line;
        String lineOut;
        
        char buf[];
        char status;
        String addr;
        
        try{
            
            in = new BufferedReader(new FileReader(TINY_DNS_CONFIG));
            out= new BufferedWriter(new FileWriter(TINY_DNS_CONFIG + ".tmp"));
            
            while((line = in.readLine()) != null){
                
                buf = line.toCharArray();
                
                if((buf[0] == '-') || (buf[0] == '+')){
                    //check and see if it matches one of our servers and if it does, see if
                    //the status has changed
                    for(hHost server : servers){
                        
                        addr    = server.getAddr();
                        status  = server.getStatus();
                        
                        if(line.indexOf(addr) != -1){
                            /*
                                this is the server, check the existing status
                                ok, we are running this because status HAS changed
                                so just set the status to the one in the Vector hHost object
                             */
                            buf[0] = status;
                        }
                    }
                    
                    
                }

                /* always copy the line .. we aren't doing anything in the loop
                 * other than changing the status
                 */
                lineOut = new String(buf);
                lineOut = lineOut + "\r\n";
                out.write(lineOut);
            }
            
            in.close();
            
            out.flush();
            out.close();
            
            boolean ret = true;
            
            if(!(new File(TINY_DNS_CONFIG).delete())){
                System.out.println("ERROR DELETING DNS CONFIG!!");
                ret = false;
            }
            
            if(!(
            new File(TINY_DNS_CONFIG + ".tmp").renameTo(new File(TINY_DNS_CONFIG))
            )){
                System.out.println("ERROR RENAMING DNS TMP CONFIG!!");
                ret = false;
            }
            
            return ret;
            
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        
    }
    
    private static boolean remakeCDB(){
        
        String cmds[] = new String[3];
        
        cmds[0] = "/bin/csh";
        cmds[1] = "-c";
        cmds[2] = TINY_DNS_EXEC_CMD;
        
        File locale = new File(TINY_DNS_BASE_DIR);
        
        try{
            // execute the command
            Process proc = Runtime.getRuntime().exec(cmds, TINY_DNS_ENV_VARS, locale);

            // wait until it's done executing
            proc.waitFor();

            // what did the process output from the Input pipe back to
            // this process (okay, who named this stuff)?
            InputStream out = proc.getInputStream();

            // output it (really slowly)
            int i;

            while ((i = out.read()) != -1) System.out.print((char) i);
            System.out.print("\r\n");
            return true;

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        
    }
    
    private void doIt(){
        
        ArrayList<hHost> servers = new ArrayList<>();
        
        boolean done = false;
        boolean changed = false;
        boolean testresult = false;
        
        Service svc = new Service();
        
        svc.setAlias("MonDNS Reports");
        svc.setComment("Service to output realtime MonDNS status reports");
        svc.setPort(12222);
        svc.setProtocol("TCP");
        svc.setServiceName("mondns-status");
        
        listener = new SvcListener(this, svc);
        listener.start();
        
        do{
            this.message = "";
            
            synchronized(listener){
                
                servers.clear();

                /**

                - load the servers from the file
                - loop through and check their status
                - if it has changed, we need to 
                    change their status in the config file, 
                - do this by creating a temp file, 
                    and rewriting the old config into it
                    and replacing the +/- with the server IP
                    that matches the line in the global vector list
                - re-make the dns config

                */
                changed = false;

                if(loadServers(servers)){

                    for(hHost testHost : servers){

                        testresult = testStatus(testHost);

                        char res = (testresult == true) ? '+' : '-';

                        if(res != testHost.getStatus()){
                            changed = true;
                            if(DEBUG){
                                System.out.println(
                                    "Server:" + testHost.getAddr() + 
                                    "has changed from:" + testHost.getStatus() + 
                                    " to:" + res);
                            }

                        }

                        if(changed){
                            //rewrite the data file
                            testHost.setStatus(res);
                            rewriteConfig(servers);
                            remakeCDB();
                        }

                    }

                }else{
                    System.out.println("Could not load servers!");
                }
                listener.setMessage(message);
            }
            
            //take a nap
            try{Thread.sleep(INTERVAL);}catch(InterruptedException ie){}
            
        }while(!done);
        
    }
    
    public static void main(String args[]){
        
        MonDNS mondns = new MonDNS();
        mondns.doIt();
        
    }
    
    static class hHost {
        
        private String addr;
        private char status;
        
        public hHost(String server, char s){
            this.addr = addr;
            this.status = s;
        }
        
        public hHost(){
            this.addr = "";
            this.status = '\0';
        }
        
        public void setStatus(char s){
            this.status = s;
        }
        
        public void setAddr(String addr){
            this.addr = addr;
        }
        
        public char getStatus(){
            return this.status;
        }
        
        public String getAddr(){
            return this.addr;
        }
        
        public String toString(){
            
            String s;
            
            switch(status){
                
                case '+':
                    s = "up";
                    break;
                    
                case '-':
                    s = "down";
                    break;
                    
                default:
                    s = "unsupported";
                    break;
            }
                     
            return("Address:" + this.addr + " Status is:" + s);
        }
    }
    
}
