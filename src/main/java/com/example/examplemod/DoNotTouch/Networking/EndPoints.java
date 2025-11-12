package com.example.examplemod.DoNotTouch.Networking;

public enum EndPoints {
    SPAWNED_BOSS("/spawned-boss"),
    DEFEATED_BOSS("/defeated-boss");

    private final String path;

    EndPoints(String path) {
        this.path = path;
    }

    public String getEndPointPath() {
        return "/minecraft" + path;
    }
}
