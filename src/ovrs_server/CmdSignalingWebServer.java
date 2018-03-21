/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ovrs_server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import netscape.javascript.JSObject;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 *
 * @author mreddy3
 */
public class CmdSignalingWebServer extends WebSocketServer{
    
    final int NEW_ENTRY = 0;
    final int EXISTING_ENTRY = 1;
    final int DEL_ENTRY = 2;
    final int VIDEO = 3;
    final int NO_ROOM = -1;


    final String CMD_KEY = "CMD";
    final String FROM_KEY = "FROM";
    final String VFRAME_KEY = "VFRAME";
    final String ADDR_KEY = "ADDR";
    final String DEL_ENTRY_KEY = "DEL_ENTRY";
    final String EXISTING_ENTRY_KEY = "EXISTING_ENTRY";
    final String ENTRIES_KEY = "ENTRIES";
    final String NEW_ENTRY_KEY = "NEW_ENTRY";
    
    
    
    HashMap<String, WebSocket> mAllParticipants = null;
    //Set<WebSocket> mAllParticipants = null;
    CmdServerCallBacks mCallBacks = null;
    VideoHandlingServer mVideoHandlerSocket = null;
    public class receivedFrame{
        WebSocket ws;
        JsonObject receivedFrame;
        
        public receivedFrame(WebSocket ws, JsonObject receivedFrame){
            this.ws = ws;
            this.receivedFrame = receivedFrame;
        }
    }
    ArrayList<JsonObject> mSenderQueue = null;
    
   public CmdSignalingWebServer(int port, Draft draft, CmdServerCallBacks callbacks){
       super(new InetSocketAddress(port), Collections.singletonList(draft));
       //mAllParticipants = new HashSet<>();
       mAllParticipants = new HashMap<String, WebSocket>();
       mCallBacks = callbacks;
       
       mVideoHandlerSocket = new VideoHandlingServer();
       mSenderQueue = new ArrayList<>();
       
   }
    @Override
    public void onOpen(WebSocket ws, ClientHandshake ch) {
        
        
        if (mAllParticipants.size() == 5){
            JsonObject error = new JsonObject();
            error.addProperty("CMD", NO_ROOM);
            ws.send(error.toString());
            
            return;
        }
        
        ParticipantHolder curParticipant = new ParticipantHolder();
        curParticipant.sethostaddress(ws.getRemoteSocketAddress().getAddress().toString());
        
        
        JsonObject addParticipantCmd = new JsonObject();
        addParticipantCmd.addProperty("CMD", NEW_ENTRY);
        addParticipantCmd.addProperty("ADDR", ws.getRemoteSocketAddress().getAddress().toString());
        
        JsonArray existinParticipants = new JsonArray();
        Iterator iter = mAllParticipants.entrySet().iterator();
        //1. Todo for the allprevious participants tell that a new member is joined
        while(iter.hasNext()){  
            Map.Entry entry = (Map.Entry)iter.next();
            WebSocket curSoc = (WebSocket)entry.getValue();
            curSoc.send(addParticipantCmd.toString());
            existinParticipants.add(entry.getKey().toString());
        }
        //2. for the new Participant tell the number of existing participants
        if (existinParticipants.size() > 0){
            JsonObject addExistingCMD = new JsonObject();
            addExistingCMD.addProperty("CMD", EXISTING_ENTRY);
            addExistingCMD.add("ENTRIES", existinParticipants);
            ws.send(addExistingCMD.toString());
        }
        
        //3. update new participant to list
        mAllParticipants.put(ws.getRemoteSocketAddress().getAddress().toString(), ws);
        
    }
    
       

    @Override
    public void onClose(WebSocket ws, int i, String string, boolean bln) {
        String curIp = ws.getRemoteSocketAddress().getAddress().toString();
        System.out.println("Client close before remove "+mAllParticipants.size());
        if(mAllParticipants.containsKey(curIp)){
            mAllParticipants.remove(curIp);
            
            Iterator iter = mAllParticipants.entrySet().iterator();
            //1. Todo for the allprevious participants tell that a new member is joined
            while(iter.hasNext()){  
                Map.Entry entry = (Map.Entry)iter.next();
                WebSocket curSoc = (WebSocket)entry.getValue();
                JsonObject removeParticipantCMD = new JsonObject();
                removeParticipantCMD.addProperty("CMD", DEL_ENTRY);
                removeParticipantCMD.addProperty("ADDR", ws.getRemoteSocketAddress().getAddress().toString());
                curSoc.send(removeParticipantCMD.toString());
            }
            
        }
        System.out.println("Client close after remove "+mAllParticipants.size());
        /*
        
        if (mAllParticipants.contains(ws)){
            
            System.out.println("Contains "+ws.getRemoteSocketAddress().toString());
                   
            mAllParticipants.remove(ws);
            for(WebSocket cur : mAllParticipants){
                JsonObject removeParticipantCMD = new JsonObject();
                removeParticipantCMD.addProperty("CMD", DEL_ENTRY);
                removeParticipantCMD.addProperty("ADDR", ws.getRemoteSocketAddress().getAddress().toString());
                cur.send(removeParticipantCMD.toString());
            }
        }
        
        
        */
        
       
    }

   
    @Override
    public void onError(WebSocket ws, Exception excptn) {
        System.out.println("onError "+ excptn.getMessage());
    }

    @Override
    public void onStart() {
           System.out.println("Started..."+getPort() +" "+getAddress().getAddress().getHostAddress());
           mCallBacks.onCmdServerStarted(getPort());
    }

    @Override
    public void onMessage(WebSocket ws, String inFrame) {
        sendToAll(ws, inFrame);
    }
    
    synchronized void sendToAll(WebSocket ws, String inFrame){
        
        
        Iterator iter = mAllParticipants.entrySet().iterator();
            //1. Todo for the allprevious participants tell that a new member is joined
            while(iter.hasNext()){  
                Map.Entry entry = (Map.Entry)iter.next();
                if (ws.getRemoteSocketAddress().getAddress().toString().equalsIgnoreCase(entry.getKey().toString())){
                    continue;
                }
                
                try{
                    WebSocket curSoc = (WebSocket)entry.getValue();
                    JsonObject videoFrame = new JsonObject();
                    videoFrame.addProperty("CMD", VIDEO);
                    videoFrame.addProperty("FROM", ws.getRemoteSocketAddress().getAddress().toString());
                    videoFrame.addProperty("VFRAME", inFrame);               
                    curSoc.send(videoFrame.toString());
                }
                catch(Exception ee){
                    System.out.println(ee.getMessage());
                }                
               
        }
            
        /*
        //String sendData = ws.getRemoteSocketAddress().toString()+"#@@#"+inFrame;
        for(WebSocket cur : mAllParticipants){
            if (cur.equals(ws)){
                continue;
            }
            else{
                try{
                    JsonObject videoFrame = new JsonObject();
                    videoFrame.addProperty("CMD", VIDEO);
                    videoFrame.addProperty("FROM", ws.getRemoteSocketAddress().getAddress().toString());
                    videoFrame.addProperty("VFRAME", inFrame);               
                    cur.send(videoFrame.toString());
                }
                catch(Exception ee){
                    System.out.println(ee.getMessage());
                }                
            }
        }*/
    }
    
    
    
    
    
}
