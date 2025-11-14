package com.example.examplemod.OperatingSystem;

public abstract class FileSystemNode {
    protected final String name;
    protected DirectoryNode parent;

    public FileSystemNode(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public DirectoryNode getParent() { return parent; }
    public String getFullPath() {
        if (parent == null) return "/";
        if (parent.getParent() == null) return "/" + name;
        return parent.getFullPath() + "/" + name;
    }

    public abstract boolean isDirectory();
}

