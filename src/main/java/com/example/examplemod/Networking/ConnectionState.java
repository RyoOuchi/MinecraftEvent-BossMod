package com.example.examplemod.Networking;


public record ConnectionState(int clientSeq, int serverSeq) {
    public ConnectionState updateClientSeq(int newClientSyn) {
        return new ConnectionState(newClientSyn, this.serverSeq);
    }

    public ConnectionState updateServerSeq(int newServerSyn) {
        return new ConnectionState(this.clientSeq, newServerSyn);
    }

    public boolean validateClientAckNumber(int ackNumber) {
        return ackNumber == this.clientSeq + 1;
    }

    public boolean validateServerAckNumber(int ackNumber) {
        return ackNumber == this.serverSeq + 1;
    }

    public int getClientSeq() {
        return clientSeq;
    }
    public int getServerSeq() {
        return serverSeq;
    }

}
