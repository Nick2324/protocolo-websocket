package co.com.servidor.websocket;

import co.com.servidor.utilitario.WebSocketUtil;
import co.com.servidor.websocket.FrameWebSocket.Metadata.TipoFrame;
import java.util.ArrayList;

public class AccionPing implements AccionSegunFrame {

    @Override
    public boolean trataFrame(FrameWebSocket.Metadata.TipoFrame tipo) {
        return tipo == TipoFrame.PING;
    }

    @Override
    public void accionFrame(ArrayList<FrameWebSocket> frames,
                            WebSocket socket,
                            byte[] payload) {
        if(frames.size() == 1){
            try {
                socket.send(FrameWebSocket.generarPong(
                        frames.get(0).getPayload()).getFrameEnBytes());
            } catch (FrameWSPayloadException ex) {
                new AccionClose().accionFrame(frames, 
                                              socket, 
                                              WebSocketUtil.getPayloadClose(
                                                   CausaClose.PROTOCOL_ERROR, 
                                                   "PING invalido".getBytes()));
            }
        }
    }
    
}
