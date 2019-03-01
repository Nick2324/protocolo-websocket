package co.com.servidor.websocket;

import co.com.servidor.websocket.FrameWebSocket.Metadata.TipoFrame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;

public class ProtocoloWebSocket{

    public static boolean soloEscribe = false;
    private final String[] versiones = new String[]{"13","16"};
    public static final String RESULTADO = "resultado";
    public static final String RESPONSE = "respuesta";
    public static final String MAGIC_STRING = 
            "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private AccionSegunFrame[] acciones;
    
    public enum Header{
        
        HOST("Host"),
        UPGRADE("Upgrade"),
        CONNECTION("Connection"),
        WSKEY("Sec-WebSocket-Key"),
        WSACCEPT("Sec-WebSocket-Accept"),
        WSVERSION("Sec-WebSocket-Version");
        
        private final String header;
        
        private Header(String header){
            this.header = header;
        }
        
        @Override
        public String toString(){
            return this.header;
        }
        
    }
    
    public ProtocoloWebSocket() {
        //!*!*!*! Se debería generalizar
        this.acciones = new AccionSegunFrame[3];
        this.acciones[0] = new AccionClose();
        this.acciones[1] = new AccionPing();
        this.acciones[2] = new AccionPong();
    }
    
    private boolean verificaUpgrade(HashMap<String,String> request){
        String connection = request.get(Header.CONNECTION.toString());
        String upgrade = request.get(Header.UPGRADE.toString());
        boolean connectionUpgrade = false;
        if(connection != null){
            for(String valorConexion:connection.split(", ")){
                if(valorConexion.equals("Upgrade")){
                    connectionUpgrade = true;
                }
            }
        }
        return connectionUpgrade && upgrade != null &&
           upgrade.equals("websocket");
    }
    
    private boolean verificaVersion(HashMap<String,String> request){
        String version = request.get(Header.WSVERSION.toString());
        if(version != null){
            for(String versionSoportada:this.versiones){
                if(versionSoportada.equals(version)){
                    return true;
                }
            }
        }
        return false;
    }
    
