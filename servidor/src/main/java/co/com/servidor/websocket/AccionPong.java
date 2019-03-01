package co.com.servidor.websocket;

import co.com.servidor.utilitario.WebSocketUtil;
import co.com.servidor.websocket.FrameWebSocket.Metadata.TipoFrame;
import java.util.ArrayList;

public class AccionPong implements AccionSegunFrame {

    @Override
    public boolean trataFrame(FrameWebSocket.Metadata.TipoFrame tipo) {
        return tipo == TipoFrame.PONG;
    }

    @Override
    public void accionFrame(ArrayList<FrameWebSocket> frames,
                            WebSocket socket,
                            byte[] payload) {
        if(frames.size() == 1){
            if(socket.getCache().enCache(WebSocket.CacheWS.PING.toString())){
                String[] payloadEnviado = 
                        (String[])socket.getCache().getCacheVar(
                                WebSocket.CacheWS.PING.toString());
                boolean encontrado = false;
                for(String payloadPong:payloadEnviado){
                    if(new String(frames.get(0).getPayload()).equals(payloadPong)){
                        encontrado = true;
                        break;
                    }
                }
                if(encontrado){
                    socket.getCache().removerCache(WebSocket.CacheWS.PING.toString());
                }else{
                    //!*!*! No dice que se hace
                    //new AccionClose().accionFrame(frames, socket);
                }
            }
        }else{
            new AccionClose().accionFrame(frames, socket, 
                                          WebSocketUtil.getPayloadClose(
                                                CausaClose.PROTOCOL_ERROR, 
                                                "PING invalido".getBytes()));
        }
    }
    
}
