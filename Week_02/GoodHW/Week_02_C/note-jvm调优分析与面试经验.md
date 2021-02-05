## 1 GC日志解读与分析

###  GCLogAnalysis.java 

启动参数，打印gc日志：

```
java -Xloggc:gc.demo.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps GCLogAnalysis
```



```
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/*
演示GC日志生成与解读
*/
public class GCLogAnalysis {
    private static Random random = new Random();

    public static void main(String[] args) {
        // 当前毫秒时间戳
        long startMillis = System.currentTimeMillis();
        // 持续运行毫秒数; 可根据需要进行修改   在1秒内不断生成对象
        long timeoutMillis = TimeUnit.SECONDS.toMillis(1);
        // 结束时间戳
        long endMillis = startMillis + timeoutMillis;
        LongAdder counter = new LongAdder();
        System.out.println("正在执行...");
        // 缓存一部分对象; 进入老年代
        int cacheSize = 2000;
        Object[] cachedGarbage = new Object[cacheSize];
        // 在此时间范围内,持续循环
        while (System.currentTimeMillis() < endMillis) {
            // 生成垃圾对象
            Object garbage = generateGarbage(100 * 1024);
            counter.increment();
            //生成的对象有50%的几率被放入数组，也就是被gcroots引用
            int randomIndex = random.nextInt(2 * cacheSize);
            if (randomIndex < cacheSize) {
                cachedGarbage[randomIndex] = garbage;
            }
        }
        System.out.println("执行结束!共生成对象次数:" + counter.longValue());
        System.out.println(cachedGarbage[0]);
        System.out.println(cachedGarbage[1]);
        System.out.println(cachedGarbage[2]);
        System.out.println(cachedGarbage[3]);
    }

    // 生成对象
    private static Object generateGarbage(int max) {
        int randomSize = random.nextInt(max);
        int type = randomSize % 4;
        Object result = null;
        switch (type) {
            case 0:
                result = new int[randomSize];
                break;
            case 1:
                result = new byte[randomSize];
                break;
            case 2:
                result = new double[randomSize];
                break;
            default:
                StringBuilder builder = new StringBuilder();
                String randomString = "randomString-Anything";
                while (builder.length() < randomSize) {
                    builder.append(randomString);
                    builder.append(max);
                    builder.append(randomSize);
                }
                result = builder.toString();
                break;
        }
        return result;
    }
}
```



GCLogAnalysis程序分析：

​	模拟业务系统，在1s内不断生成各种类型的对象（基本类型数组和字符串类型，长度都是10w）， 生成的对象有50%的几率被放入数组（模拟一半的对象是短生命周期对象，一半长生命周期，当然数组同一位置被覆盖的对象引用也是短生命周期），也就是被gcroots引用（生成对象的局部变量表引用garbage变量始终只指向当前生成的对象，则先前生成的对象如果没有被数组放入数组或在数组的位置被覆盖，就可以被gc）。

​	由于没有指定-XX:PretenureSizeThreshold ,默认都会在eden区分配空间。预计年轻代会不断增加，发生young gc时，会把survivor区达到岁数的对象（大部分是我们随机生成的但没有被放入数组的对象）移入old区。如果

​	

---

实验开始

#### -Xmx128m -Xms128m -XX:+PrintGCDetails -XX:+PrintGCDateStamps

