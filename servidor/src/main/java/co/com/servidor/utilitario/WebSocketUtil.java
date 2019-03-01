package co.com.servidor.utilitario;

import co.com.servidor.websocket.CausaClose;
import java.nio.ByteBuffer;

public class WebSocketUtil {

    public static byte[] getPayloadClose(short codigo, byte[] causa){
        int length = 0;
        byte[] codigoByte = ByteBuffer.allocate(2).
                            putShort(codigo).
                            array();
        if(causa != null)length += causa.length;
        length += 2;
        byte[] payload = new byte[length];
        int i = 0;
        while(i < codigoByte.length){
           payload[i] = codigoByte[i++];
        }
        if(causa != null){
            for(int j = 0; j < causa.length; j++){
                payload[i++] = causa[j];
            }
        }
        return payload;
    }
    
}
