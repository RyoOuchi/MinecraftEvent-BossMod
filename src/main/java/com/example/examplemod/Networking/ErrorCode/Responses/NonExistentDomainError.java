package com.example.examplemod.Networking.ErrorCode.Responses;

import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Enums.ErrorCodes;
import com.example.examplemod.Networking.ErrorCode.IErrorCode;

public class NonExistentDomainError implements IErrorCode {
    @Override
    public DataPacket response(DataPacket packet) {
        final String errorMessage = "The domain name does not exist.";
        final byte[] data = errorMessage.getBytes();
        return packet.updateData(data).updateErrorCode(ErrorCodes.NXDOMAIN);
    }
}
