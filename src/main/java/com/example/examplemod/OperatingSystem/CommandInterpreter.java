package com.example.examplemod.OperatingSystem;


import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;
import com.example.examplemod.OperatingSystem.UnixCommands.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandInterpreter {
    private final Map<String, Command> commands = new HashMap<>();
    private final CommandContext context;

    public CommandInterpreter(FileSystem fs, ServerBlockEntity serverBlockEntity) {
        this.context = new CommandContext(fs, fs.getRoot(), serverBlockEntity);
        registerDefaults();
    }

    private void registerDefaults() {
        register(new LsCommand());
        register(new MkdirCommand());
        register(new CdCommand());
        register(new EchoCommand());
        register(new CatCommand());
        register(new TouchCommand());
        register(new RmCommand());
        register(new UploadCommand());
        register(new HelpCommand(commands));
    }

    private void register(Command cmd) {
        commands.put(cmd.getName(), cmd);
    }

    public void startInteractiveShell() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("🖥️ Virtual Unix Shell Started. Type 'help' for commands.\n");

        while (true) {
            System.out.print(context.getCurrentDirectory().getFullPath() + " $ ");
            String input = scanner.nextLine().trim();

            if (input.equals("exit")) {
                System.out.println("👋 Exiting shell.");
                break;
            }

            execute(input);
        }
    }

    public void execute(String input) {
        if (input.isEmpty()) return;

        String[] parts = input.split("\\s+");
        String cmdName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        Command command = commands.get(cmdName);
        if (command != null) {
            command.execute(context, args);
        } else {
            System.out.println("Unknown command: " + cmdName);
        }
    }

    public CommandContext getContext() {
        return context;
    }
}
