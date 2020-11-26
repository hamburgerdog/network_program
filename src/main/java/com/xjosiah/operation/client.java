package com.xjosiah.operation;

import javax.net.ServerSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import static java.lang.Thread.sleep;

public class client {
    public static void main(String[] args) throws IOException, InterruptedException {
//        String question = "((1+1.231)*2-1)/123.123-10+293*680";
//        String question = "1*6+2/(2+(3-3))+4+5*2/2";
        String question = "4/4-1+(1/(1*2)+2*(2-1))";
//        String question = "(4/4-1+(1/(1*2)+2*(2-1)+(1023/1023-(1002*1002)/(1002*1002)))";

        int port = 3000;
        String inetAddress = "127.0.0.1";
        Socket socket = new Socket(inetAddress, port);

        PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        printWriter.write(question);
        printWriter.flush();
        socket.shutdownOutput();

        String read = reader.readLine();
        System.out.println(read);

        printWriter.close();
        reader.close();
        socket.close();
    }
}
