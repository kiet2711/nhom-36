package com.auction.network;

public class Request {
    private CommandType command;
    private String token;   // username sau khi login
    private String data;    // JSON payload

    public Request() {}
    public Request(CommandType command, String data) {
        this.command = command;
        this.data = data;
    }

    public CommandType getCommand() { return command; }
    public String getToken()        { return token; }
    public String getData()         { return data; }
    public void setToken(String t)  { this.token = t; }
}