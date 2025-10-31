package com.example.examplemod.OperatingSystem.UnixCommands;


import com.example.examplemod.OperatingSystem.CommandContext;

import java.nio.charset.StandardCharsets;

public class CatCommand implements Command {

    @Override
    public String getName() { return "cat"; }

    @Override
    public String getDescription() { return "Display the contents of a file"; }

    @Override
    public void execute(CommandContext context, String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: cat <path>");
            return;
        }

        String path = context.getFullPath(args[0]);

        context.getFileSystem().cat(path).ifPresentOrElse(
                bytes -> System.out.println(new String(bytes, StandardCharsets.UTF_8)),
                () -> System.out.println("  File not found: " + path)
        );
    }
}