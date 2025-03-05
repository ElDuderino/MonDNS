package com.pyropath.mondns;

import java.io.*;

public class Service {
    
    private String servicename;     //the service name
    private int port;               //the service port
    private int protocol;           //the service protocol
    private String alias;           //the service alias
    private String comment;         //the service comment
        
    /** 
     * NOTE:
     * Think about throwing an UnsupportedProtocolException here 
     */
    public Service(String servicename, 
                    int port, 
                    String protocol, 
                    String alias, 
                    String comment){
        
        this.servicename = servicename;
        this.port = port;
        char _protocol[] = protocol.toCharArray();
        this.protocol = Protocols.getProtocolByDesc(_protocol);
        this.alias = alias;
        this.comment = comment;
        
    }
    
    public Service(){
        
        this.servicename = "";
        this.port = -1;
        this.protocol = Protocols.getProtocolByDesc("".toCharArray());
        this.alias = "";
        this.comment = "";
    
    }
    
    /** 
     * NOTE:
     * Think about throwing an UnsupportedProtocolException here 
     */
    public void setProtocol(String protocol){
        char _protocol[] = protocol.toCharArray();
        this.protocol = Protocols.getProtocolByDesc(_protocol);
    }
    
    /** 
     * NOTE:
     * Think about throwing an UnsupportedProtocolException here 
     */
    public void setProtocol(int protocol){
        this.protocol = protocol;
    }
    
    public int getProtocol(){
        return this.protocol;
    }
    
    public void setPort(int port){
        this.port = port;
    }
    
    public int getPort(){
        return this.port;
    }
    
    public void setAlias(String alias){
        this.alias = alias;
    }
    
    public String getAlias(){
        return this.alias;
    }
    
    public void setServiceName(String servicename){
        this.servicename = servicename;
    }
    
    public String getServiceName(){
        return this.servicename;
    }
    
    public void setComment(String comment){
        this.comment = comment;
    }
    
    public String getComment(){
        return this.comment;
    }
    
    /** 
     * populate a Service object from config/services 
     * based on the service name and protocol
     */
    public static boolean getServiceByNameProto(Service _Service, 
                                                String servicename, 
                                                String protocol){
                                                    
        //enforce the integrity of the service object
        _Service.setServiceName(servicename);
        _Service.setProtocol(protocol);
        
        try{                                            
            //setup a service class based on the servicename and protocol
            BufferedReader reader = new BufferedReader(new FileReader(MonDNS.SERVICES_CONFIG));
            String line;
            boolean done = false;
            while(((line = reader.readLine()) != null) && !done){
                
                if(line.length() <= 0) continue;
                
                char buf[] = line.toCharArray();
                
                //comment or empty line??
                if(buf.length <= 0) continue;
                if(buf[0] == '#') continue;
                
                line        = line.toLowerCase();
                servicename = servicename.toLowerCase();
                protocol    = protocol.toLowerCase();
                
                if((line.indexOf(servicename) == -1) || (line.indexOf(protocol) == -1)){
                    continue;
                }else{             
                    
                    //we found the service - parse it
                    //see if there is a description
                    int s = line.length();
                    
                    if((s = line.indexOf('#')) != -1){
                        _Service.setComment(line.substring(s));
                    }else{
                        _Service.setComment("");
                        s = line.length();
                    }
                    
                    
                    //find the port number boundaries in the string
                    //s will either be the end of the string or the 
                    //start of the comment #, we don't need anything 
                    //after the comment
                    char rest[] = line.substring(0, s).toCharArray();
                    
                    //check and see if we have the exact service name in the line 
                    //if not, continue...
                    int i = 0;
                    char svcbuf[] = new char[s];
                    do{
                        svcbuf[i] = rest[i];
                        i++;
                    }while((rest[i] != '\t') && (rest[i] != ' '));
                    //swap them
                    //swap everything to the proper size
                    char tmp[] = new char[i];
                    for(int z = 0; z < i; z++){
                        tmp[z] = svcbuf[z];
                    }
                    svcbuf = new char[i];
                    for(int z = 0; z < i; z++){
                        svcbuf[z] = tmp[z];
                    }
                    /* may be a different service containing similar characters
                        e.g. doing a simple stristr would find all of these for tcp/ftp
                        ftp-data	20/tcp 
                        ftp-data	20/udp
                        # 21 is registered to ftp, but also used by fsp
                        ftp		21/tcp
                        ftp		21/udp
                     *
                     **/	
                    if(new String(svcbuf).compareToIgnoreCase(servicename) != 0){
                        continue;
                    }
                    
                    char nbuf[] = new char[5]; //max number of chars 65535
                    
                    boolean foundspc = false;
                    int j = 0;
                    i = 0;
                    //start get port
                    for( i = 0; i < rest.length; i++){
                        
                        if(!foundspc){
                            if((rest[i] == ' ') || (rest[i] == '\t')){
                                foundspc = true;
                            }
                        }
                        
                        if(foundspc){
                            
                            if(isNumericChar(rest[i])){
                                nbuf[j] = rest[i];
                                if(j < buf.length){
                                    j++;
                                }
                            }
                            if(rest[i] == '/') break;
                        }
                         
                    }
                    
                    //swap everything to the proper size
                    tmp = new char[j];
                    for(int z = 0; z < j; z++){
                        tmp[z] = nbuf[z];
                    }
                    nbuf = new char[j];
                    for(int z = 0; z < j; z++){
                        nbuf[z] = tmp[z];
                    }
                    //end get port CRITICAL!!
                    try{
                        String num = new String(nbuf);
                        int port = Integer.parseInt(num.toString());
                        _Service.setPort(port);
                        
                    }catch(NumberFormatException nfe){
                        nfe.printStackTrace();
                        return false;
                    }
                    
                    //get an alias if it exists //we trimmed out the comment, 
                    //so loop to the end of the string after the first space or tab
                    //or until the next space or tab
                    foundspc = false;
                    char aliasbuf[] = new char[rest.length - i]; //maximum alias size
                    j = 0;
                    for(; i < rest.length; i++){
                        if(!foundspc){
                            if((rest[i] == ' ') || (rest[i] == '\t')){
                                foundspc = true;
                            }
                        }
                        
                        if(foundspc){
                            if((rest[i] != '\t') && (rest[i] != ' ')){
                                break;
                            }else{
                                aliasbuf[j] = rest[i];
                                j++;
                            }
                        }
                    }
                    
                    if(j >= 0){
                        _Service.setAlias(new String(aliasbuf));
                    }else{
                        _Service.setAlias("");
                    }
                    
                    done = true;    
                }
            }
            
            reader.close();
            return done;
            
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean isNumericChar(char c){
        
        if((c >= 0x30) && (c <= 0x39)){
            return true;
        }
        return false;   
    }
    
    public String toString(){
        
        return "Service Name:" + this.servicename + " Alias:" + this.alias + "\r\n" +
                "Protocol:" + Protocols.protocolToString(this.protocol) + " Port:" + this.port + "\r\n" +
                "Comments:" + this.comment + "\r\n";
    
    }
}
