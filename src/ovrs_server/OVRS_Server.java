/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ovrs_server;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.drafts.Draft_6455;

/**
 *
 * @author mreddy3
 */
public class OVRS_Server {
    
    JmDNS jMDns = null;
    
    CmdServerCallBacks mCallBacksHandler = new CmdServerCallBacks() {
        @Override
        public void onCmdServerStarted(int port) {
            try{
                jMDns = JmDNS.create();
                ServiceInfo info = ServiceInfo.create( "_http._tcp.", 
                        "OVRS_SERVER", port, "Video Service");
                jMDns.registerService(info);
                
                
                
            }
            catch(Exception ee){
                System.out.println(ee.getMessage());
                close();
            }
            
        }

        @Override
        public void onCmdServerStopped() {
            
        }

        @Override
        public void onCmdServerError() {
            
        }
    };
    
    CmdSignalingWebServer cmdServer = null;
    public OVRS_Server(){
        System.out.println(System.getProperty("os.name"));
        if(System.getProperty("os.name").contains("Windows")){
            cmdServer = new CmdSignalingWebServer(0, new Draft_6455(), mCallBacksHandler);
        }
        else{
            cmdServer = new CmdSignalingWebServer(0, new Draft_17(), mCallBacksHandler);
        }
        cmdServer.start();
    }
    
    public void close(){
        try{
            if (null != cmdServer)
                cmdServer.stop();
            jMDns.unregisterAllServices();
        }
        catch(Exception ww){
            System.out.println(ww.getMessage());
        }
        
    }
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        final OVRS_Server server = new OVRS_Server();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            
            @Override
            public void run(){
                System.out.println("Closing...");
                server.close();
                
            }
        
        
        
        }
        );
        
    }
    
}
