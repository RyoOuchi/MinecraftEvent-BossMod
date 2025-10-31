package com.example.examplemod.OperatingSystem.UnixCommands;


import com.example.examplemod.OperatingSystem.CommandContext;
import com.example.examplemod.OperatingSystem.DirectoryNode;

public class CdCommand implements Command {
    @Override
    public String getName() { return "cd"; }
    @Override
    public String getDescription() { return "Change directory"; }

    @Override
    public void execute(CommandContext context, String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: cd <path>");
            return;
        }

        String targetPath = args[0].trim();

        if (targetPath.equals(".")) {
            return;
        }

        if (targetPath.equals("..")) {
            DirectoryNode current = context.getCurrentDirectory();
            if (current.getParent() != null) {
                context.setCurrentDirectory(current.getParent());
            }
            return;
        }

        // Resolve relative or absolute paths
        String resolvedPath = context.getFullPath(targetPath);
        DirectoryNode target = context.getFileSystem().navigateToDirectory(resolvedPath);

        if (target != null) {
            context.setCurrentDirectory(target);
        } else {
            System.out.println("    Directory not found: " + targetPath);
        }
    }

}