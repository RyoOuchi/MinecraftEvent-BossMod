package com.example.examplemod.OperatingSystem.UnixCommands;


import com.example.examplemod.OperatingSystem.CommandContext;

public class MkdirCommand implements Command {
    @Override
    public String getName() { return "mkdir"; }
    @Override
    public String getDescription() { return "Create a directory"; }

    @Override
    public void execute(CommandContext context, String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: mkdir <path>");
            return;
        }
        context.getFileSystem().mkdir(context.getFullPath(args[0]));
    }
}