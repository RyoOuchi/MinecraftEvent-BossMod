package com.example.examplemod.Networking.ErrorCode.Responses;

import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Enums.ErrorCodes;
import com.example.examplemod.Networking.ErrorCode.IErrorCode;

public class NoError implements IErrorCode {
    @Override
    public DataPacket response(final DataPacket packet) {
        return packet.updateErrorCode(ErrorCodes.NOERROR);
    }
}
