package com.example.blazegraph.test;

public class Application {

    public static void main(String[] args) {
        CustomBlazegraphServer blazegraphServer = new CustomBlazegraphServer();
        try {
            blazegraphServer.start(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
