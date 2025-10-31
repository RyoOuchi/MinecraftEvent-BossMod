package com.example.examplemod.Networking.ErrorCode.Responses;

import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Enums.ErrorCodes;
import com.example.examplemod.Networking.ErrorCode.IErrorCode;

public class FormatError implements IErrorCode {

    @Override
    public DataPacket response(DataPacket packet) {
        final String errorMessage = "The request was malformed or contained invalid fields.";
        final byte[] data = errorMessage.getBytes();
        return packet.updateData(data).updateErrorCode(ErrorCodes.FORMERR);
    }
}
