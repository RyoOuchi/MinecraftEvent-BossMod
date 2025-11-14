package com.example.examplemod.DoNotTouch.Packets;

import com.example.examplemod.DoNotTouch.GUI.TeamLogInScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenGuiPacket {
    public OpenGuiPacket() {}

    public static void encode(OpenGuiPacket msg, FriendlyByteBuf buf) {}

    public static OpenGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenGuiPacket();
    }

    public static void handle(OpenGuiPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleClient(OpenGuiPacket msg) {
        Minecraft.getInstance().setScreen(new TeamLogInScreen());
    }
}