    private String generarLlave(HashMap<String, String> request){
        String llave = request.get(Header.WSKEY.toString());
        if(llave != null){
            try {
                MessageDigest digest =
                    MessageDigest.getInstance("SHA-1");
                digest.reset();
                digest.update((llave + MAGIC_STRING).getBytes());
                String llaveAccept = new String(Base64.encodeBase64(digest.digest()));
                return llaveAccept;
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(ProtocoloWebSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
    
    private String respuestaHandshake(HashMap<String,String> request){
        String accept = this.generarLlave(request);
        String response = null;
        if(accept != null){
            response = "HTTP/1.1 101 Switching Protocols\r\n" +
                       Header.UPGRADE.toString() + ": websocket\r\n" +
                       Header.CONNECTION.toString() + ": Upgrade\r\n" +
                       Header.WSACCEPT.toString() + ": " + accept + "\r\n" +
                       "\r\n";
        }
        System.out.println(response);
        return response;
    }
    
    public HashMap<String,Object> handshake(InputStream is){
        HashMap<String,Object> response = new HashMap();
        BufferedReader datosPeticion = new BufferedReader(
                          new InputStreamReader(is));
        System.out.println("Stream leido");
        response.put(RESULTADO, true);
        response.put(RESPONSE, "");
        try {
            String leido = datosPeticion.readLine();
            System.out.println("*********");
            System.out.println(leido);
            System.out.println("*********");
            if(leido != null && leido.contains("GET ")){
                Header[] headers = Header.values();
                HashMap<String,String> request = new HashMap();
                do{
                    System.out.println(leido);
                    leido = datosPeticion.readLine();
                    if(!leido.equals("\r\n") && !leido.equals("\n") &&
                       !leido.equals("")){
                        if(leido.contains(":")){
                            String header = 
                                    leido.substring(0, leido.indexOf(":"));
                            for(Header h : headers){
                                if(h.toString().equals(header)){
                                    request.put(header,
                                                leido.substring(leido.indexOf(":") + 2));
                                }
                            }
                        }else{
                            throw new IOException();
                        }
                    }
                }while(!leido.equals("\r\n") && !leido.equals("\n") && 
                        !leido.equals(""));
                System.out.println("Version");
                System.out.println(this.verificaVersion(request));
                System.out.println("Upgrade");
                System.out.println(this.verificaUpgrade(request));
                if(this.verificaUpgrade(request) && 
                   this.verificaVersion(request)){
                    response.put(RESULTADO, true);
                    response.put(RESPONSE,this.respuestaHandshake(request));
                }else{
                    response.put(RESULTADO, false);
                }
            }else{
                response.put(RESULTADO, false);
            }
        } catch (IOException ex) {
            response.put(RESULTADO, false);
            Logger.getLogger(ProtocoloWebSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return response;
    }
    
    public byte[][] generaFrames(byte[] payload, int tipo) 
            throws FrameWSPayloadException{
        ArrayList<FrameWebSocket> framesMensaje = new ArrayList();
        int i = 0;
        byte[][] mensajePorFrames = null;
        if(payload != null){
            //Por ahora mientras pruebo. Si salen errores por mensajes mas grandes
            //o algo, corregir
            if(payload.length <= FrameWebSocket.Metadata.LONGITUD_127){
                if(tipo == 0){
                    framesMensaje.add(FrameWebSocket.generarFrameBinario(
                        payload, true, true));
                }else{
                    framesMensaje.add(FrameWebSocket.generarFrameTexto(
                        payload, true, true));
                }
            }
            mensajePorFrames = new byte[framesMensaje.size()][];
            i = 0;
            for(FrameWebSocket frame : framesMensaje){
                mensajePorFrames[i++] = frame.getFrameEnBytes();
            }
        }
        return mensajePorFrames;
    }
    
    public boolean esMensajeControl(ArrayList<FrameWebSocket> frames){
        boolean retorno = true;
        for(FrameWebSocket frame : frames){
            retorno = TipoFrame.CLOSE.getTipoFrame() <= 
                    frame.getMetadata().getTipoFrame().getTipoFrame() &&
                   frame.getMetadata().getTipoFrame().getTipoFrame() <= 
                    TipoFrame.CONTROL_F.getTipoFrame() && retorno;        
        }
        return retorno;
    }
    
    public void tratarMensajeControl(ArrayList<FrameWebSocket> frames,
                                     WebSocket socket){
        FrameWebSocket frame = frames.get(0);
        for(AccionSegunFrame accion:acciones){
            if(accion.trataFrame(frame.getMetadata().getTipoFrame())){
                //null?
                accion.accionFrame(frames, socket, null);
            }
        }
    }
    
    public ArrayList<FrameWebSocket> leeFrames(InputStream ir) throws FrameWSPayloadException{
        ArrayList<FrameWebSocket> frames = new ArrayList();
        try {
            FrameWebSocket frame;
            do{
                frame = new FrameWebSocket(ir);
                frames.add(frame);
            }while(!frame.mensajeFinal());
        }catch (FrameWSPayloadException | FrameWSMetadataException e) {
            e.printStackTrace();
            frames.clear();
            frames.add(FrameWebSocket.generarClose(null));
        }
        
        return frames;
    }
    
    public byte[] extraerMensaje(ArrayList<FrameWebSocket> frames){
        byte[] mensaje;    
        int length = 0;
        for(FrameWebSocket frameRecibido : frames){
            //System.out.println(frameRecibido.getMetadata().getMetadataFrame() + " *+*+*+*");
            length += frameRecibido.getPayload().length;
        }
        //System.out.println("Length final = " + length);
        mensaje = new byte[length];
        int indice = 0;
        for(FrameWebSocket frameRecibido : frames){
            //System.out.println(Arrays.toString(frameRecibido.getPayload()));
            System.arraycopy(frameRecibido.getPayload(), 0, 
                             mensaje, indice, 
                             frameRecibido.getPayload().length);
            indice += frameRecibido.getPayload().length;
        }
        return mensaje;
    }
    
    public byte[][] generarMensaje(byte[] payload) throws FrameWSPayloadException{
        return this.generaFrames(payload, 0);
    }
    
    public byte[][] generarMensaje(String payload) throws FrameWSPayloadException{
        if(payload == null){
            payload = "";
        }
        return this.generaFrames(payload.getBytes(), 1);
    }
    
}