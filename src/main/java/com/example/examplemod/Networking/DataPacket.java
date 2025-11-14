package com.example.examplemod.Networking;

import com.example.examplemod.Networking.Enums.ErrorCodes;
import com.example.examplemod.Networking.Enums.Queries;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataPacket {
    private final List<BlockPos> routerPath;
    private final byte[] data;
    private final Queries queryType;
    private final List<BlockPos> cablePath;
    private final BlockPos senderBlockPos;
    private final BlockPos sendToBlockPos;
    private final ErrorCodes errorCode;
    private final BlockPos clientBlockPos;

    public DataPacket(final List<BlockPos> routerPath, final byte[] data, final Queries queryType, final List<BlockPos> cablePath, BlockPos senderBlockPos, BlockPos sendToBlockPos, BlockPos clientBlockPos) {
        this.routerPath = routerPath;
        this.data = data;
        this.queryType = queryType;
        this.cablePath = cablePath;
        this.senderBlockPos = senderBlockPos;
        this.sendToBlockPos = sendToBlockPos;
        this.clientBlockPos = clientBlockPos;
        this.errorCode = null;
    }

    public DataPacket(final List<BlockPos> routerPath, final byte[] data, final Queries queryType, final List<BlockPos> cablePath, BlockPos senderBlockPos, BlockPos sendToBlockPos, ErrorCodes errorCode, BlockPos clientBlockPos) {
        this.routerPath = routerPath;
        this.data = data;
        this.queryType = queryType;
        this.cablePath = cablePath;
        this.senderBlockPos = senderBlockPos;
        this.sendToBlockPos = sendToBlockPos;
        this.errorCode = errorCode;
        this.clientBlockPos = clientBlockPos;
    }

    public List<BlockPos> getRouterPath() {
        return routerPath;
    }
    public byte[] getData() {
        return data;
    }
    public Queries getQueryType() {
        return queryType;
    }
    public List<BlockPos> getCablePath() {
        return cablePath;
    }

    public BlockPos getSenderBlockPos() {
        return senderBlockPos;
    }

    public DataPacket updateCablePath(List<BlockPos> newCablePath) {
        return new DataPacket(this.routerPath, this.data, this.queryType, newCablePath, this.senderBlockPos, this.sendToBlockPos, this.errorCode, this.clientBlockPos);
    }

    public DataPacket updateRouterPath(List<BlockPos> newRouterPath) {
        return new DataPacket(newRouterPath, this.data, this.queryType, this.cablePath, this.senderBlockPos, this.sendToBlockPos, this.errorCode, this.clientBlockPos);
    }

    public DataPacket updateData(byte[] newData) {
        return new DataPacket(this.routerPath, newData, this.queryType, this.cablePath, this.senderBlockPos, this.sendToBlockPos, this.errorCode, this.clientBlockPos);
    }

    public DataPacket updateErrorCode(final ErrorCodes newErrorCode) {
        return new DataPacket(this.routerPath, this.data, this.queryType, this.cablePath, this.senderBlockPos, this.sendToBlockPos, newErrorCode, this.clientBlockPos);
    }

    public DataPacket invertRouterPath() {
        if (this.routerPath == null || this.routerPath.isEmpty()) {
            return this;
        }
        List<BlockPos> invertedPath = new ArrayList<>(this.routerPath);

        Collections.reverse(invertedPath);

        return updateRouterPath(invertedPath);
    }

    public DataPacket swapSenderAndReceiver() {
        return updateSendToBlockPos(this.senderBlockPos).updateSenderBlockPos(this.sendToBlockPos);
    }

    public DataPacket updateSenderBlockPos(BlockPos newSenderBlockPos) {
        return new DataPacket(this.routerPath, this.data, this.queryType, this.cablePath, newSenderBlockPos, this.sendToBlockPos, this.errorCode, this.clientBlockPos);
    }

    public DataPacket updateSendToBlockPos(BlockPos newSendToBlockPos) {
        return new DataPacket(this.routerPath, this.data, this.queryType, this.cablePath, this.senderBlockPos, newSendToBlockPos, this.errorCode, this.clientBlockPos);
    }

    public BlockPos getClientBlockPos() {
        return clientBlockPos;
    }

    @Nullable
    public ErrorCodes getErrorCode() {
        return errorCode;
    }

    public BlockPos getSendToBlockPos() {
        return sendToBlockPos;
    }

    public DataPacket addNewRouterPathToOldPath(final List<BlockPos> oldPath, final List<BlockPos> newPath) {
        oldPath.addAll(newPath);
        return updateRouterPath(oldPath);
    }

    public DataPacket updateQueryType(final Queries newQueryType) {
        return new DataPacket(this.routerPath, this.data, newQueryType, this.cablePath, this.senderBlockPos, this.sendToBlockPos, this.errorCode, this.clientBlockPos);
    }


}
