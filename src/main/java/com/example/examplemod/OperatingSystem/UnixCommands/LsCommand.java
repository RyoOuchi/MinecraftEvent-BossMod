package com.example.examplemod.OperatingSystem.UnixCommands;

import com.example.examplemod.OperatingSystem.CommandContext;

public class LsCommand implements Command {
    @Override
    public String getName() { return "ls"; }
    @Override
    public String getDescription() { return "List files and directories"; }

    @Override
    public void execute(CommandContext context, String[] args) {
        String target = args.length > 0 ? args[0] : context.getCurrentDirectory().getFullPath();
        context.getFileSystem().ls(context.getFullPath(target));
    }
}