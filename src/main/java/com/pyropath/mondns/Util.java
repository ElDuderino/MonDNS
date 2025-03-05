package com.pyropath.mondns;

public class Util {
    
    
    public static char toLower(char c){
    
        if((c <= 0x5A) && (c >= 0x41)){
            //convert it to lower
            return(char)(c + 0x20);
        }
        //otherwise just return it
        return c;
        
    }
    
    public static boolean areEqualIgnoreCase(char arr1[], char arr2[]){
    
        int max = arr1.length;
       
        if(arr1.length != arr2.length){ return false; }
        
        for(int i = 0; i < max; i++){
            
            if(toLower(arr1[i]) != toLower(arr2[i])){ return false; }
        }
        
        return true;
    }
    
}
