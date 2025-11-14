package com.example.examplemod.OperatingSystem;

import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;

public class CommandContext {
    private final FileSystem fileSystem;
    private DirectoryNode currentDirectory;
    private final ServerBlockEntity serverBlockEntity;

    public CommandContext(FileSystem fs, DirectoryNode startDir, ServerBlockEntity serverBlockEntity) {
        this.fileSystem = fs;
        this.currentDirectory = startDir;
        this.serverBlockEntity = serverBlockEntity;
    }

    public FileSystem getFileSystem() { return fileSystem; }
    public DirectoryNode getCurrentDirectory() { return currentDirectory; }

    public void setCurrentDirectory(DirectoryNode dir) {
        this.currentDirectory = dir;
    }

    public String getFullPath(String input) {
        if (input.startsWith("/")) return input; // absolute path
        String currentPath = currentDirectory.getFullPath();
        if (currentPath.endsWith("/")) return currentPath + input;
        return currentPath + "/" + input;
    }

    public ServerBlockEntity getServerBlockEntity() { return serverBlockEntity; }

}