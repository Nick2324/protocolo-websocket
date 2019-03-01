package co.com.servidor.websocket;

import co.com.servidor.websocket.FrameWebSocket.Metadata.TipoFrame;
import java.util.ArrayList;

public interface AccionSegunFrame {
    
    public boolean trataFrame(TipoFrame tipo);
    public void accionFrame(ArrayList<FrameWebSocket> frames,
                            WebSocket socket,
                            byte[] payload);
    
}
