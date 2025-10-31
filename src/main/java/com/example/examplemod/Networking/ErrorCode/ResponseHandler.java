package com.example.examplemod.Networking.ErrorCode;

import com.example.examplemod.Networking.DataPacket;
import com.example.examplemod.Networking.Enums.ErrorCodes;
import com.example.examplemod.Networking.ErrorCode.Responses.FormatError;
import com.example.examplemod.Networking.ErrorCode.Responses.NoError;
import com.example.examplemod.Networking.ErrorCode.Responses.NonExistentDomainError;

import java.util.EnumMap;
import java.util.Map;

public class ResponseHandler {
    public static Map<ErrorCodes, IErrorCode> responses = new EnumMap<>(ErrorCodes.class);

    static {
        responses.put(ErrorCodes.NOERROR, new NoError());
        responses.put(ErrorCodes.FORMERR, new FormatError());
        responses.put(ErrorCodes.NXDOMAIN, new NonExistentDomainError());
    }

    public static DataPacket handleResponse(final ErrorCodes code, final DataPacket packet) {
        return responses.get(code).response(packet);
    }
}
