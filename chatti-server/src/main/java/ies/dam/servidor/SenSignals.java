package ies.dam.servidor;

public enum SenSignals {
    SEND_MESSAGE((byte) 100),
    SEND_MSG_DATA((byte) 101),
    SEND_MESSAGES((byte) 102),
    TERMINATE((byte) -10);
    private final byte signal;
    SenSignals(byte b) {
        this.signal = b;
    }
    public byte getSignal(){
        return signal;
    }
}
