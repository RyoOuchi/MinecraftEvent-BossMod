package com.example.examplemod.OperatingSystem.UnixCommands;


import com.example.examplemod.OperatingSystem.CommandContext;

public class RmCommand implements Command {

    @Override
    public String getName() { return "rm"; }

    @Override
    public String getDescription() {
        return "Remove a file or directory (use -r for recursive)";
    }

    @Override
    public void execute(CommandContext context, String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: rm [-r] <path>");
            return;
        }

        boolean recursive = false;
        String targetArg = args[0];

        if (args[0].equals("-r")) {
            if (args.length < 2) {
                System.out.println("Usage: rm -r <path>");
                return;
            }
            recursive = true;
            targetArg = args[1];
        }

        String path = context.getFullPath(targetArg);
        context.getFileSystem().rm(path, recursive);
    }
}
