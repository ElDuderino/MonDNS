package com.pyropath.mondns;

public class Protocols {
    
    public final static int UNSUPPORTED_PROTOCOL = 0x00;
    public final static int TCP = 0x01;
    public final static int UDP = 0x02;
    
    
    public static int getProtocolByDesc(char desc[]){
    
        if(Util.areEqualIgnoreCase(desc, "TCP".toCharArray())){
            return TCP;
        }else if(Util.areEqualIgnoreCase(desc, "UDP".toCharArray())){
            return UDP;
        }else{
            return UNSUPPORTED_PROTOCOL;
        }
        
    }
    
    public static String protocolToString(int protocol){
        
        switch(protocol){
            case TCP:
                return "tcp";
                
            case UDP:
                return "udp";
                
            default:
                return "unsupported protocol";
                
        }
    }
    /** Creates a new instance of Protocols */
    public Protocols() {
    }
    
}
