package com.example.examplemod.Networking.ErrorCode;

import com.example.examplemod.Networking.DataPacket;

public interface IErrorCode {
    public DataPacket response(final DataPacket packet);
}
