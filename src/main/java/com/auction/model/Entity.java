package com.auction.model;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    protected String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}