package com.example.examplemod.OperatingSystem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class FileSystem {
    private final DirectoryNode root = new DirectoryNode("/");

    public boolean mkdir(String path) {
        DirectoryNode dir = navigateToParent(path);
        if (dir == null) return false;

        String name = getNameFromPath(path);
        if (dir.getChild(name) != null) return false;

        dir.addChild(new DirectoryNode(name));
        System.out.println("    Created directory: " + path);
        return true;
    }

    public boolean createFile(String path, byte[] content) {
        DirectoryNode dir = navigateToParent(path);
        if (dir == null) return false;

        String name = getNameFromPath(path);
        dir.addChild(new FileNode(name, content));
        System.out.println("    Created file: " + path);
        return true;
    }

    public Optional<byte[]> cat(String path) {
        FileNode file = getFile(path);
        return file != null ? Optional.of(file.getContent()) : Optional.empty();
    }

    public boolean rm(String path, boolean recursive) {
        DirectoryNode parent = navigateToParent(path);
        if (parent == null) {
            System.out.println("    Parent directory not found for " + path);
            return false;
        }

        String name = getNameFromPath(path);
        FileSystemNode node = parent.getChild(name);
        if (node == null) {
            System.out.println("    No such file or directory: " + path);
            return false;
        }

        if (node.isDirectory()) {
            DirectoryNode dir = (DirectoryNode) node;
            if (!dir.getChildren().isEmpty() && !recursive) {
                System.out.println("    Directory not empty: " + path + " (use recursive mode)");
                return false;
            }
            if (recursive) {
                deleteRecursively(dir);
            }
        }

        parent.removeChild(name);
        System.out.println("    Removed: " + path);
        return true;
    }

    private void deleteRecursively(DirectoryNode dir) {
        for (FileSystemNode child : dir.getChildren().values().toArray(new FileSystemNode[0])) {
            if (child.isDirectory()) {
                deleteRecursively((DirectoryNode) child);
            }
            dir.removeChild(child.getName());
            System.out.println("    Deleted: " + child.getFullPath());
        }
    }

    public void ls(String path) {
        DirectoryNode dir = navigateToDirectory(path);
        if (dir == null) {
            System.out.println("    Directory not found: " + path);
            return;
        }

        System.out.println("    Listing " + path + ":");
        for (Map.Entry<String, FileSystemNode> entry : dir.getChildren().entrySet()) {
            System.out.println("   " + (entry.getValue().isDirectory() ? "[DIR] " : "[FILE] ") + entry.getKey());
        }
    }

    private DirectoryNode navigateToParent(String fullPath) {
        String[] parts = fullPath.split("/");
        DirectoryNode current = root;
        for (int i = 1; i < parts.length - 1; i++) {
            FileSystemNode node = current.getChild(parts[i]);
            if (node == null || !node.isDirectory()) return null;
            current = (DirectoryNode) node;
        }
        return current;
    }

    public DirectoryNode navigateToDirectory(String fullPath) {
        if (fullPath.equals("/")) return root;
        String[] parts = fullPath.split("/");
        DirectoryNode current = root;
        for (int i = 1; i < parts.length; i++) {
            FileSystemNode node = current.getChild(parts[i]);
            if (node == null || !node.isDirectory()) return null;
            current = (DirectoryNode) node;
        }
        return current;
    }

    private FileNode getFile(String fullPath) {
        DirectoryNode parent = navigateToParent(fullPath);
        if (parent == null) return null;
        FileSystemNode node = parent.getChild(getNameFromPath(fullPath));
        return (node instanceof FileNode) ? (FileNode) node : null;
    }

    private String getNameFromPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash == -1) ? path : path.substring(lastSlash + 1);
    }

    public void echo(String path, String text) {
        createFile(path, text.getBytes(StandardCharsets.UTF_8));
    }

    public DirectoryNode getRoot() {
        return root;
    }

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Root", saveDirectory(root));
        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        root.getChildren().clear();
        if (tag.contains("Root")) {
            CompoundTag rootTag = tag.getCompound("Root");
            loadDirectory(root, rootTag);
        }
    }

    private CompoundTag saveDirectory(DirectoryNode dir) {
        CompoundTag dirTag = new CompoundTag();
        for (FileSystemNode node : dir.getChildren().values()) {
            if (node.isDirectory()) {
                dirTag.put(node.getName(), saveDirectory((DirectoryNode) node));
            } else {
                dirTag.putByteArray(node.getName(), ((FileNode) node).getContent());
            }
        }
        return dirTag;
    }

    private void loadDirectory(DirectoryNode dir, CompoundTag tag) {
        for (String key : tag.getAllKeys()) {
            Tag childTag = tag.get(key);
            if (childTag instanceof CompoundTag childDirTag) {
                DirectoryNode subDir = new DirectoryNode(key);
                dir.addChild(subDir);
                loadDirectory(subDir, childDirTag);
            } else if (childTag instanceof net.minecraft.nbt.ByteArrayTag byteArrayTag) {
                dir.addChild(new FileNode(key, byteArrayTag.getAsByteArray()));
            }
        }
    }
}

