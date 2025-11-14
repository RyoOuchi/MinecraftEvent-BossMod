package com.example.examplemod.Packet;


import com.example.examplemod.Blocks.VSCodeBlock.VSCodeBlockEntity;
import com.example.examplemod.Blocks.VSCodeBlock.VSCodeBlockScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveCodePacket {
    private final String fileName;
    private final String code;
    private final BlockPos position;

    public SaveCodePacket(String fileName, String code, BlockPos position) {
        this.fileName = fileName;
        this.code = code;
        this.position = position;
    }

    public static void encode(SaveCodePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.fileName);
        buf.writeUtf(msg.code);
        buf.writeBlockPos(msg.position);
    }

    public static SaveCodePacket decode(FriendlyByteBuf buf) {
        return new SaveCodePacket(buf.readUtf(), buf.readUtf(), buf.readBlockPos());
    }

    public static void handle(SaveCodePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            System.out.println("Received code from client:\n" + msg.code + "\nfor file: " + msg.fileName);
            if (ctx.get().getSender() == null) return;
            ServerPlayer player = ctx.get().getSender();
            VSCodeBlockEntity blockEntity = (VSCodeBlockEntity) player.level.getBlockEntity(msg.position);
            if (blockEntity == null) return;
            final ItemStack itemStack = blockEntity.getItemInSlot();
            VSCodeBlockScreen.setCode(itemStack, msg.code, msg.fileName);

        });
        ctx.get().setPacketHandled(true);
    }
}
