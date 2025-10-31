package com.example.examplemod.OperatingSystem;

import java.util.HashMap;
import java.util.Map;

public class DirectoryNode extends FileSystemNode {
    private final Map<String, FileSystemNode> children = new HashMap<>();

    public DirectoryNode(String name) {
        super(name);
    }

    public Map<String, FileSystemNode> getChildren() { return children; }

    public void addChild(FileSystemNode node) {
        children.put(node.getName(), node);
        node.parent = this;
    }

    public FileSystemNode getChild(String name) {
        return children.get(name);
    }

    public void removeChild(String name) {
        children.remove(name);
    }

    @Override
    public boolean isDirectory() { return true; }
}

