# java网络编程学习

## 简介
用于存放学习java网络编程过程遇见的重难点笔记和相关代码

* 对运算字符串进行识别的作业 @since2020.11.26
* IO流 - 多线程 @since2020.11.26

## IO Stream

### read()  和 write()

`public abstract int read(int) throws IOException`

`public abstract void write(int) throws IOException`

这两个抽象方法是由 `inputStream` 和 `outputStream` 的具体子类实现的，以`ByteArrayStream`类为例，重点讲解`read()`方法的一些特点：

```java
		/**
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an {@code int} in the range
     * {@code 0} to {@code 255}. If no byte is available
     * because the end of the stream has been reached, the value
     * {@code -1} is returned.
     * <p>
     * This {@code read} method
     * cannot block.
     *
     * @return  the next byte of data, or {@code -1} if the end of the
     *          stream has been reached.
     */
    public synchronized int read() {
        return (pos < count) ? (buf[pos++] & 0xff) : -1;
    }

		/**
     * Writes the specified byte to this {@code ByteArrayOutputStream}.
     *
     * @param   b   the byte to be written.
     */
    public synchronized void write(int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }
```

`read()`虽然只读取1字节，但是会返回一个int，在存放到字节数组的时候要进行类型转换，**这会产生一个-128到127之间的有符号整数**，要特别注意int类型转换成byte类型会忽略掉高位，因此在写时最好使用0-255之间的int类型数据，否则就会出现这种情况(只保留了低8位)：

```java
    @Test
    public void testReader(){
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(16)){
            out.write('*');
            out.flush();
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            int read = in.read();
            System.out.println(read);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    -------------------------------------------------------
     T E S T S
    -------------------------------------------------------
    Running 
    1
```

在平时我们往往是使用`byte[]`来读写数据的，如等待读取网络流：

```java
	int bytesRead = 0;
	int bytesToRead = 1024;
	byte[] input = new byte[bytesToRead];
	while (bytesRead<bytesToRead){
    int result = in.read(input,bytesRead,bytesToRead - bytesRead);
    if (result == -1) break;
    bytesRead += result;
  }
```

当我们尝试读取暂时未满的缓冲区时，往往会返回0，必须要等待所需的全部字节都可用才会返回长度，这往往比读取单字节方法好，因为这种情况下单字节方法会阻塞线程。如果要以非阻塞的方式获取缓存区的数据，可用`available()`方法来返回最小可读的数据长度，在流的最后该方法返回的是0，而`read()`方法在流结束的时候返回-1，但**如果参数中的length是0，则不会注意到流的结束，而继续返回0**

### 过滤器

过滤器通过构造器串联到一起，但在多数情况下了不违背过滤器链间的隐形规范，我们应**当只对链中最后一个过滤器进行读写操作**：

```java
    @Test
    public void testFlitter() {
      	//	只对最后一个进行操作
        FileInputStream fileInputStream = new FileInputStream("data.txt");
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				//	使用超类来实现永远只操作最后一个过滤器
        InputStream in = new FileInputStream("data.txt");
        in = new BufferedInputStream(in);
				//	使用超类中不存在的特定方法
        DataOutputStream dout = new DataOutputStream(
                                new BufferedOutputStream(
                                new FileOutputStream("data.txt")));
      
      	//	如果要使用链中多个过滤器的方法则要保证只对最后一个过滤器进行读写
    }
```



## 线程 - 多线程编程

### Future、Callable、Executor

创建一个==ExecutorService==，它会根据需要创建线程，可以将其理解为一个线程池，可以向它提交Callable任务，每个任务都会得到一个Future，之后可以先Future请求得到任务的结果，如果结果未就绪轮询的线程会**阻塞**，直到任务完成。

```java
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

@Test
public void doTest(){
  	//	array={0,1,2,3,4,5,6,7,8,9}
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

  -------------------------------------------------------
   T E S T S
  -------------------------------------------------------
  Running Thread.TestCallable
  9
```

在最终结合两个`Future`的比较结果时，`submit1.get()` 和 `sb2~` 都会阻塞等待结果，只有两个线程都结束时才会比较他们的结果，并返回最大值。

使用线程池的时候，使用`submit()`就可以由`Executor`去选择线程池中某一空闲的线程来调用`Runnable`接口中的`run()`，一旦能够明确确定所有任务都已进入线程中，不需再使用线程池，就应当使用`Executors.shutdown()`来显示告知线程池关闭连接，这个操作不会中止等待中的工作，可以在还有工作要完成的情况下发生，不过==应当注意在网络程序中很小这样关闭线程池，因为无法确知终点。==

```java
    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task the task to submit
     * @param result the result to return
     * @param <T> the type of the result
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<?> submit(Runnable task);
```



### 线程调度

一个线程有10种方式可以暂停或者指示它准备暂停：

* 可以对IO阻塞
* 可以对同步对象阻塞
* **可以放弃**，放弃表示线程原因暂停，让其他有相同优先级的线程有机会运行，但不会释放占有的锁，线程在放弃的时候一般不做任何的同步，在没有你要放弃的情况下，使用放弃效果不明显
* **可以休眠**，休眠是更有力的放弃，不管有没有其他线程准备运行，休眠线程都会暂停，这样可以有效的减少低优先级线程的“饥饿”，但其和放弃一样，不会释放占用的锁，要避免在同步方法或者块内让线程休眠。唤醒是通过调用休眠线程的`interrupt()`实现的，一个线程被休眠，其Thread对象还是可以得到处理（即可以调用对象的方法和字段）。休眠进程在唤醒后会得到一个异常，随后就转入到catch块中执行
* 可以连接另一个线程，连接线程等待被连接线程的结束，被连接线程即调用了`join()`的线程，**连接线程通常是隐式地作为当前线程存在的，没有作为参数传递给**`join()`，连接到另一个线程的线程可以被中断，如果线程被中断会跳过等待连接完成。
* **可以等待一个对象**，通常会配合在**等待对象**上使用`notify()` `notifyall()`方法来通知**与该对象有关的线程等待结束**，因为可能有多个线程等待同一对象，在通知前一定要得到该对象的锁。一旦线程等待通知就会尝试获得该对象的锁，否则会阻塞。==一般要将`wait()`放到检查当前对象状态的循环中，不能因为线程得到了通知就认为对象一定处在正确的情况下。==
  *有三种情况会终止`wait()`引起的睡眠：*
  * *时间到期*
  * *线程被中断*
  * *对象得到通知*
* 可以结束
* 可以被更高优先级的线程抢占
* 可以挂起
* 可以停止

最后两种已被舍弃，因为这会可能会让对象处于不一致状态





