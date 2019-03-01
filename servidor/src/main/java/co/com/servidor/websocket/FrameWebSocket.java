package co.com.servidor.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FrameWebSocket {
    
    private Metadata metadata;
    private byte[] payload;
    
    public static final class Metadata{
        
        private byte metadataFrame;
        private long payloadLen;
        private byte[] mask;
        private TipoFrame tipoFrame;
        
        public static final long LONGITUD_125 = 125;
        public static final long LONGITUD_126 = (long)Math.pow(2,16);
        public static final long LONGITUD_127 = (long)Math.pow(2,63);
        
        public enum TipoFrame{
            CONTINUATION((byte)0x0),
            TEXT((byte)0x1),
            BINARY((byte)0x2),
            NON_CONTROL_3((byte)0x3),
            NON_CONTROL_4((byte)0x4),
            NON_CONTROL_5((byte)0x5),
            NON_CONTROL_6((byte)0x6),
            NON_CONTROL_7((byte)0x7),
            CLOSE((byte)0x8),
            PING((byte)0x9),
            PONG((byte)0xA),
            CONTROL_B((byte)0xB),
            CONTROL_C((byte)0xC),
            CONTROL_D((byte)0xD),
            CONTROL_E((byte)0xE),
            CONTROL_F((byte)0xF);
            
            private byte tipoFrame;
            
            private TipoFrame(byte tipoFrame){
                this.tipoFrame = tipoFrame;
            }
            
            public byte getTipoFrame(){
                return this.tipoFrame;
            }
            
        }
        
        public Metadata(){}
        
        public Metadata(InputStream ir) throws FrameWSMetadataException{
            try{
                String extensionInvalida = "Extensiones no implementadas";
                int lectura;
                do{
                    lectura = ir.read();
                }while(lectura == -1);
                this.metadataFrame = (byte)lectura;
                byte maskPayLoadLength;
                int payloadLength;
                
                //Utiliza bits rsv1, rsv2 o rsv3
                if((this.metadataFrame >= -128 && this.metadataFrame <= -113)||
                      (this.metadataFrame >= 0 && this.metadataFrame <= 15)){
                    for(TipoFrame tipo:TipoFrame.values()){
                        if(tipo.getTipoFrame() == (this.metadataFrame & 15)){
                            this.tipoFrame = tipo;
                        }
                    }
                    maskPayLoadLength = (byte)ir.read();
                    boolean getMask = maskPayLoadLength < 0;
                    payloadLength = getMask? 
                                    this.convierteByteAInt(maskPayLoadLength) - 
                                        128:
                                    this.convierteByteAInt(maskPayLoadLength);
                    if(payloadLength <= 125){
                        this.payloadLen = payloadLength;
                    }else{
                        byte[] payloadGrande;
                        short cantidadBytes;
                        if(payloadLength == 126){ //payload de 16 bits
                            cantidadBytes = 2;                           
                        }else{ //payload de 64 bits
                            cantidadBytes = 8;
                        }
                        payloadGrande = new byte[cantidadBytes];
                        ir.read(payloadGrande);
                        ByteBuffer bb = ByteBuffer.wrap(payloadGrande);
                        if(cantidadBytes == 2){
                            this.payloadLen = bb.asShortBuffer().get();
                        }else{
                            this.payloadLen = bb.asLongBuffer().get();
                        }
                    }
                    if(getMask){
                        this.mask = new byte[4];
                        ir.read(this.mask);
                    }
                }else{
                    throw new FrameWSMetadataException(extensionInvalida);
                }
            }catch(IOException e){
                e.printStackTrace();
                throw new FrameWSMetadataException("Frame malformado");
            }
        }
        
        private int convierteByteAInt(byte aConvertir){
            if(aConvertir > 0){
               return (int)aConvertir;
            }else{
               return 128 + (int)aConvertir + 128;
            }
         }
        
        public boolean finalMensaje(){
            return this.metadataFrame < 0;
        }

        public void setPayloadLen(long payloadLen) {
            this.payloadLen = payloadLen;
        }
        
        public long getPayloadLen() {
            return this.payloadLen;
        }

        public byte[] getMask() {
            return mask;
        }
        
        public void generarMetadata(boolean fin, Metadata.TipoFrame opcode){
            this.metadataFrame = opcode.getTipoFrame();
            if(fin){
                this.metadataFrame += (byte)128;
            }
        }
        
        public byte[] generaMetaBytes(){
            int cantidadBytes = 1;
            byte sumadorMascara;
            short extendido;
            
            if(this.mask != null && this.mask.length == 4){
                cantidadBytes += 4;
                sumadorMascara = 64;
            }else{
                sumadorMascara = 0;
            }
            
            if(this.payloadLen <= 125){
                extendido = 0;
                cantidadBytes +=1;
            }else if(this.payloadLen <= 65536){
                extendido = 2;
                cantidadBytes += 3;
            }else{
                extendido = 8;
                cantidadBytes += 9;
            }
            
            byte[] metadata = new byte[cantidadBytes];
            metadata[0] = this.metadataFrame;
            
            switch(extendido){
                case 0:
                    metadata[1] = (byte)(sumadorMascara + this.payloadLen);
                    break;
                case 2:
                    metadata[1] = (byte)(sumadorMascara + 126);
                    ByteBuffer bb = ByteBuffer.allocate(extendido);
                    bb.putShort((short)this.payloadLen);
                    bb.rewind();
                    System.arraycopy(bb.array(), 0, metadata, 2, extendido);
                    break;
                case 8:
                    metadata[1] = (byte)(sumadorMascara + 127);
                    ByteBuffer bb1 = ByteBuffer.allocate(extendido);
                    bb1.putLong(this.payloadLen);
                    bb1.rewind();
                    System.arraycopy(bb1.array(), 0, metadata, 2, extendido);
                    break;
            }
            if(sumadorMascara > 0){
                System.arraycopy(this.mask, 0, 
                                 metadata, metadata.length - 4,
                                 4);
            }
            return metadata;
        }
        
        public byte getMetadataFrame(){
            return this.metadataFrame;
        }

        public TipoFrame getTipoFrame() {
            return tipoFrame;
        }
        
        
        public static boolean esLongitudPayloadValida(long longitud){
            //(2^63)/(2^3) = 2^60
            return longitud <= Math.pow(2,60);
        }
        
    }

    public FrameWebSocket(InputStream ir) throws 
            FrameWSPayloadException,FrameWSMetadataException{
        byte[] mensaje = null;
        //Lee el frame websocket
        try{
            metadata = new Metadata(ir);
            mensaje = new byte[(int)metadata.getPayloadLen()];
            ir.read(mensaje);
        }catch(IOException e){
            throw new FrameWSPayloadException();
        }
        this.payload = this.decodificaPayload(metadata,mensaje);
    }
    
    public FrameWebSocket(Metadata.TipoFrame opcode, byte[] payload, 
            boolean fin, boolean inicial) throws FrameWSPayloadException{
        this.metadata = new Metadata();
        if(payload == null){
            payload = new byte[0];
        }
        if(Metadata.esLongitudPayloadValida(payload.length)){
            this.metadata.setPayloadLen(payload.length);
            if(!inicial)opcode = Metadata.TipoFrame.CONTINUATION;
            this.metadata.generarMetadata(fin, opcode);
            this.payload = payload;
        }else{
            throw new FrameWSPayloadException();
        }
    }
    
    public byte[] getFrameEnBytes(){
        byte[] metadataFrame = this.metadata.generaMetaBytes();
        byte[] frame = new byte[metadataFrame.length + this.payload.length];
        System.arraycopy(metadataFrame, 0, 
                         frame, 0, 
                         metadataFrame.length);
        System.arraycopy(this.payload, 0, 
                         frame, metadataFrame.length, 
                         this.payload.length);
        return frame;
    }
    
    private byte[] decodificaPayload(Metadata metadata, byte[] bytes){
        byte[] decodificado = new byte[bytes.length];
        //System.out.println("Mascara = " + Arrays.toString(metadata.getMask()));
        for(int i = 0; i < bytes.length; i++){
            decodificado[i] = (byte)(bytes[i] ^ metadata.getMask()[i & 0x3]);
        }
        //System.out.println("Final decodificado = " + Arrays.toString(decodificado));
        return decodificado;
    }
    
    public boolean mensajeFinal(){
        return this.metadata.finalMensaje();
    }

    public byte[] getPayload() {
        return payload;
    }
    
    public Metadata getMetadata(){
        return this.metadata;
    }
    
    //*!*!*!*!*! PONER EN BUILDER
    public static FrameWebSocket generarFrameBinario(byte[] payload, 
            boolean fin, boolean inicial) throws FrameWSPayloadException{
        return new FrameWebSocket(Metadata.TipoFrame.BINARY,
                                  payload,fin,inicial);
    }
    
    public static FrameWebSocket generarFrameTexto(byte[] payload, 
            boolean fin, boolean inicial) throws FrameWSPayloadException{
        return new FrameWebSocket(Metadata.TipoFrame.TEXT,
                                  payload, fin, inicial);
    }
    
    public static FrameWebSocket generarPong(byte[] payload) throws 
            FrameWSPayloadException{
        try{
            if(payload.length > 125){
                throw new FrameWSPayloadException();
            }
            return new FrameWebSocket(Metadata.TipoFrame.PONG,
                                        payload, true, true);
        }catch(NullPointerException e){
            throw new FrameWSPayloadException();
        }
    }
    
    public static FrameWebSocket generarPing(byte[] payload) throws 
            FrameWSPayloadException{
        try{
            if(payload.length > 125){
                throw new FrameWSPayloadException();
            }
            return new FrameWebSocket(Metadata.TipoFrame.PING,
                                      payload, true, true);
        }catch(NullPointerException e){
            throw new FrameWSPayloadException();
        }
    }
    
    public static FrameWebSocket generarClose(byte[] payload) throws
            FrameWSPayloadException {
        if(payload != null && payload.length > 125){
            throw new FrameWSPayloadException();
        }
        return new FrameWebSocket(Metadata.TipoFrame.CLOSE,
                                  payload, true, true);
    }
    
}
