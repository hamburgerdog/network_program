# java网络编程学习

## 简介
用于存放学习java网络编程过程遇见的重难点笔记和相关代码

* 对运算字符串进行识别的作业 @since2020.11.26
* IO流 - 多线程 @since2020.11.26
* internet地址 @since2020.11.27
* URL 和 URI 类 @since2020.11.28
* HTTP @since 2020.11.28
* URLConnection @since 2020.11.29

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

## internet地址

### 按IP地址查找

调用`getByname()`并提供一个IP地址串做参数会创建一个internet地址对象，当实际上可能并不存在这样的主机，因为只有在`getHostName()`显式请求主机名的时候才会进行DNS检查，如果请求主机名并最终完成了一个DNS查找，但是指定IP地址的主机无法找到，那主机名也会保持为最初的字符串（即点分四段字符串），主机名比IP地址稳定很多，**从主机名创建一个新的InetAddress对象被认为是不安全的，因为这需要进行DNS查找。**

**Object方法**：

* `public boolean equals(Object o)`判断的时候只会对IP地址进行分析，主机名不会被解析，即意味则相同IP地址的两台机器会被认为是相等的
* `public int hashCode()`和`equals()`一致，只会生成IP地址的哈希值
* `public String toString()`主机名加IP地址

### 多线程处理服务器日志的案例

```java
/**
 *	处理日志的线程
 */
public class LookupTask implements Callable<String> {
  
  private String line;  
  public LookupTask(String line) {
    this.line = line;
  }
  
  @Override
  public String call() {
    try {
      // separate out the IP address
      int index = line.indexOf(' ');
      String address = line.substring(0, index);
      String theRest = line.substring(index);
      String hostname = InetAddress.getByName(address).getHostName();
      return hostname + " " + theRest;
    } catch (Exception ex) {
      return line;
    }
  }
}

/**
 *	主线程
 */
public class PooledWeblog {

  private final static int NUM_THREADS = 4;	//	线程池的大小
  
  public static void main(String[] args) throws IOException {
    ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    Queue<LogEntry> results = new LinkedList<LogEntry>();
    
    try (BufferedReader in = new BufferedReader(
      new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));) {
      for (String entry = in.readLine(); entry != null; entry = in.readLine()) {
        LookupTask task = new LookupTask(entry);
        Future<String> future = executor.submit(task);
        LogEntry result = new LogEntry(entry, future);
        results.add(result);
      }
    } 
    // Start printing the results. This blocks each time a result isn't ready.阻塞！
    for (LogEntry result : results) {
      try {
        System.out.println(result.future.get());
      } catch (InterruptedException | ExecutionException ex) {
        System.out.println(result.original);
      }
    }
    executor.shutdown();
  }
  
  private static class LogEntry {
    String original;
    Future<String> future;
    
    LogEntry(String original, Future<String> future) {
     this.original = original;
     this.future = future;
    }
  }
}
```

> 日志文件过于庞大的时候，这个程序会占用很大的内存，为了避免这个问题可以将输出也放到一个单独的线程之中，与输入共享一个队列，这样可以避免队列膨胀，但**需要有个信号告知输出线程可以运行了**

## URL  和 URI

### URL



**URL由一下5个部分组成：**

* 协议
* 授权机构
* 路径
* 片段标识符，ref（锚点）
* 查询字符串

**有四个构造器方法：**

1. String url 由字符串形式的绝对URL作为唯一参数直接生成一个URL，如果构造不成功，说明不支持这个协议，==除了能验证协议外，JAVA不会对构造的URL完成任何正确性检查==
2. String protocol， String hostname，String file 使用该协议的默认端口号，**file参数必须加 / 开头**
3. String protocol， String hostname，int port，String file 指定端口号
4.  URL base，String relative 基于父URL生成相对URL

**从URL获取数据：**

```java
//	四个方法都会抛出IOException

//	缺点：默认获取的数据都是URL引用的原始内容，
public InputStream openStream();

//	可以和服务器直接通信，访问服务器发送的所有数据和协议的元数据:即可以访问首部信息
public URLConnection openConnection();

//	指定代理服务器
public URLConnection openConnection(Proxy proxy);

//	从服务器获取的数据首部中找Content-type字段来获取对象
public Object getContent();

//	解决getContent()难以预测获得哪种对象的问题
public Object getContent(Class[] clazz);
```

**URL间相等性与比较**：

`equals()`方法在处理主机名的时候会尝试用DNS解析，只有两个URL都指向同一个主机、端口和路径上的相同资源，而且有相同的片段标识符合查询字符串时，才认为URL是相等的，`hashCode()`也同理。为了具体比较URL标识的资源可以使用`sameFile()`，这个方法可以检查两个URL是否指向相同的资源（也包括DNS查询）。

> **警告⚠️**：URL上的`equals()`可能是一个阻塞IO的操作，应当避免将URL存放到依赖该方法的结构中，如`java.util.HashMap`更好的选择方式是`java.net.URI`

 `URL.toString()`生成一个绝对URL字符串，使用`toExternalForm()`打印信息更合适，该方法将一个URL对象转换为一个字符串，事实上`toString()`调用的就是该方法，最后使用`toURI()`可以将URL转换成URI，URI类提供了更精确、更符合规范的操作行为，**URL类应当主要用于从服务器中下载内容。**

### URI

URL对象对应网络获取应用层协议的一个表示，而URI对象纯粹用于解析和处理字符串，URI没有网络获取功能。URI从字符串中构造，其**并不依赖底层协议处理器**，只要URI语法上正确，Java就不需要理解与URI相关的协议。