```
正在执行...
2021-01-21T14:56:24.985+0800: [GC (Allocation Failure) [PSYoungGen: 33280K->5102K(38400K)] 33280K->12018K(125952K), 0.0100642 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-21T14:56:25.015+0800: [GC (Allocation Failure) [PSYoungGen: 38347K->5117K(38400K)] 45263K->21489K(125952K), 0.0089902 secs] [Times: user=0.06 sys=0.03, real=0.01 secs] 
2021-01-21T14:56:25.042+0800: [GC (Allocation Failure) [PSYoungGen: 38397K->5108K(38400K)] 54769K->30514K(125952K), 0.0045955 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.062+0800: [GC (Allocation Failure) [PSYoungGen: 38355K->5107K(38400K)] 63760K->43053K(125952K), 0.0066549 secs] [Times: user=0.03 sys=0.03, real=0.01 secs] 
2021-01-21T14:56:25.077+0800: [GC (Allocation Failure) [PSYoungGen: 38356K->5111K(38400K)] 76302K->55028K(125952K), 0.0060325 secs] [Times: user=0.05 sys=0.03, real=0.01 secs] 
2021-01-21T14:56:25.092+0800: [GC (Allocation Failure) [PSYoungGen: 38391K->5116K(19968K)] 88308K->66029K(107520K), 0.0052065 secs] [Times: user=0.05 sys=0.03, real=0.00 secs] 
2021-01-21T14:56:25.101+0800: [GC (Allocation Failure) [PSYoungGen: 19930K->8765K(29184K)] 80843K->70446K(116736K), 0.0038736 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.108+0800: [GC (Allocation Failure) [PSYoungGen: 23613K->11419K(29184K)] 85294K->75652K(116736K), 0.0031727 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.114+0800: [GC (Allocation Failure) [PSYoungGen: 26188K->12535K(29184K)] 90421K->78465K(116736K), 0.0037257 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.123+0800: [GC (Allocation Failure) [PSYoungGen: 27383K->8874K(29184K)] 93313K->84349K(116736K), 0.0046865 secs] [Times: user=0.05 sys=0.03, real=0.01 secs] 
2021-01-21T14:56:25.128+0800: [Full GC (Ergonomics) [PSYoungGen: 8874K->0K(29184K)] [ParOldGen: 75474K->76352K(87552K)] 84349K->76352K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0241459 secs] [Times: user=0.06 sys=0.00, real=0.02 secs] 
2021-01-21T14:56:25.157+0800: [Full GC (Ergonomics) [PSYoungGen: 14697K->0K(29184K)] [ParOldGen: 76352K->81613K(87552K)] 91050K->81613K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0346010 secs] [Times: user=0.06 sys=0.00, real=0.03 secs] 
2021-01-21T14:56:25.196+0800: [Full GC (Ergonomics) [PSYoungGen: 14848K->714K(29184K)] [ParOldGen: 81613K->86920K(87552K)] 96461K->87634K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0326548 secs] [Times: user=0.09 sys=0.00, real=0.03 secs] 
2021-01-21T14:56:25.231+0800: [Full GC (Ergonomics) [PSYoungGen: 14440K->5525K(29184K)] [ParOldGen: 86920K->87414K(87552K)] 101361K->92939K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0304657 secs] [Times: user=0.11 sys=0.00, real=0.03 secs] 
2021-01-21T14:56:25.264+0800: [Full GC (Ergonomics) [PSYoungGen: 14826K->9393K(29184K)] [ParOldGen: 87414K->86693K(87552K)] 102240K->96086K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0134093 secs] [Times: user=0.09 sys=0.00, real=0.01 secs] 
2021-01-21T14:56:25.279+0800: [Full GC (Ergonomics) [PSYoungGen: 14788K->10216K(29184K)] [ParOldGen: 86693K->87470K(87552K)] 101482K->97686K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0194987 secs] [Times: user=0.09 sys=0.00, real=0.02 secs] 
2021-01-21T14:56:25.300+0800: [Full GC (Ergonomics) [PSYoungGen: 14758K->11302K(29184K)] [ParOldGen: 87470K->87470K(87552K)] 102229K->98772K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0027779 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.304+0800: [Full GC (Ergonomics) [PSYoungGen: 14806K->12626K(29184K)] [ParOldGen: 87470K->87089K(87552K)] 102276K->99716K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0107065 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-21T14:56:25.315+0800: [Full GC (Ergonomics) [PSYoungGen: 14291K->12663K(29184K)] [ParOldGen: 87089K->87089K(87552K)] 101381K->99753K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0025395 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.318+0800: [Full GC (Ergonomics) [PSYoungGen: 14793K->14084K(29184K)] [ParOldGen: 87089K->87062K(87552K)] 101883K->101146K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0164102 secs] [Times: user=0.09 sys=0.00, real=0.02 secs] 
2021-01-21T14:56:25.335+0800: [Full GC (Ergonomics) [PSYoungGen: 14755K->13742K(29184K)] [ParOldGen: 87062K->87432K(87552K)] 101817K->101174K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0086374 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-21T14:56:25.344+0800: [Full GC (Ergonomics) [PSYoungGen: 14786K->14543K(29184K)] [ParOldGen: 87432K->86990K(87552K)] 102219K->101533K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0215160 secs] [Times: user=0.11 sys=0.03, real=0.02 secs] 
2021-01-21T14:56:25.366+0800: [Full GC (Ergonomics) [PSYoungGen: 14627K->14399K(29184K)] [ParOldGen: 86990K->86990K(87552K)] 101618K->101389K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0086801 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-21T14:56:25.375+0800: [Full GC (Ergonomics) [PSYoungGen: 14777K->14735K(29184K)] [ParOldGen: 86990K->86990K(87552K)] 101767K->101725K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0052565 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-21T14:56:25.381+0800: [Full GC (Ergonomics) [PSYoungGen: 14807K->14771K(29184K)] [ParOldGen: 86990K->86990K(87552K)] 101797K->101761K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0034321 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.384+0800: [Full GC (Ergonomics) [PSYoungGen: 14800K->14699K(29184K)] [ParOldGen: 87422K->87278K(87552K)] 102223K->101977K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0061628 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-21T14:56:25.391+0800: [Full GC (Ergonomics) [PSYoungGen: 14845K->14845K(29184K)] [ParOldGen: 87278K->86977K(87552K)] 102124K->101823K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0167367 secs] [Times: user=0.01 sys=0.00, real=0.02 secs] 
2021-01-21T14:56:25.408+0800: [Full GC (Ergonomics) [PSYoungGen: 14848K->14845K(29184K)] [ParOldGen: 87471K->86977K(87552K)] 102319K->101823K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0034082 secs] [Times: user=0.05 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.412+0800: [Full GC (Ergonomics) [PSYoungGen: 14845K->14845K(29184K)] [ParOldGen: 87360K->86977K(87552K)] 102206K->101823K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0034429 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-21T14:56:25.416+0800: [Full GC (Ergonomics) [PSYoungGen: 14848K->14845K(29184K)] [ParOldGen: 87514K->87514K(87552K)] 102362K->102360K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0074763 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-21T14:56:25.424+0800: [Full GC (Allocation Failure) [PSYoungGen: 14845K->14845K(29184K)] [ParOldGen: 87514K->87488K(87552K)] 102360K->102334K(116736K), [Metaspace: 3505K->3505K(1056768K)], 0.0389934 secs] [Times: user=0.13 sys=0.00, real=0.04 secs] 
2021-01-21T14:56:25.465+0800: [Full GC (Ergonomics) [PSYoungGen: 14848K->0K(29184K)] [ParOldGen: 87503K->631K(87552K)] 102351K->631K(116736K), [Metaspace: 3530K->3530K(1056768K)], 0.0088513 secs] [Times: user=0.05 sys=0.00, real=0.01 secs] 
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	at GCLogAnalysis.generateGarbage(GCLogAnalysis.java:49)
	at GCLogAnalysis.main(GCLogAnalysis.java:26)
Heap
 PSYoungGen      total 29184K, used 307K [0x00000000fd580000, 0x0000000100000000, 0x0000000100000000)
  eden space 14848K, 2% used [0x00000000fd580000,0x00000000fd5ccde8,0x00000000fe400000)
  from space 14336K, 0% used [0x00000000ff200000,0x00000000ff200000,0x0000000100000000)
  to   space 14336K, 0% used [0x00000000fe400000,0x00000000fe400000,0x00000000ff200000)
 ParOldGen       total 87552K, used 631K [0x00000000f8000000, 0x00000000fd580000, 0x00000000fd580000)
  object space 87552K, 0% used [0x00000000f8000000,0x00000000f809dd10,0x00000000fd580000)
 Metaspace       used 3536K, capacity 4502K, committed 4864K, reserved 1056768K
  class space    used 388K, capacity 390K, committed 512K, reserved 1048576K

Process finished with exit code 1

```

