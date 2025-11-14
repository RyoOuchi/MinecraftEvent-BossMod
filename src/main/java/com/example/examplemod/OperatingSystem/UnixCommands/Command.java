package com.example.examplemod.OperatingSystem.UnixCommands;


import com.example.examplemod.OperatingSystem.CommandContext;

public interface Command {
    String getName();
    String getDescription();
    void execute(CommandContext context, String[] args);
}
