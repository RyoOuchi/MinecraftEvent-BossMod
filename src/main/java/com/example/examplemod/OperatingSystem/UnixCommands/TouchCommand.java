package com.example.examplemod.OperatingSystem.UnixCommands;

import com.example.examplemod.OperatingSystem.CommandContext;

public class TouchCommand implements Command {

    @Override
    public String getName() { return "touch"; }

    @Override
    public String getDescription() { return "Create a new empty file"; }

    @Override
    public void execute(CommandContext context, String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: touch <filename>");
            return;
        }

        String path = context.getFullPath(args[0]);

        var fs = context.getFileSystem();

        var file = fs.cat(path);
        if (file.isPresent()) {
            System.out.println("    File already exists: " + path);
            return;
        }

        boolean created = fs.createFile(path, new byte[0]);
        if (created)
            System.out.println("    Created empty file: " + path);
        else
            System.out.println("    Failed to create file: " + path);
    }
}