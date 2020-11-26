package com.xjosiah.IOStream;

import org.junit.Test;

import java.io.*;

public class TestIOOpera {
//    @Test
//    public void testFlitter() {
//        FileInputStream fileInputStream = new FileInputStream("data.txt");
//        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
//
//        InputStream in = new FileInputStream("data.txt");
//        in = new BufferedInputStream(in);
//
//        DataOutputStream dout = new DataOutputStream(
//                                new BufferedOutputStream(
//                                new FileOutputStream("data.txt")));
//
//    }

    @Test
    public void testReader() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(16)) {
            out.write(257);
            out.flush();
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            int read = in.read();
            System.out.println(read);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
