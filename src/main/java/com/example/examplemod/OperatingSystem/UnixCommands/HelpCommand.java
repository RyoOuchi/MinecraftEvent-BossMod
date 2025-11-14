package com.example.examplemod.OperatingSystem.UnixCommands;


import com.example.examplemod.OperatingSystem.CommandContext;

import java.util.Map;

public class HelpCommand implements Command {
    private final Map<String, Command> registry;

    public HelpCommand(Map<String, Command> registry) {
        this.registry = registry;
    }

    @Override
    public String getName() { return "help"; }
    @Override
    public String getDescription() { return "Show this help menu"; }

    @Override
    public void execute(CommandContext context, String[] args) {
        System.out.println("🧭 Available Commands:");
        for (Command cmd : registry.values()) {
            System.out.printf("  %-8s - %s%n", cmd.getName(), cmd.getDescription());
        }
        System.out.println("  exit     - Quit shell");
    }
}