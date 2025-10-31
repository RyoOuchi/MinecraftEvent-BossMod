package com.example.examplemod.Packet;

import com.example.examplemod.Screens.BrowserScreen.BrowserDisplayScreen;
import com.example.examplemod.Screens.Screen.CustomScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BrowserResponsePacket {
    private final BlockPos pos;
    private final String message;
    private final String fileName;

    public BrowserResponsePacket(BlockPos pos, String message, String fileName) {
        this.pos = pos;
        this.message = message;
        this.fileName = fileName;
    }

    public static void encode(BrowserResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.message);
        buf.writeUtf(msg.fileName);
    }

    public static BrowserResponsePacket decode(FriendlyByteBuf buf) {
        return new BrowserResponsePacket(buf.readBlockPos(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(BrowserResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(BrowserResponsePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new BrowserDisplayScreen(msg.message));
    }
}
