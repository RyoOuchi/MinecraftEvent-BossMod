package com.example.examplemod.OperatingSystem;

class FileNode extends FileSystemNode {
    private byte[] content;

    public FileNode(String name, byte[] content) {
        super(name);
        this.content = content;
    }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }

    @Override
    public boolean isDirectory() { return false; }
}

