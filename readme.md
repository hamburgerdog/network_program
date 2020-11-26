# java网络编程学习

## 简介
用于存放学习java网络编程过程遇见的重难点笔记和相关代码

* 对运算字符串进行识别的作业 @since2020.11.26

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

