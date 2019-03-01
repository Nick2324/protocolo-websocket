package co.com.servidor.websocket;

public class CausaClose {
    
    public static final short NORMAL_CLUSURE = 1000;
    public static final short ENDPOINT_GOING_AWAY = 1001;
    public static final short PROTOCOL_ERROR = 1002;
    public static final short DATATYPE_NOT_RECOGNIZED = 1003;
    public static final short FORMAT_DATATYPE_NOT_RECOGNIZED = 1007;
    public static final short POLICY_VIOLATED = 1008;
    public static final short MESSAGE_TOO_BIG = 1009;
    public static final short EXTENSIONS_NOT_SERVED = 1010;
    public static final short UNEXPECTED_CONDITION = 1001;
    
}
