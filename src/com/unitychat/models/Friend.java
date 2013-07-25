package com.unitychat.models;

public class Friend {

    public enum Status {
        ACTIVE,
        INGAME,
        CHATTING,
        INACTIVE;
    }

    private String id, tag;
    private Status status;

    public Friend(String id, String tag, Status status) {
        this.id = id;
        this.tag = tag;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
