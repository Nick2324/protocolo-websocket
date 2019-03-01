package co.com.servidor.websocket;

import co.com.servidor.utilitario.Cache;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public abstract class ServidorWebSocket {

    protected Thread servidor;
    protected int port;
    protected String host;
    protected ArrayList<WebSocket> sockets;
    protected Cache cache;
    
    public ServidorWebSocket() {
        this.port = 80;
        this.host = "127.0.0.1";
        this.cache = new Cache();
        this.sockets = new ArrayList();
    }
    
    public ServidorWebSocket(int port){
        this.port = port;
        this.host = "127.0.0.1";
        this.cache = new Cache();
        this.sockets = new ArrayList();
    }
    
    public ServidorWebSocket(int port, String host){
        this.port = port;
        this.host = host;
        this.cache = new Cache();
        this.sockets = new ArrayList();
    }
    
    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    protected abstract WebSocket instanciarWebSocket(Socket cliente);
    protected abstract void preInstanciaSocket();
    protected abstract void postInstanciaSocket(WebSocket websocket);
    protected abstract void onServerStart();
    protected abstract void onServerShutDown();
    
    public boolean eliminarSocket(WebSocket ws){
        return this.sockets.remove(ws);
    }
    
    public synchronized void iniciarServidor(){
        try {
            //servidor
            ServerSocket ss = new ServerSocket(this.port);
            if(!ss.isClosed()){
                this.onServerStart();
                while(!ss.isClosed()){
                    this.preInstanciaSocket();
                    WebSocket websocket = this.instanciarWebSocket(ss.accept());
                    this.sockets.add(websocket);
                    if(websocket.handshake()){
                        websocket.listen();
                        this.postInstanciaSocket(websocket);
                    }
                }
                this.onServerShutDown();
            }
        } catch (IOException ex){
            ex.printStackTrace();
        }
    }

}