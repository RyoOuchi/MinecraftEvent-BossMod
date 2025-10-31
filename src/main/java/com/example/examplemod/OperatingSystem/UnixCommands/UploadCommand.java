package com.example.examplemod.OperatingSystem.UnixCommands;

import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;
import com.example.examplemod.Blocks.VSCodeBlock.VSCodeBlockEntity;
import com.example.examplemod.OperatingSystem.CommandContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.nio.charset.StandardCharsets;

public class UploadCommand implements Command {
    @Override
    public String getName() {
        return "upload";
    }

    @Override
    public String getDescription() {
        return "Upload the file from the VSCode block above into the current directory.";
    }

    @Override
    public void execute(CommandContext context, String[] args) {
        final ServerBlockEntity serverBlockEntity = context.getServerBlockEntity();
        if (serverBlockEntity == null) {
            System.out.println("    Error: Server block entity is null.");
            return;
        }

        final Level level = serverBlockEntity.getLevel();
        if (level == null) {
            System.out.println("    Error: Level is null.");
            return;
        }

        final BlockPos idePos = serverBlockEntity.getBlockPos().above();
        final BlockEntity be = level.getBlockEntity(idePos);

        if (!(be instanceof VSCodeBlockEntity ideBlockEntity)) {
            System.out.println("    Error: No VSCode block found above the server.");
            return;
        }

        final ItemStack codeFileInSlot = ideBlockEntity.getItemInSlot();
        if (codeFileInSlot.isEmpty()) {
            System.out.println("    No code file found in the VSCode block slot.");
            return;
        }

        String fileName = codeFileInSlot.getOrCreateTag().getString("fileName");
        if (!fileName.endsWith(".code")) {
            fileName += ".code";
        }

        String fileContent = codeFileInSlot.getOrCreateTag().getString("code");
        if (fileContent.isEmpty()) {
            System.out.println("    The code file is empty, creating blank file instead.");
        }

        String dirPath = context.getCurrentDirectory().getFullPath();
        if (!dirPath.endsWith("/")) dirPath += "/";
        String filePath = dirPath + fileName;

        boolean success = context.getFileSystem().createFile(filePath, fileContent.getBytes(StandardCharsets.UTF_8));
        if (success)
            System.out.println("    Uploaded file: " + filePath);
        else
            System.out.println("    Failed to upload file. File may already exist.");
    }
}
