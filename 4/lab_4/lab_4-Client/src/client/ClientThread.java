package client;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

public class ClientThread implements Runnable{
    
    Socket socket;
    DataInputStream dis;
    MainForm main;
    StringTokenizer st;
    
    public ClientThread(Socket socket, MainForm main){
        this.main = main;
        this.socket = socket;
        try {
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            main.appendMessage("[IOException]: "+ e.getMessage(), "Error", Color.RED, Color.RED);
        }
    }

    @Override
    public void run() {
        try {
            while(true){
                String data = dis.readUTF();
                st = new StringTokenizer(data);
                /** Get Message CMD **/
                String CMD = st.nextToken();
                switch(CMD){
                    case "CMD_MESSAGE":
                        String msg = "";
                        String frm = st.nextToken();
                        while(st.hasMoreTokens()){
                            msg = msg +" "+ st.nextToken();
                        }
                        main.appendMessage(msg, frm, Color.MAGENTA, Color.BLUE);
                        break;
                        
                    case "CMD_ONLINE":
                        Vector online = new Vector();
                        while(st.hasMoreTokens()){
                            String list = st.nextToken();
                            if(!list.equalsIgnoreCase(main.username)){
                                online.add(list);
                            }
                        }
                        main.appendOnlineList(online);
                        break;
                        
                    default: 
                        main.appendMessage("[CMDException]: Unknown Command "+ CMD, "CMDException", Color.RED, Color.RED);
                    break;
                }
            }
        } catch(IOException e){
            main.appendMessage("[IOException]: "+ e.getMessage(), "Error", Color.RED, Color.RED);
        }
    }
}