日志解读：

```
2021-01-21T14:56:24.985+0800: [GC (Allocation Failure) [PSYoungGen: 33280K->5102K(38400K)] 33280K->12018K(125952K), 0.0100642 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
```

**[GC (Allocation Failure)**：GC发生了原因是分配内存失败

**[PSYoungGen: 33280K->5102K(38400K)] 33280K->12018K(125952K), 0.0100642 secs]**：发生了younggc,使用的是并行垃圾收集器，young区内存回收结果和当前容量 33280K->5102K(38400K)。堆内存当前内存变化和容量。此次younggc时间 0.0100642 secs。

**[Times: user=0.02 sys=0.00, real=0.01 secs] **：用户进程的CPU占用时长、内核进程的CPU占用时长、实际运行时间。



>[user/sys/real的详细解释]: https://my.oschina.net/dabird/blog/714569
>
>简单概括：user：所有gc线程此次GC花费的总时间，sys是操作系统花费在这次GC上的CPU时间，real实际执行时间。因为user+sys对应的是多个线程的时间和，所以user+sys可能大于real。
>
>**例1：**
>
>```
>[Times: user=11.53 sys=1.38, real=1.03 secs]
>```
>
>​	在这个例子中，`user` + `sys` 时间的和比 `real` 时间要大，这主要是因为日志时间是从 JVM 中获得的，而这个 JVM 在多核的处理器上被配置了多个 GC 线程，由于多个线程并行地执行 GC，因此整个 GC 工作被这些线程共享，最终导致实际的时钟时间（real）小于总的 CPU 时间（user + sys）。
>
>**例2：**
>
>```
>[Times: user=0.09 sys=0.00, real=0.09 secs]
>```
>
>​	上面的例子中的 GC 时间是从 Serial 垃圾收集器 （串行垃圾收集器）中获得的。由于 Serial 垃圾收集器是使用单线程进行垃圾收集的，因此 `real` 时间等于 `user` 和 `sys` 时间之和。
>
>​	在做性能优化时，我们一般采用 `real` 时间来优化程序。因为最终用户只关心点击页面发出请求到页面上展示出内容所花的时间，也就是响应时间，而不关心你到底使用了多少个 GC 线程或者处理器。但并不是说 `sys` 和 `user` 两个时间不重要，当我们想通过增加 GC 线程或者 CPU 数量来减少 GC 停顿时间时，可以参考这两个时间。
>





