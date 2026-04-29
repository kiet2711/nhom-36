package com.auction.network;

public class Response {
    private String status;  // "OK" | "ERROR"
    private String data;

    public Response() {}
    public Response(String status, String data) {
        this.status = status;
        this.data = data;
    }

    public static Response ok(String data)    { return new Response("OK", data); }
    public static Response error(String msg)  { return new Response("ERROR", msg); }

    public String getStatus() { return status; }
    public String getData()   { return data; }
    public boolean isOk()     { return "OK".equals(status); }
}