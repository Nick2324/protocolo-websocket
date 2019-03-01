package co.com.servidor.websocket;

import co.com.servidor.websocket.FrameWebSocket.Metadata.TipoFrame;
import java.util.ArrayList;

public class AccionClose implements AccionSegunFrame{

    @Override
    public boolean trataFrame(FrameWebSocket.Metadata.TipoFrame tipo) {
        return tipo == TipoFrame.CLOSE;
    }

    @Override
    public void accionFrame(ArrayList<FrameWebSocket> frames,
                            WebSocket socket,
                            byte[] payload) {
        if(!socket.getCache().enCache(WebSocket.CacheWS.SESION_CERRADA.toString())){
            try {
                socket.onClose();
                socket.send(FrameWebSocket.generarClose(payload).getFrameEnBytes());
                socket.getCache().setCacheVar(
                        WebSocket.CacheWS.SESION_CERRADA.toString(), null);
            } catch (FrameWSPayloadException ex) {
                ex.printStackTrace();
            }
            socket.closeConnection();
        }
    }
    
}