URI引用最多有三个部分：模式、模式特定部分和片段标识符，与URI规范不同，URI可以使用非ASCII字符，URI类中非ASCII字符不会想完成百分号的转义，这样在`getRawFoo()`这类获取URI原始编码部分的方法中，也不会得到用百分号转义后的字符，同时URI对象是不可变的，这对线程安全有帮助。

**比较和相等：**相等的URI必须同为层次或者不透明的，比较模式和授权机构时不区分大小写，其余部分区分

`toString()`返回URI的未边发字符串形式，无法保证这是一个语法正确的URI，这种方法适合人阅读但不适合用来获取数据，`toASCIISting()`返回URI的编码字符串，大多数时候都应该使用这种URI字符串形式。

### x-www-form-urlencoded

不同操作系统之间是有区别 的，web设计人员要处理这种差异，如有些操作系统允许文件名中有空格，但大部分都不允许，为了解决这类问题必须把URL使用的字符规定为必须来自ASCII的一个固定子集：

* `[a-zA-z0-9-_.!~*‘(,)]`

* 字符 / & ? @ ; $ + = % 也可以使用，但只能用于特殊用途，如果在路径或查询字符串中都应该被编码

URL类不会自动编码和解码，因此需要使用`URLEncode`和`URLDecode`这两个类来编码解码，编码方式很简单，字符转换为字节，每个字节要写为百分号后面加两个十六进制数字，URL更部分之间的分隔符不需要编码

在编码的时候必须逐个部分对URL进行编码，而不是对整个URL进行编码，通常只有路径和查询字符串需要被编码，解码是可以传入整个URL，因为解码方法对非转义字符不会进行处理

```java
public class QueryString {

  private StringBuilder query = new StringBuilder();
  
  public QueryString() {
  }

  public synchronized void add(String name, String value) { 
    query.append('&');
    encode(name, value);
  }
  
  private synchronized void encode(String name, String value) {
    try { 
      query.append(URLEncoder.encode(name, "UTF-8"));
      query.append('='); 
      query.append(URLEncoder.encode(value, "UTF-8"));
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException("Broken VM does not support UTF-8");
    }
  }
  
  public synchronized String getQuery() {
    return query.toString();
  }
  
  @Override
  public String toString() {
    return getQuery();
  }
  
  public static void main(String[] args) {
    QueryString qs = new QueryString();
    qs.add("h1", "en");
    qs.add("as_q", "Java");
    qs.add("as_epq", "I/O");
    String url = "http://www.test.com/search?" + qs;
    System.out.println(url);
  }
}
```

## HTTP

### HTTP方法

* GET，获取一个资源表示，没有副作用，如果失败可以重复执行GET
* POST，提交表单，先服务器提交数据，要防止重复提交
* HEAD，和GET方法相似，但不会获得请求的主体，只需要起始行和请求HTTP首部
* PUT，和GET相似无副作用，可以重复该方法把同一个文档放在同一个服务器的同一个位置
* DELETE，有权限安全问题
* OPTION
* TRACE

HTTP首部中两个比较关键面字段：`Content-type`指明MIME媒体类型，`Content-length`指明主体的长度，这对传送一个二进制类型的文件而言很重要

### Cookie

一种用于存储连接件持久客户端状态的小文本串，cookie在请求和响应的HTTP首部，从服务器传给客户端，再从客户端传到服务器，cookie中通常不包含数据，只是指示服务器上的数据，用`CookeiManager`和`CookieStore`来管理和在本地存放和获取cookie

## URLConnection

> **URLConnection和HTTP联系过于紧密**，默认每个传输文件前都有一个MIME首部或类型的东西

URLConnection是一个抽象类，在运行时环境使用`java.lang.Class.forName().newInstance` (java7未过时的方法 )来实例化这个类，不过该抽象类中只有一个`connect()`方法需要具体的子类实现，它建立与服务器的连接，需要依赖具体的协议，直接使用URLConnection类的程序遵循以下步骤：

1. 构造一个URL对象
2. 调用URL对象的`openConnection()`获取一个对应的URLConnection
3. 配置URLConnection
4. 读取首部字段
5. 获取输入流
6. 获得输出流
7. 关闭连接

**读数据的方法**`public int getContentLength()`：

HTTP服务器不总会在数据发送完后就立即关闭联系，因此在读取数据的时候不知道是何时停止读取的，要下载一个二进制文件，更可靠的方式是想获取一个文件的长度，在根据这个长度读取相应的字节数，很多服务器不会费力的为文本文件提供`content-length`首部，但是对二进制文件来说这个首部是必须的

### 配置连接

7个保护的实例字段

[![D66YFA.png](https://s3.ax1x.com/2020/11/29/D66YFA.png)](https://imgchr.com/i/D66YFA)

有对应设置和获取方法，==只能在URLConnection连接前修改这些实例字段，即实例方法必须在连接前使用==

### 缓存

使用GET和HTTP访问的页面通常可以缓存，HTTPS和POST的通常不缓存，客户端在请求资源的时候会询问服务器资源是否有被更新过，即如果资源的最后修改时间比客户端上一次获取资源的时间要晚则说明资源需要重新更新，或者资源缓存时间到期也需要更新。`Etag`首部就是资源改变时这个资源的唯一标识符

### 流模式

通常填写`content-length`需要知道主体的长度，而在写首部的时候往往不知道该值，因此JAVA会先把资源缓存，直到流关闭才可以知道该值，但当处理很长的表单时，响应的负担会很大，Java是这样解决的：

1. 预先知道数据的大小，如使用PUT上传文件时可以告诉HttpURLConnection对象文件大小
2. 分块，请求主体以多个部分发送，这样需要再连接URL之前将分块大小告知连接

这两种方式对身份认证和重定向有一定的影响，除非确实有必要，否则不要使用流模式