#### G1gc日志分析

```
正在执行...
2021-01-22T14:08:39.386+0800: [GC pause (G1 Evacuation Pause) (young), 0.0068346 secs]
   [Parallel Time: 5.9 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 314.6, Avg: 316.8, Max: 320.3, Diff: 5.7]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.2, Max: 0.7, Diff: 0.7, Sum: 1.6]
      [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
         [Processed Buffers: Min: 0, Avg: 0.0, Max: 0, Diff: 0, Sum: 0]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.1, Diff: 0.1, Sum: 0.1]
      [Object Copy (ms): Min: 0.0, Avg: 3.2, Max: 5.4, Diff: 5.4, Sum: 25.5]
      [Termination (ms): Min: 0.0, Avg: 0.1, Max: 0.3, Diff: 0.3, Sum: 0.9]
         [Termination Attempts: Min: 1, Avg: 1.1, Max: 2, Diff: 1, Sum: 9]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.1, Diff: 0.0, Sum: 0.4]
      [GC Worker Total (ms): Min: 0.1, Avg: 3.6, Max: 5.7, Diff: 5.7, Sum: 28.5]
      [GC Worker End (ms): Min: 320.3, Avg: 320.3, Max: 320.3, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 0.8 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.1 ms]
      [Humongous Register: 0.1 ms]
      [Humongous Reclaim: 0.0 ms]
      [Free CSet: 0.0 ms]
   [Eden: 51.0M(51.0M)->0.0B(44.0M) Survivors: 0.0B->7168.0K Heap: 62.4M(1024.0M)->21.5M(1024.0M)]
 [Times: user=0.05 sys=0.05, real=0.01 secs] 
2021-01-22T14:08:39.413+0800: [GC pause (G1 Evacuation Pause) (young), 0.0064011 secs]
   [Parallel Time: 5.6 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 342.0, Avg: 342.8, Max: 343.9, Diff: 1.9]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.4, Diff: 0.3, Sum: 0.9]
      [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
         [Processed Buffers: Min: 0, Avg: 0.4, Max: 3, Diff: 3, Sum: 3]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 3.4, Avg: 4.4, Max: 5.2, Diff: 1.8, Sum: 35.1]
      [Termination (ms): Min: 0.0, Avg: 0.2, Max: 0.3, Diff: 0.3, Sum: 1.2]
         [Termination Attempts: Min: 1, Avg: 1.0, Max: 1, Diff: 0, Sum: 8]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 3.6, Avg: 4.7, Max: 5.5, Diff: 1.9, Sum: 37.5]
      [GC Worker End (ms): Min: 347.5, Avg: 347.5, Max: 347.5, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 0.6 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.1 ms]
      [Humongous Reclaim: 0.1 ms]
      [Free CSet: 0.0 ms]
   [Eden: 44.0M(44.0M)->0.0B(44.0M) Survivors: 7168.0K->7168.0K Heap: 79.9M(1024.0M)->43.0M(1024.0M)]
 [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T14:08:39.435+0800: [GC pause (G1 Evacuation Pause) (young), 0.0062453 secs]
   [Parallel Time: 5.5 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 364.0, Avg: 364.7, Max: 368.7, Diff: 4.7]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 1.0]
      [Update RS (ms): Min: 0.0, Avg: 0.1, Max: 0.4, Diff: 0.4, Sum: 1.1]
         [Processed Buffers: Min: 0, Avg: 1.8, Max: 3, Diff: 3, Sum: 14]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 0.0, Avg: 4.1, Max: 4.9, Diff: 4.9, Sum: 33.1]
      [Termination (ms): Min: 0.0, Avg: 0.3, Max: 0.6, Diff: 0.6, Sum: 2.4]
         [Termination Attempts: Min: 1, Avg: 1.0, Max: 1, Diff: 0, Sum: 8]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 0.6, Avg: 4.7, Max: 5.4, Diff: 4.8, Sum: 37.6]
      [GC Worker End (ms): Min: 369.3, Avg: 369.4, Max: 369.4, Diff: 0.1]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 0.6 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.1 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.1 ms]
      [Humongous Register: 0.0 ms]
      [Humongous Reclaim: 0.0 ms]
      [Free CSet: 0.0 ms]
   [Eden: 44.0M(44.0M)->0.0B(61.0M) Survivors: 7168.0K->7168.0K Heap: 94.4M(1024.0M)->58.2M(1024.0M)]
 [Times: user=0.03 sys=0.06, real=0.01 secs] 
2021-01-22T14:08:39.470+0800: [GC pause (G1 Evacuation Pause) (young), 0.0084821 secs]
   [Parallel Time: 7.5 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 399.3, Avg: 400.8, Max: 406.2, Diff: 6.9]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.2, Max: 0.3, Diff: 0.2, Sum: 1.3]
      [Update RS (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 0.8]
         [Processed Buffers: Min: 0, Avg: 1.5, Max: 3, Diff: 3, Sum: 12]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 0.2, Avg: 5.4, Max: 6.8, Diff: 6.6, Sum: 42.8]
      [Termination (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 1.1]
         [Termination Attempts: Min: 1, Avg: 1.0, Max: 1, Diff: 0, Sum: 8]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.1, Diff: 0.1, Sum: 0.2]
      [GC Worker Total (ms): Min: 0.4, Avg: 5.8, Max: 7.3, Diff: 6.9, Sum: 46.3]
      [GC Worker End (ms): Min: 406.6, Avg: 406.6, Max: 406.6, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.1 ms]
   [Other: 0.9 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.1 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.1 ms]
      [Humongous Reclaim: 0.1 ms]
      [Free CSet: 0.1 ms]
   [Eden: 61.0M(61.0M)->0.0B(219.0M) Survivors: 7168.0K->9216.0K Heap: 138.8M(1024.0M)->83.3M(1024.0M)]
 [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T14:08:39.661+0800: [GC pause (G1 Evacuation Pause) (young), 0.0240811 secs]
   [Parallel Time: 22.9 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 590.8, Avg: 590.8, Max: 591.0, Diff: 0.2]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 0.8]
      [Update RS (ms): Min: 0.1, Avg: 0.2, Max: 0.2, Diff: 0.1, Sum: 1.4]
         [Processed Buffers: Min: 0, Avg: 2.0, Max: 3, Diff: 3, Sum: 16]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 22.0, Avg: 22.1, Max: 22.4, Diff: 0.4, Sum: 177.0]
      [Termination (ms): Min: 0.0, Avg: 0.3, Max: 0.4, Diff: 0.4, Sum: 2.2]
         [Termination Attempts: Min: 1, Avg: 1.3, Max: 3, Diff: 2, Sum: 10]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 22.6, Avg: 22.7, Max: 22.8, Diff: 0.2, Sum: 181.6]
      [GC Worker End (ms): Min: 613.5, Avg: 613.5, Max: 613.6, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 1.0 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.1 ms]
      [Humongous Reclaim: 0.2 ms]
      [Free CSet: 0.2 ms]
   [Eden: 219.0M(219.0M)->0.0B(75.0M) Survivors: 9216.0K->29.0M Heap: 355.4M(1024.0M)->168.3M(1024.0M)]
 [Times: user=0.05 sys=0.05, real=0.02 secs] 
2021-01-22T14:08:39.708+0800: [GC pause (G1 Evacuation Pause) (young), 0.0113994 secs]
   [Parallel Time: 10.5 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 637.6, Avg: 637.9, Max: 638.2, Diff: 0.5]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 0.9]
      [Update RS (ms): Min: 0.0, Avg: 0.2, Max: 0.3, Diff: 0.3, Sum: 1.2]
         [Processed Buffers: Min: 0, Avg: 2.1, Max: 4, Diff: 4, Sum: 17]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 9.4, Avg: 9.6, Max: 9.9, Diff: 0.4, Sum: 76.7]
      [Termination (ms): Min: 0.0, Avg: 0.3, Max: 0.5, Diff: 0.5, Sum: 2.6]
         [Termination Attempts: Min: 1, Avg: 1.0, Max: 1, Diff: 0, Sum: 8]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 9.9, Avg: 10.2, Max: 10.4, Diff: 0.6, Sum: 81.7]
      [GC Worker End (ms): Min: 648.1, Avg: 648.1, Max: 648.1, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.1 ms]
   [Other: 0.8 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.1 ms]
      [Humongous Reclaim: 0.1 ms]
      [Free CSet: 0.1 ms]
   [Eden: 75.0M(75.0M)->0.0B(148.0M) Survivors: 29.0M->13.0M Heap: 263.7M(1024.0M)->195.1M(1024.0M)]
 [Times: user=0.06 sys=0.02, real=0.01 secs] 
2021-01-22T14:08:39.775+0800: [GC pause (G1 Evacuation Pause) (young), 0.0140590 secs]
   [Parallel Time: 12.9 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 705.1, Avg: 705.3, Max: 705.9, Diff: 0.8]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 0.9]
      [Update RS (ms): Min: 0.0, Avg: 0.2, Max: 0.4, Diff: 0.4, Sum: 1.2]
         [Processed Buffers: Min: 0, Avg: 1.8, Max: 4, Diff: 4, Sum: 14]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 11.8, Avg: 12.2, Max: 12.4, Diff: 0.6, Sum: 97.5]
      [Termination (ms): Min: 0.0, Avg: 0.1, Max: 0.3, Diff: 0.3, Sum: 1.1]
         [Termination Attempts: Min: 1, Avg: 1.4, Max: 3, Diff: 2, Sum: 11]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 12.0, Avg: 12.6, Max: 12.8, Diff: 0.8, Sum: 100.9]
      [GC Worker End (ms): Min: 717.9, Avg: 717.9, Max: 717.9, Diff: 0.1]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 0.9 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.1 ms]
      [Humongous Reclaim: 0.2 ms]
      [Free CSet: 0.1 ms]
   [Eden: 148.0M(148.0M)->0.0B(201.0M) Survivors: 13.0M->21.0M Heap: 379.2M(1024.0M)->245.6M(1024.0M)]
 [Times: user=0.00 sys=0.09, real=0.01 secs] 
2021-01-22T14:08:39.860+0800: [GC pause (G1 Evacuation Pause) (young), 0.0220712 secs]
   [Parallel Time: 20.8 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 789.6, Avg: 789.7, Max: 789.8, Diff: 0.2]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 0.9]
      [Update RS (ms): Min: 0.2, Avg: 0.3, Max: 0.8, Diff: 0.6, Sum: 2.3]
         [Processed Buffers: Min: 0, Avg: 2.4, Max: 4, Diff: 4, Sum: 19]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.2]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 19.3, Avg: 19.8, Max: 19.9, Diff: 0.6, Sum: 158.1]
      [Termination (ms): Min: 0.0, Avg: 0.3, Max: 0.5, Diff: 0.5, Sum: 2.1]
         [Termination Attempts: Min: 1, Avg: 1.0, Max: 1, Diff: 0, Sum: 8]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 20.2, Avg: 20.5, Max: 20.7, Diff: 0.5, Sum: 163.8]
      [GC Worker End (ms): Min: 810.0, Avg: 810.2, Max: 810.4, Diff: 0.4]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 1.1 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.1 ms]
      [Humongous Register: 0.1 ms]
      [Humongous Reclaim: 0.2 ms]
      [Free CSet: 0.2 ms]
   [Eden: 201.0M(201.0M)->0.0B(247.0M) Survivors: 21.0M->28.0M Heap: 505.9M(1024.0M)->313.9M(1024.0M)]
 [Times: user=0.06 sys=0.06, real=0.02 secs] 
2021-01-22T14:08:39.992+0800: [GC pause (G1 Evacuation Pause) (young), 0.0303394 secs]
   [Parallel Time: 29.2 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 920.6, Avg: 921.6, Max: 924.2, Diff: 3.6]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 1.0]
      [Update RS (ms): Min: 0.0, Avg: 0.3, Max: 0.9, Diff: 0.9, Sum: 2.5]
         [Processed Buffers: Min: 0, Avg: 2.6, Max: 8, Diff: 8, Sum: 21]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.1, Diff: 0.1, Sum: 0.3]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 25.1, Avg: 27.3, Max: 28.1, Diff: 3.1, Sum: 218.2]
      [Termination (ms): Min: 0.0, Avg: 0.2, Max: 0.3, Diff: 0.3, Sum: 1.4]
         [Termination Attempts: Min: 1, Avg: 1.3, Max: 3, Diff: 2, Sum: 10]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.1, Max: 0.1, Diff: 0.0, Sum: 0.4]
      [GC Worker Total (ms): Min: 25.4, Avg: 28.0, Max: 29.0, Diff: 3.6, Sum: 223.8]
      [GC Worker End (ms): Min: 949.6, Avg: 949.6, Max: 949.6, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 0.9 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.1 ms]
      [Humongous Reclaim: 0.2 ms]
      [Free CSet: 0.2 ms]
   [Eden: 247.0M(247.0M)->0.0B(253.0M) Survivors: 28.0M->35.0M Heap: 630.2M(1024.0M)->398.6M(1024.0M)]
 [Times: user=0.09 sys=0.16, real=0.03 secs] 
2021-01-22T14:08:40.049+0800: [GC pause (G1 Humongous Allocation) (young) (initial-mark), 0.0133621 secs]
   [Parallel Time: 12.4 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 978.8, Avg: 978.8, Max: 979.0, Diff: 0.2]
      [Ext Root Scanning (ms): Min: 0.2, Avg: 0.3, Max: 1.0, Diff: 0.8, Sum: 2.7]
      [Update RS (ms): Min: 0.0, Avg: 0.3, Max: 0.6, Diff: 0.6, Sum: 2.0]
         [Processed Buffers: Min: 0, Avg: 2.1, Max: 4, Diff: 4, Sum: 17]
      [Scan RS (ms): Min: 0.0, Avg: 0.1, Max: 0.4, Diff: 0.4, Sum: 0.5]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 10.5, Avg: 11.3, Max: 11.7, Diff: 1.2, Sum: 90.6]
      [Termination (ms): Min: 0.0, Avg: 0.2, Max: 0.3, Diff: 0.3, Sum: 1.4]
         [Termination Attempts: Min: 1, Avg: 1.3, Max: 3, Diff: 2, Sum: 10]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 12.1, Avg: 12.2, Max: 12.2, Diff: 0.2, Sum: 97.4]
      [GC Worker End (ms): Min: 991.0, Avg: 991.0, Max: 991.0, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.1 ms]
   [Other: 0.9 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.2 ms]
      [Humongous Reclaim: 0.1 ms]
      [Free CSet: 0.1 ms]
   [Eden: 114.0M(253.0M)->0.0B(317.0M) Survivors: 35.0M->36.0M Heap: 543.9M(1024.0M)->444.9M(1024.0M)]
 [Times: user=0.00 sys=0.01, real=0.01 secs] 
2021-01-22T14:08:40.063+0800: [GC concurrent-root-region-scan-start]
2021-01-22T14:08:40.063+0800: [GC concurrent-root-region-scan-end, 0.0003271 secs]
2021-01-22T14:08:40.063+0800: [GC concurrent-mark-start]
2021-01-22T14:08:40.068+0800: [GC concurrent-mark-end, 0.0051627 secs]
2021-01-22T14:08:40.068+0800: [GC remark 2021-01-22T14:08:40.069+0800: [Finalize Marking, 0.0001462 secs] 2021-01-22T14:08:40.069+0800: [GC ref-proc, 0.0002981 secs] 2021-01-22T14:08:40.069+0800: [Unloading, 0.0007578 secs], 0.0020395 secs]
 [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T14:08:40.071+0800: [GC cleanup 459M->446M(1024M), 0.0008175 secs]
 [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T14:08:40.072+0800: [GC concurrent-cleanup-start]
2021-01-22T14:08:40.072+0800: [GC concurrent-cleanup-end, 0.0001070 secs]
2021-01-22T14:08:40.267+0800: [GC pause (G1 Evacuation Pause) (young) (to-space exhausted), 0.0274216 secs]
   [Parallel Time: 25.5 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 1197.1, Avg: 1199.1, Max: 1205.9, Diff: 8.8]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 0.9]
      [Update RS (ms): Min: 0.0, Avg: 0.3, Max: 0.8, Diff: 0.8, Sum: 2.3]
         [Processed Buffers: Min: 0, Avg: 3.1, Max: 7, Diff: 7, Sum: 25]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.1, Diff: 0.1, Sum: 0.3]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 16.3, Avg: 22.8, Max: 24.8, Diff: 8.6, Sum: 182.8]
      [Termination (ms): Min: 0.0, Avg: 0.2, Max: 0.3, Diff: 0.3, Sum: 1.6]
         [Termination Attempts: Min: 1, Avg: 1.0, Max: 1, Diff: 0, Sum: 8]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.1, Diff: 0.0, Sum: 0.2]
      [GC Worker Total (ms): Min: 16.7, Avg: 23.5, Max: 25.5, Diff: 8.8, Sum: 188.1]
      [GC Worker End (ms): Min: 1222.6, Avg: 1222.6, Max: 1222.6, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 1.7 ms]
      [Evacuation Failure: 0.4 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.2 ms]
      [Humongous Reclaim: 0.3 ms]
      [Free CSet: 0.2 ms]
   [Eden: 317.0M(317.0M)->0.0B(6144.0K) Survivors: 36.0M->45.0M Heap: 827.7M(1024.0M)->558.4M(1024.0M)]
 [Times: user=0.03 sys=0.08, real=0.03 secs] 
2021-01-22T14:08:40.298+0800: [GC pause (G1 Evacuation Pause) (mixed), 0.0146461 secs]
   [Parallel Time: 13.3 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 1227.3, Avg: 1227.8, Max: 1229.0, Diff: 1.6]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 0.9]
      [Update RS (ms): Min: 0.0, Avg: 0.2, Max: 0.3, Diff: 0.3, Sum: 1.3]
         [Processed Buffers: Min: 0, Avg: 2.6, Max: 5, Diff: 5, Sum: 21]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.1, Diff: 0.1, Sum: 0.3]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [Object Copy (ms): Min: 11.5, Avg: 12.4, Max: 12.7, Diff: 1.2, Sum: 99.2]
      [Termination (ms): Min: 0.0, Avg: 0.1, Max: 0.1, Diff: 0.1, Sum: 0.5]
         [Termination Attempts: Min: 1, Avg: 1.0, Max: 1, Diff: 0, Sum: 8]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 11.6, Avg: 12.8, Max: 13.2, Diff: 1.6, Sum: 102.3]
      [GC Worker End (ms): Min: 1240.5, Avg: 1240.6, Max: 1240.6, Diff: 0.0]
   [Code Root Fixup: 0.0 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.2 ms]
   [Other: 1.1 ms]
      [Choose CSet: 0.1 ms]
      [Ref Proc: 0.2 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Register: 0.2 ms]
      [Humongous Reclaim: 0.1 ms]
      [Free CSet: 0.2 ms]
   [Eden: 6144.0K(6144.0K)->0.0B(49.0M) Survivors: 45.0M->2048.0K Heap: 565.7M(1024.0M)->475.1M(1024.0M)]
 [Times: user=0.00 sys=0.00, real=0.01 secs] 
执行结束!共生成对象次数:7119
Heap
 garbage-first heap   total 1048576K, used 515666K [0x00000000c0000000, 0x00000000c0102000, 0x0000000100000000)
  region size 1024K, 26 young (26624K), 2 survivors (2048K)
 Metaspace       used 3511K, capacity 4500K, committed 4864K, reserved 1056768K
  class space    used 385K, capacity 388K, committed 512K, reserved 1048576K
```









### 2 JVM线程堆栈数据分析



### 3 内存分析与相关工具*



### 4 JVM 问题分析调优经验*



### 5 GC 疑难情况问题分析



### 6 JVM 常见面试问题汇总*  

