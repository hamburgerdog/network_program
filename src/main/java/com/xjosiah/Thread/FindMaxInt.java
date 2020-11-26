package com.xjosiah.Thread;

import java.util.concurrent.Callable;

/**
 * 实现了比较大小的Callable
 */
public class FindMaxInt implements Callable<Integer> {
    private int array[];
    private int start;
    private int end;

    public FindMaxInt(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    /**
     * 回调，在Future中被get()函数所处理
     * @return  比较结果
     * @throws Exception
     */
    @Override
    public Integer call() throws Exception {
        int max = Integer.MIN_VALUE;
        for (int i=start;i<end;i++){
            max = array[i]>=max?array[i]:max;
        }
        return max;
    }
}
