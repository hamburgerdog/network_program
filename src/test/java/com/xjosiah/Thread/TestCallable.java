package com.xjosiah.Thread;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestCallable {
    @Test
    public void doTest(){
        int array[] = new int[10];
        for (int i =0;i<array.length;i++){
            array[i]=i;
        }

        FindMaxInt findMaxInt1 = new FindMaxInt(array, 0, array.length / 2);
        FindMaxInt findMaxInt2 = new FindMaxInt(array, array.length / 2,array.length);

        //  Executor：线程池 用于通过实现Callable的类创建任务，构造一个Future来处理任务
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        Future<Integer> submit1 = threadPool.submit(findMaxInt1);
        Future<Integer> submit2 = threadPool.submit(findMaxInt2);

        try {
            System.out.println(Math.max(submit1.get(),submit2.get()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
