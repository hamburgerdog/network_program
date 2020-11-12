package com.xjosiah.operation;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class server {
    public static void main(String[] args) throws IOException {
        int port=3000;
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = null;
        int count = 0 ;
        System.out.println("server start !");
        while(count<5){
            socket = serverSocket.accept();
            ServerSocketThreads serverSocketThreads = new ServerSocketThreads(socket);
            serverSocketThreads.start();
            count++;
        }
    }
}
