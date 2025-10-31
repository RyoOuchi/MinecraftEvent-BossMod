package com.example.examplemod.Packet;

import com.example.examplemod.Screens.TerminalScreen.TerminalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TerminalOutputPacket {
    private final String output;
    private final String currentPath;

    public TerminalOutputPacket(String output, String currentPath) {
        this.output = output;
        this.currentPath = currentPath;
    }

    public static void encode(TerminalOutputPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.output);
        buf.writeUtf(packet.currentPath);
    }

    public static TerminalOutputPacket decode(FriendlyByteBuf buf) {
        return new TerminalOutputPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(TerminalOutputPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(packet));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(TerminalOutputPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TerminalScreen terminal) {
            for (String line : msg.output.split("\n")) {
                terminal.addLogLine(line);
            }
            terminal.setPromptPath(msg.currentPath);
        }
    }

}
