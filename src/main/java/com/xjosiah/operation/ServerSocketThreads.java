package com.xjosiah.operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ServerSocketThreads extends Thread{
    private static Socket socket;

    public ServerSocketThreads(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
            String read = reader.readLine();
            System.out.println("收到算式： \t"+read);
            ArrayList<String> mathToken = op_test.getMathToken(read);
            double result = op_test.doMath(mathToken);
            printWriter.write("算式\t"+read+" 的结果是：\t "+String.valueOf(result));
            printWriter.flush();
//            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                reader.close();
//                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
