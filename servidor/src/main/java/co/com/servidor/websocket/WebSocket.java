package co.com.servidor.websocket;

import co.com.servidor.utilitario.Cache;
import co.com.servidor.utilitario.WebSocketUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class WebSocket {

    private final ProtocoloWebSocket protocolo;
    private boolean isHandshakeAccepted;
    private final Socket cliente;
    protected Cache cache;
    protected boolean resuelvePeticionesParalelas;
    protected ServidorWebSocket callback;

    public enum CacheWS {

        SESION_CERRADA("SES_CER"),
        PING("PI"),
        PONG("PO");

        public String cacheKey;

        private CacheWS(String cacheKey) {
            this.cacheKey = cacheKey;
        }

        @Override
        public String toString() {
            return this.cacheKey;
        }

    }

    public WebSocket(Socket cliente, ServidorWebSocket callback) {
        this.cliente = cliente;
        this.protocolo = new ProtocoloWebSocket();
        this.cache = new Cache();
        this.resuelvePeticionesParalelas = false;
        this.callback = callback;
    }

    public abstract Cache getCache();

    public abstract void onClose();

    public void close(String causa){
        close(CausaClose.NORMAL_CLUSURE,causa.getBytes());
    }
    
    public void close (short codigo, String causa){
        this.close(codigo,causa.getBytes());
    }
    
    public void close(short codigo, byte[] causa){
        
        new AccionClose().accionFrame(null, 
                                      this, 
                                      WebSocketUtil.getPayloadClose(
                                                   codigo, 
                                                   causa));
        this.closeConnection();
        this.callback.eliminarSocket(this);
    }
    
    public void closeConnection(){
        try{
            this.cliente.close();
        }catch(IOException | NullPointerException e){
            e.printStackTrace();
        }
    }

    protected abstract void onMessage(JSONObject mensaje) throws JSONException;

    protected abstract void onOpen();

    protected void onError(String causa){
        this.onClose();
        this.close(CausaClose.UNEXPECTED_CONDITION,
                   causa);
    }
    
    private void loopEscrituraEnvio(byte[][] frames, OutputStream or) throws 
            IOException{
        for (byte[] frame : frames) {
            or.write(frame);
            or.flush();
        }
    }
    
    public void send(byte[] datos){
        try {
            this.loopEscrituraEnvio(this.protocolo.generarMensaje(datos), 
                                    this.cliente.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            this.closeConnection();
        } catch (FrameWSPayloadException ex) {
            ex.printStackTrace();
        }
    }
    
    public void send(String datos){
        try {
            loopEscrituraEnvio(protocolo.generarMensaje(datos), 
                               cliente.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            this.closeConnection();
        } catch (FrameWSPayloadException ex) {
            ex.printStackTrace();
        }
    }
    
    public void listen(){
        WebSocket ws = this;
        new Thread(){
            
            @Override
            public void run(){
                try{
                    while(!cliente.isClosed()){
                        if(cliente.getInputStream().available() > 0){
                            ArrayList<FrameWebSocket> frames = 
                                    protocolo.leeFrames(cliente.getInputStream());
                            if(!protocolo.esMensajeControl(frames)){
                                if(resuelvePeticionesParalelas){
                                    new Thread(){
                                        
                                        @Override
                                        public void run(){
                                             try {
                                                onMessage(
                                                        new JSONObject(new String(
                                                                protocolo.extraerMensaje(
                                                                        frames))));
                                            } catch (JSONException ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }.start();
                                }else{
                                    onMessage(
                                        new JSONObject(new String(
                                                        protocolo.extraerMensaje(
                                                    frames))));
                                }
                            }else{
                                protocolo.tratarMensajeControl(frames,ws);
                            }
                        }
                    }
                }catch(JSONException | FrameWSPayloadException | IOException ex){
                    ex.printStackTrace();
                }
            }
            
        }.start();
    }
    
    public boolean handshake() throws IOException {
        if (!cliente.isClosed()) {
            PrintWriter or = 
                    new PrintWriter(this.cliente.getOutputStream(),
                                    true);
            
            this.isHandshakeAccepted = false;
            HashMap<String, Object> handshakeProcesado = null;

            final ScheduledThreadPoolExecutor executor
                    = new ScheduledThreadPoolExecutor(1);
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    if (!isHandshakeAccepted) {
                        try {
                            cliente.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }, 5, TimeUnit.SECONDS);

            handshakeProcesado
                    = this.protocolo.handshake(this.cliente.getInputStream());
            this.isHandshakeAccepted = (Boolean) handshakeProcesado.get(ProtocoloWebSocket.RESULTADO);
            
            if(this.isHandshakeAccepted){
                or.write(
                    (String)handshakeProcesado.get(ProtocoloWebSocket.RESPONSE));
                or.flush();
                this.onOpen();
            }
        }

        return this.isHandshakeAccepted;
        
    }

}
