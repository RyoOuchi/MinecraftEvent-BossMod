package com.example.examplemod.OperatingSystem.UnixCommands;


import com.example.examplemod.OperatingSystem.CommandContext;

public class EchoCommand implements Command {
    @Override
    public String getName() { return "echo"; }

    @Override
    public String getDescription() {
        return "Write text to a file (echo <text> > <file>)";
    }

    @Override
    public void execute(CommandContext context, String[] args) {
        // Combine the arguments back into one string
        String line = String.join(" ", args);
        String[] split = line.split(">");
        if (split.length != 2) {
            System.out.println("Usage: echo <text> > <filename>");
            return;
        }

        String textPart = split[0].trim();
        String filePath = split[1].trim();

        // ✅ Use echo() instead of createFile()
        context.getFileSystem().echo(context.getFullPath(filePath), textPart);
    }
}
