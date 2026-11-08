package ies.dam.servidor;

public enum RecSignals {
    EXIT_VIEW((byte) -2),
    EMAIL_SENT((byte) 51),
    CODE_SENT((byte) 52),
    PASSWORD_SENT((byte) 53),
    USER_SWITCH((byte) 97),
    MESSAGE_SENT((byte) 99),
    LOGIN((byte) 100),
    REGISTER((byte) 101),
    NEXT_RAND((byte) 105),
    ADD_RAND((byte) 106),
    START_RAND((byte) 107),
    STOP_RAND((byte) 108),
    ACCEPT_REQUEST((byte) 109),
    REJECT_REQUEST((byte) 110),
    SEND_REQUEST((byte) 111),
    DELETE_CONTACT((byte) 112);
    private final byte signal;
    RecSignals(byte signal){
        this.signal = signal;
    }
    public byte getSignal(){
        return signal;
    }
}
