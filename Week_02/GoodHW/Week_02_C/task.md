## 第3课

### 1、 使用 GCLogAnalysis.java 自己演练一遍串行/并行/CMS/G1的案例。

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

​	由于没有指定-XX:PretenureSizeThreshold ,默认都会在eden区分配空间。预计年轻代会不断增加，发生young gc时，会把survivor区达到岁数的对象（大部分是我们随机生成的但没有被放入数组的对象）移入old区。如果old区空间达到一定值时，可能会发生fullGC。

---

实验开始

#### 1）串行 128m：

```
-Xmx128m -Xms128m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseSerialGC
```

```
正在执行...
2021-01-22T13:15:10.087+0800: [GC (Allocation Failure) 2021-01-22T13:15:10.088+0800: [DefNew: 34878K->4352K(39296K), 0.0133626 secs] 34878K->10999K(126720K), 0.0148890 secs] [Times: user=0.00 sys=0.01, real=0.02 secs] 
2021-01-22T13:15:10.121+0800: [GC (Allocation Failure) 2021-01-22T13:15:10.121+0800: [DefNew: 39296K->4343K(39296K), 0.0154960 secs] 45943K->22669K(126720K), 0.0156257 secs] [Times: user=0.03 sys=0.00, real=0.02 secs] 
2021-01-22T13:15:10.147+0800: [GC (Allocation Failure) 2021-01-22T13:15:10.147+0800: [DefNew: 39255K->4350K(39296K), 0.0121589 secs] 57582K->37312K(126720K), 0.0122311 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.170+0800: [GC (Allocation Failure) 2021-01-22T13:15:10.170+0800: [DefNew: 38625K->4350K(39296K), 0.0087956 secs] 71587K->47644K(126720K), 0.0088428 secs] [Times: user=0.03 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.188+0800: [GC (Allocation Failure) 2021-01-22T13:15:10.188+0800: [DefNew: 39294K->4347K(39296K), 0.0122334 secs] 82588K->60148K(126720K), 0.0122835 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.211+0800: [GC (Allocation Failure) 2021-01-22T13:15:10.211+0800: [DefNew: 39291K->4349K(39296K), 0.0140009 secs] 95092K->75292K(126720K), 0.0140635 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.235+0800: [GC (Allocation Failure) 2021-01-22T13:15:10.235+0800: [DefNew: 39028K->39028K(39296K), 0.0000205 secs]2021-01-22T13:15:10.235+0800: [Tenured: 70942K->83916K(87424K), 0.0339479 secs] 109970K->83916K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0345384 secs] [Times: user=0.02 sys=0.02, real=0.04 secs] 
2021-01-22T13:15:10.279+0800: [GC (Allocation Failure) 2021-01-22T13:15:10.279+0800: [DefNew: 34543K->34543K(39296K), 0.0000421 secs]2021-01-22T13:15:10.279+0800: [Tenured: 83916K->87282K(87424K), 0.0278102 secs] 118460K->92959K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0279427 secs] [Times: user=0.03 sys=0.00, real=0.03 secs] 
2021-01-22T13:15:10.317+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.317+0800: [Tenured: 87282K->87256K(87424K), 0.0282442 secs] 126489K->104172K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0283296 secs] [Times: user=0.03 sys=0.00, real=0.03 secs] 
2021-01-22T13:15:10.355+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.355+0800: [Tenured: 87256K->86875K(87424K), 0.0378175 secs] 126406K->105517K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0379273 secs] [Times: user=0.05 sys=0.00, real=0.04 secs] 
2021-01-22T13:15:10.401+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.401+0800: [Tenured: 87178K->87178K(87424K), 0.0096745 secs] 126317K->112467K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0097456 secs] [Times: user=0.01 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.415+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.415+0800: [Tenured: 87178K->87178K(87424K), 0.0044032 secs] 126424K->117146K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0044891 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.422+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.422+0800: [Tenured: 87178K->87393K(87424K), 0.0228659 secs] 125825K->119779K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0229433 secs] [Times: user=0.03 sys=0.00, real=0.02 secs] 
2021-01-22T13:15:10.448+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.448+0800: [Tenured: 87393K->87399K(87424K), 0.0369852 secs] 126601K->116689K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0370563 secs] [Times: user=0.05 sys=0.00, real=0.04 secs] 
2021-01-22T13:15:10.487+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.487+0800: [Tenured: 87399K->87399K(87424K), 0.0095334 secs] 126577K->119279K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0096165 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.499+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.499+0800: [Tenured: 87399K->87399K(87424K), 0.0031699 secs] 126646K->122300K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0032546 secs] [Times: user=0.02 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.504+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.504+0800: [Tenured: 87399K->87399K(87424K), 0.0043850 secs] 126501K->124295K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0044516 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.509+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.509+0800: [Tenured: 87399K->86841K(87424K), 0.0312076 secs] 126621K->121030K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0312559 secs] [Times: user=0.03 sys=0.00, real=0.03 secs] 
2021-01-22T13:15:10.541+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.541+0800: [Tenured: 87406K->87406K(87424K), 0.0083507 secs] 126701K->123564K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0083928 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.551+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.551+0800: [Tenured: 87406K->87406K(87424K), 0.0064825 secs] 126658K->123924K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0065309 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.558+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.558+0800: [Tenured: 87406K->87406K(87424K), 0.0039282 secs] 126458K->124516K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0039930 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.562+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.562+0800: [Tenured: 87406K->87291K(87424K), 0.0159420 secs] 126694K->123906K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0159892 secs] [Times: user=0.01 sys=0.00, real=0.02 secs] 
2021-01-22T13:15:10.579+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.579+0800: [Tenured: 87291K->87291K(87424K), 0.0019917 secs] 126552K->124344K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0020429 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.582+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.582+0800: [Tenured: 87330K->87330K(87424K), 0.0046706 secs] 126601K->125058K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0047098 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.587+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.587+0800: [Tenured: 87330K->87330K(87424K), 0.0019763 secs] 126543K->125820K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0020275 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.589+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.589+0800: [Tenured: 87402K->87337K(87424K), 0.0131789 secs] 126668K->126024K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0132318 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-22T13:15:10.603+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.603+0800: [Tenured: 87337K->87337K(87424K), 0.0020343 secs] 126477K->126184K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0020759 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.605+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.605+0800: [Tenured: 87337K->87337K(87424K), 0.0019001 secs] 126585K->126256K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0019382 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.607+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.607+0800: [Tenured: 87337K->87337K(87424K), 0.0021299 secs] 126511K->126287K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0021709 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:15:10.609+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.610+0800: [Tenured: 87337K->87418K(87424K), 0.0203571 secs] 126499K->125957K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0204180 secs] [Times: user=0.03 sys=0.00, real=0.02 secs] 
2021-01-22T13:15:10.630+0800: [Full GC (Allocation Failure) 2021-01-22T13:15:10.630+0800: [Tenured: 87418K->87392K(87424K), 0.0194025 secs] 125957K->125931K(126720K), [Metaspace: 3505K->3505K(1056768K)], 0.0194492 secs] [Times: user=0.02 sys=0.00, real=0.02 secs] 
Heap
 def new generation   total 39296K, used 38985K [0x00000000f8000000, 0x00000000faaa0000, 0x00000000faaa0000)
  eden space 34944K, 100% used [0x00000000f8000000, 0x00000000fa220000, 0x00000000fa220000)
  from space 4352K,  92% used [0x00000000fa220000, 0x00000000fa612700, 0x00000000fa660000)
  to   space 4352K,   0% used [0x00000000fa660000, 0x00000000fa660000, 0x00000000faaa0000)
 tenured generation   total 87424K, used 87392K [0x00000000faaa0000, 0x0000000100000000, 0x0000000100000000)
   the space 87424K,  99% used [0x00000000faaa0000, 0x00000000ffff8318, 0x00000000ffff8400, 0x0000000100000000)
 Metaspace       used 3536K, capacity 4502K, committed 4864K, reserved 1056768K
  class space    used 388K, capacity 390K, committed 512K, reserved 1048576K
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	at GCLogAnalysis.generateGarbage(GCLogAnalysis.java:49)
	at GCLogAnalysis.main(GCLogAnalysis.java:26)
```

日志分析：

​	

| Avg Pause GC Time | **13.5 ms** |
| :---------------- | ----------- |
| Max Pause GC Time | **40.0 ms** |
| **Throughput**    | **22.652%** |

 * 每次young区接近满的时候，都会发生youngGC.
 * 通过堆内存大小-young区大小 ，可以看出，发生当old区可用空间不足时会发生fullGC。
 * 由于对fullGC无法回收出足够空间或回收速度比young区晋升速度慢，所以内存一直不足，故一直在执行fullGC，最终由于young和old区空间不足，抛出内存溢出异常。

#### 2）串行 512m：

```
正在执行...
2021-01-22T13:39:58.948+0800: [GC (Allocation Failure) 2021-01-22T13:39:58.948+0800: [DefNew: 139776K->17472K(157248K), 0.0286305 secs] 139776K->42795K(506816K), 0.0287392 secs] [Times: user=0.02 sys=0.02, real=0.03 secs] 
2021-01-22T13:39:59.009+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.009+0800: [DefNew: 157248K->17468K(157248K), 0.0461682 secs] 182571K->90333K(506816K), 0.0462297 secs] [Times: user=0.02 sys=0.03, real=0.05 secs] 
2021-01-22T13:39:59.084+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.084+0800: [DefNew: 157244K->17469K(157248K), 0.0310619 secs] 230109K->132017K(506816K), 0.0311052 secs] [Times: user=0.03 sys=0.00, real=0.03 secs] 
2021-01-22T13:39:59.151+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.151+0800: [DefNew: 157245K->17470K(157248K), 0.0332652 secs] 271793K->178957K(506816K), 0.0333170 secs] [Times: user=0.02 sys=0.01, real=0.03 secs] 
2021-01-22T13:39:59.214+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.214+0800: [DefNew: 157246K->17470K(157248K), 0.0316103 secs] 318733K->222127K(506816K), 0.0316644 secs] [Times: user=0.00 sys=0.03, real=0.03 secs] 
2021-01-22T13:39:59.271+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.271+0800: [DefNew: 157184K->17469K(157248K), 0.0335195 secs] 361841K->269541K(506816K), 0.0335838 secs] [Times: user=0.02 sys=0.02, real=0.03 secs] 
2021-01-22T13:39:59.327+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.327+0800: [DefNew: 157245K->17471K(157248K), 0.0301295 secs] 409317K->314682K(506816K), 0.0301654 secs] [Times: user=0.02 sys=0.02, real=0.03 secs] 
2021-01-22T13:39:59.378+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.378+0800: [DefNew: 157247K->157247K(157248K), 0.0000182 secs]2021-01-22T13:39:59.378+0800: [Tenured: 297210K->265411K(349568K), 0.0485325 secs] 454458K->265411K(506816K), [Metaspace: 3505K->3505K(1056768K)], 0.0486514 secs] [Times: user=0.05 sys=0.00, real=0.05 secs] 
2021-01-22T13:39:59.450+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.450+0800: [DefNew: 139776K->17471K(157248K), 0.0106502 secs] 405187K->313664K(506816K), 0.0106889 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-22T13:39:59.488+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.488+0800: [DefNew: 157247K->17471K(157248K), 0.0412576 secs] 453440K->361992K(506816K), 0.0413122 secs] [Times: user=0.02 sys=0.02, real=0.04 secs] 
2021-01-22T13:39:59.559+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.559+0800: [DefNew: 156754K->156754K(157248K), 0.0000666 secs]2021-01-22T13:39:59.559+0800: [Tenured: 344520K->311956K(349568K), 0.0891689 secs] 501275K->311956K(506816K), [Metaspace: 3505K->3505K(1056768K)], 0.0893350 secs] [Times: user=0.09 sys=0.00, real=0.09 secs] 
2021-01-22T13:39:59.681+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.681+0800: [DefNew: 139776K->139776K(157248K), 0.0000410 secs]2021-01-22T13:39:59.681+0800: [Tenured: 311956K->324196K(349568K), 0.0793367 secs] 451732K->324196K(506816K), [Metaspace: 3505K->3505K(1056768K)], 0.0794693 secs] [Times: user=0.08 sys=0.00, real=0.08 secs] 
2021-01-22T13:39:59.784+0800: [GC (Allocation Failure) 2021-01-22T13:39:59.784+0800: [DefNew: 139776K->139776K(157248K), 0.0000558 secs]2021-01-22T13:39:59.785+0800: [Tenured: 324196K->318420K(349568K), 0.0883827 secs] 463972K->318420K(506816K), [Metaspace: 3505K->3505K(1056768K)], 0.0885180 secs] [Times: user=0.08 sys=0.00, real=0.09 secs] 
执行结束!共生成对象次数:6842
Heap
 def new generation   total 157248K, used 5714K [0x00000000e0000000, 0x00000000eaaa0000, 0x00000000eaaa0000)
  eden space 139776K,   4% used [0x00000000e0000000, 0x00000000e0594b28, 0x00000000e8880000)
  from space 17472K,   0% used [0x00000000e9990000, 0x00000000e9990000, 0x00000000eaaa0000)
  to   space 17472K,   0% used [0x00000000e8880000, 0x00000000e8880000, 0x00000000e9990000)
 tenured generation   total 349568K, used 318420K [0x00000000eaaa0000, 0x0000000100000000, 0x0000000100000000)
   the space 349568K,  91% used [0x00000000eaaa0000, 0x00000000fe195138, 0x00000000fe195200, 0x0000000100000000)
 Metaspace       used 3512K, capacity 4500K, committed 4864K, reserved 1056768K
  class space    used 385K, capacity 388K, committed 512K, reserved 1048576K
```

共生成对象次数:6842

#### 03) 并行512m

```
正在执行...
2021-01-22T13:42:01.198+0800: [GC (Allocation Failure) [PSYoungGen: 131584K->21503K(153088K)] 131584K->42142K(502784K), 0.0175002 secs] [Times: user=0.02 sys=0.11, real=0.02 secs] 
2021-01-22T13:42:01.243+0800: [GC (Allocation Failure) [PSYoungGen: 153087K->21499K(153088K)] 173726K->83318K(502784K), 0.0201296 secs] [Times: user=0.03 sys=0.08, real=0.02 secs] 
2021-01-22T13:42:01.292+0800: [GC (Allocation Failure) [PSYoungGen: 153083K->21492K(153088K)] 214902K->121103K(502784K), 0.0158237 secs] [Times: user=0.03 sys=0.09, real=0.02 secs] 
2021-01-22T13:42:01.340+0800: [GC (Allocation Failure) [PSYoungGen: 153076K->21496K(153088K)] 252687K->162070K(502784K), 0.0166702 secs] [Times: user=0.13 sys=0.00, real=0.02 secs] 
2021-01-22T13:42:01.382+0800: [GC (Allocation Failure) [PSYoungGen: 153055K->21493K(153088K)] 293628K->202354K(502784K), 0.0163823 secs] [Times: user=0.00 sys=0.13, real=0.02 secs] 
2021-01-22T13:42:01.421+0800: [GC (Allocation Failure) [PSYoungGen: 153077K->21497K(80384K)] 333938K->248007K(430080K), 0.0176100 secs] [Times: user=0.09 sys=0.11, real=0.02 secs] 
2021-01-22T13:42:01.451+0800: [GC (Allocation Failure) [PSYoungGen: 80326K->35591K(116736K)] 306836K->268258K(466432K), 0.0100585 secs] [Times: user=0.08 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.476+0800: [GC (Allocation Failure) [PSYoungGen: 94471K->45791K(116736K)] 327138K->284656K(466432K), 0.0110729 secs] [Times: user=0.06 sys=0.05, real=0.01 secs] 
2021-01-22T13:42:01.497+0800: [GC (Allocation Failure) [PSYoungGen: 104219K->55994K(116736K)] 343084K->303210K(466432K), 0.0129536 secs] [Times: user=0.11 sys=0.02, real=0.01 secs] 
2021-01-22T13:42:01.525+0800: [GC (Allocation Failure) [PSYoungGen: 114720K->38834K(116736K)] 361935K->320580K(466432K), 0.0168511 secs] [Times: user=0.09 sys=0.03, real=0.02 secs] 
2021-01-22T13:42:01.553+0800: [GC (Allocation Failure) [PSYoungGen: 97714K->23647K(116736K)] 379460K->339490K(466432K), 0.0130623 secs] [Times: user=0.02 sys=0.08, real=0.01 secs] 
2021-01-22T13:42:01.566+0800: [Full GC (Ergonomics) [PSYoungGen: 23647K->0K(116736K)] [ParOldGen: 315843K->244071K(349696K)] 339490K->244071K(466432K), [Metaspace: 3505K->3505K(1056768K)], 0.0567888 secs] [Times: user=0.31 sys=0.00, real=0.06 secs] 
2021-01-22T13:42:01.633+0800: [GC (Allocation Failure) [PSYoungGen: 58279K->20132K(116736K)] 302351K->264204K(466432K), 0.0046683 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.651+0800: [GC (Allocation Failure) [PSYoungGen: 78886K->19156K(116736K)] 322958K->282049K(466432K), 0.0083507 secs] [Times: user=0.08 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.671+0800: [GC (Allocation Failure) [PSYoungGen: 77583K->21397K(116736K)] 340476K->303042K(466432K), 0.0085521 secs] [Times: user=0.11 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.690+0800: [GC (Allocation Failure) [PSYoungGen: 80277K->19823K(116736K)] 361922K->321431K(466432K), 0.0104624 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.700+0800: [Full GC (Ergonomics) [PSYoungGen: 19823K->0K(116736K)] [ParOldGen: 301607K->270627K(349696K)] 321431K->270627K(466432K), [Metaspace: 3505K->3505K(1056768K)], 0.0772154 secs] [Times: user=0.25 sys=0.00, real=0.08 secs] 
2021-01-22T13:42:01.798+0800: [GC (Allocation Failure) [PSYoungGen: 58797K->18005K(116736K)] 329425K->288633K(466432K), 0.0078541 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.823+0800: [GC (Allocation Failure) [PSYoungGen: 76495K->22089K(116736K)] 347122K->309212K(466432K), 0.0088024 secs] [Times: user=0.11 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.849+0800: [GC (Allocation Failure) [PSYoungGen: 80969K->21746K(116736K)] 368092K->329762K(466432K), 0.0109807 secs] [Times: user=0.02 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.860+0800: [Full GC (Ergonomics) [PSYoungGen: 21746K->0K(116736K)] [ParOldGen: 308015K->292612K(349696K)] 329762K->292612K(466432K), [Metaspace: 3505K->3505K(1056768K)], 0.0645940 secs] [Times: user=0.39 sys=0.00, real=0.06 secs] 
2021-01-22T13:42:01.937+0800: [GC (Allocation Failure) [PSYoungGen: 58796K->21801K(116736K)] 351408K->314413K(466432K), 0.0047081 secs] [Times: user=0.09 sys=0.00, real=0.00 secs] 
2021-01-22T13:42:01.954+0800: [GC (Allocation Failure) [PSYoungGen: 80149K->19235K(116736K)] 372761K->331764K(466432K), 0.0082688 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:01.962+0800: [Full GC (Ergonomics) [PSYoungGen: 19235K->0K(116736K)] [ParOldGen: 312528K->303132K(349696K)] 331764K->303132K(466432K), [Metaspace: 3505K->3505K(1056768K)], 0.0673918 secs] [Times: user=0.31 sys=0.00, real=0.07 secs] 
2021-01-22T13:42:02.045+0800: [GC (Allocation Failure) [PSYoungGen: 58754K->20549K(116736K)] 361886K->323681K(466432K), 0.0052713 secs] [Times: user=0.09 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:02.064+0800: [GC (Allocation Failure) [PSYoungGen: 79290K->21136K(116736K)] 382422K->344014K(466432K), 0.0093594 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:42:02.073+0800: [Full GC (Ergonomics) [PSYoungGen: 21136K->0K(116736K)] [ParOldGen: 322878K->308980K(349696K)] 344014K->308980K(466432K), [Metaspace: 3505K->3505K(1056768K)], 0.0678696 secs] [Times: user=0.39 sys=0.00, real=0.07 secs] 
执行结束!共生成对象次数:6473
Heap
 PSYoungGen      total 116736K, used 2788K [0x00000000f5580000, 0x0000000100000000, 0x0000000100000000)
  eden space 58880K, 4% used [0x00000000f5580000,0x00000000f58391e0,0x00000000f8f00000)
  from space 57856K, 0% used [0x00000000fc780000,0x00000000fc780000,0x0000000100000000)
  to   space 57856K, 0% used [0x00000000f8f00000,0x00000000f8f00000,0x00000000fc780000)
 ParOldGen       total 349696K, used 308980K [0x00000000e0000000, 0x00000000f5580000, 0x00000000f5580000)
  object space 349696K, 88% used [0x00000000e0000000,0x00000000f2dbd3f8,0x00000000f5580000)
 Metaspace       used 3512K, capacity 4500K, committed 4864K, reserved 1056768K
  class space    used 385K, capacity 388K, committed 512K, reserved 1048576K
```

共生成对象次数:6473

#### 4）并行1g

```
正在执行...
2021-01-22T13:43:59.594+0800: [GC (Allocation Failure) [PSYoungGen: 262144K->43505K(305664K)] 262144K->76897K(1005056K), 0.0244907 secs] [Times: user=0.09 sys=0.11, real=0.02 secs] 
2021-01-22T13:43:59.674+0800: [GC (Allocation Failure) [PSYoungGen: 305649K->43510K(305664K)] 339041K->152032K(1005056K), 0.0368600 secs] [Times: user=0.00 sys=0.22, real=0.04 secs] 
2021-01-22T13:43:59.765+0800: [GC (Allocation Failure) [PSYoungGen: 305654K->43518K(305664K)] 414176K->227244K(1005056K), 0.0349594 secs] [Times: user=0.08 sys=0.17, real=0.03 secs] 
2021-01-22T13:43:59.866+0800: [GC (Allocation Failure) [PSYoungGen: 305662K->43506K(305664K)] 489388K->301539K(1005056K), 0.0365187 secs] [Times: user=0.08 sys=0.09, real=0.04 secs] 
2021-01-22T13:43:59.971+0800: [GC (Allocation Failure) [PSYoungGen: 305650K->43518K(305664K)] 563683K->376884K(1005056K), 0.0421934 secs] [Times: user=0.03 sys=0.19, real=0.04 secs] 
2021-01-22T13:44:00.073+0800: [GC (Allocation Failure) [PSYoungGen: 305662K->43518K(160256K)] 639028K->450246K(859648K), 0.0339246 secs] [Times: user=0.13 sys=0.09, real=0.03 secs] 
2021-01-22T13:44:00.132+0800: [GC (Allocation Failure) [PSYoungGen: 160254K->75071K(232960K)] 566982K->487743K(932352K), 0.0175508 secs] [Times: user=0.11 sys=0.00, real=0.02 secs] 
2021-01-22T13:44:00.173+0800: [GC (Allocation Failure) [PSYoungGen: 191807K->89970K(232960K)] 604479K->510652K(932352K), 0.0211627 secs] [Times: user=0.08 sys=0.02, real=0.02 secs] 
2021-01-22T13:44:00.220+0800: [GC (Allocation Failure) [PSYoungGen: 206706K->100467K(232960K)] 627388K->539154K(932352K), 0.0255716 secs] [Times: user=0.11 sys=0.00, real=0.03 secs] 
2021-01-22T13:44:00.270+0800: [GC (Allocation Failure) [PSYoungGen: 217203K->76424K(232960K)] 655890K->569051K(932352K), 0.0301119 secs] [Times: user=0.13 sys=0.11, real=0.03 secs] 
2021-01-22T13:44:00.328+0800: [GC (Allocation Failure) [PSYoungGen: 192755K->42093K(232960K)] 685382K->602613K(932352K), 0.0279712 secs] [Times: user=0.09 sys=0.09, real=0.03 secs] 
2021-01-22T13:44:00.378+0800: [GC (Allocation Failure) [PSYoungGen: 158643K->46967K(232960K)] 719163K->645286K(932352K), 0.0185492 secs] [Times: user=0.06 sys=0.05, real=0.02 secs] 
2021-01-22T13:44:00.397+0800: [Full GC (Ergonomics) [PSYoungGen: 46967K->0K(232960K)] [ParOldGen: 598319K->339593K(699392K)] 645286K->339593K(932352K), [Metaspace: 3505K->3505K(1056768K)], 0.0818376 secs] [Times: user=0.58 sys=0.01, real=0.08 secs] 
执行结束!共生成对象次数:8646
Heap
 PSYoungGen      total 232960K, used 7267K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
  eden space 116736K, 6% used [0x00000000eab00000,0x00000000eb218d98,0x00000000f1d00000)
  from space 116224K, 0% used [0x00000000f8e80000,0x00000000f8e80000,0x0000000100000000)
  to   space 116224K, 0% used [0x00000000f1d00000,0x00000000f1d00000,0x00000000f8e80000)
 ParOldGen       total 699392K, used 339593K [0x00000000c0000000, 0x00000000eab00000, 0x00000000eab00000)
  object space 699392K, 48% used [0x00000000c0000000,0x00000000d4ba26c8,0x00000000eab00000)
 Metaspace       used 3512K, capacity 4500K, committed 4864K, reserved 1056768K
  class space    used 385K, capacity 388K, committed 512K, reserved 1048576K
```

共生成对象次数:8646 ，可以看到只发生了一次fullGC。



#### 5）并行1g 不指定Xms1g。

```
正在执行...
2021-01-22T13:44:59.295+0800: [GC (Allocation Failure) [PSYoungGen: 65017K->10739K(75776K)] 65017K->22098K(249344K), 0.0079462 secs] [Times: user=0.06 sys=0.03, real=0.01 secs] 
2021-01-22T13:44:59.325+0800: [GC (Allocation Failure) [PSYoungGen: 75763K->10736K(140800K)] 87122K->44024K(314368K), 0.0150136 secs] [Times: user=0.03 sys=0.05, real=0.01 secs] 
2021-01-22T13:44:59.389+0800: [GC (Allocation Failure) [PSYoungGen: 140751K->10735K(140800K)] 174039K->83746K(314368K), 0.0167043 secs] [Times: user=0.03 sys=0.08, real=0.02 secs] 
2021-01-22T13:44:59.433+0800: [GC (Allocation Failure) [PSYoungGen: 140783K->10740K(270848K)] 213794K->131740K(444416K), 0.0188337 secs] [Times: user=0.03 sys=0.08, real=0.02 secs] 
2021-01-22T13:44:59.452+0800: [Full GC (Ergonomics) [PSYoungGen: 10740K->0K(270848K)] [ParOldGen: 121000K->117184K(256512K)] 131740K->117184K(527360K), [Metaspace: 3506K->3506K(1056768K)], 0.0304077 secs] [Times: user=0.11 sys=0.02, real=0.03 secs] 
2021-01-22T13:44:59.580+0800: [GC (Allocation Failure) [PSYoungGen: 260096K->10750K(270848K)] 377280K->191947K(527360K), 0.0233723 secs] [Times: user=0.03 sys=0.09, real=0.02 secs] 
2021-01-22T13:44:59.604+0800: [Full GC (Ergonomics) [PSYoungGen: 10750K->0K(270848K)] [ParOldGen: 181197K->162498K(350208K)] 191947K->162498K(621056K), [Metaspace: 3506K->3506K(1056768K)], 0.0366376 secs] [Times: user=0.22 sys=0.00, real=0.04 secs] 
2021-01-22T13:44:59.694+0800: [GC (Allocation Failure) [PSYoungGen: 260096K->78251K(230912K)] 422594K->241948K(581120K), 0.0236078 secs] [Times: user=0.03 sys=0.08, real=0.02 secs] 
2021-01-22T13:44:59.748+0800: [GC (Allocation Failure) [PSYoungGen: 230827K->98302K(250880K)] 394524K->273702K(601088K), 0.0216536 secs] [Times: user=0.23 sys=0.00, real=0.02 secs] 
2021-01-22T13:44:59.801+0800: [GC (Allocation Failure) [PSYoungGen: 250878K->98301K(215040K)] 426278K->310052K(565248K), 0.0323977 secs] [Times: user=0.14 sys=0.08, real=0.03 secs] 
2021-01-22T13:44:59.853+0800: [GC (Allocation Failure) [PSYoungGen: 215037K->93368K(232960K)] 426788K->340820K(583168K), 0.0248189 secs] [Times: user=0.20 sys=0.03, real=0.03 secs] 
2021-01-22T13:44:59.898+0800: [GC (Allocation Failure) [PSYoungGen: 210104K->77405K(232960K)] 457556K->372164K(583168K), 0.0298809 secs] [Times: user=0.14 sys=0.05, real=0.03 secs] 
2021-01-22T13:44:59.928+0800: [Full GC (Ergonomics) [PSYoungGen: 77405K->0K(232960K)] [ParOldGen: 294758K->273159K(476672K)] 372164K->273159K(709632K), [Metaspace: 3506K->3506K(1056768K)], 0.0637315 secs] [Times: user=0.38 sys=0.01, real=0.06 secs] 
2021-01-22T13:45:00.019+0800: [GC (Allocation Failure) [PSYoungGen: 116736K->41780K(232960K)] 389895K->314940K(709632K), 0.0087171 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
2021-01-22T13:45:00.052+0800: [GC (Allocation Failure) [PSYoungGen: 158516K->39937K(232960K)] 431676K->350473K(709632K), 0.0199259 secs] [Times: user=0.06 sys=0.02, real=0.02 secs] 
2021-01-22T13:45:00.098+0800: [GC (Allocation Failure) [PSYoungGen: 156673K->40886K(232960K)] 467209K->386235K(709632K), 0.0205597 secs] [Times: user=0.06 sys=0.00, real=0.02 secs] 
2021-01-22T13:45:00.143+0800: [GC (Allocation Failure) [PSYoungGen: 157622K->41582K(232960K)] 502971K->421843K(709632K), 0.0192404 secs] [Times: user=0.00 sys=0.09, real=0.02 secs] 
2021-01-22T13:45:00.189+0800: [GC (Allocation Failure) [PSYoungGen: 158303K->44298K(232960K)] 538564K->460725K(709632K), 0.0181544 secs] [Times: user=0.05 sys=0.06, real=0.02 secs] 
2021-01-22T13:45:00.207+0800: [Full GC (Ergonomics) [PSYoungGen: 44298K->0K(232960K)] [ParOldGen: 416427K->328792K(547840K)] 460725K->328792K(780800K), [Metaspace: 3506K->3506K(1056768K)], 0.0779299 secs] [Times: user=0.45 sys=0.00, real=0.08 secs] 
执行结束!共生成对象次数:7627
Heap
 PSYoungGen      total 232960K, used 4819K [0x00000000eab00000, 0x0000000100000000, 0x0000000100000000)
  eden space 116736K, 4% used [0x00000000eab00000,0x00000000eafb4da0,0x00000000f1d00000)
  from space 116224K, 0% used [0x00000000f1d00000,0x00000000f1d00000,0x00000000f8e80000)
  to   space 116224K, 0% used [0x00000000f8e80000,0x00000000f8e80000,0x0000000100000000)
 ParOldGen       total 547840K, used 328792K [0x00000000c0000000, 0x00000000e1700000, 0x00000000eab00000)
  object space 547840K, 60% used [0x00000000c0000000,0x00000000d41161d0,0x00000000e1700000)
 Metaspace       used 3513K, capacity 4500K, committed 4864K, reserved 1056768K
  class space    used 385K, capacity 388K, committed 512K, reserved 1048576K
```

共生成对象次数:7627

| **Throughput**    | **44.079%** |
| :---------------- | ----------- |
| Avg Pause GC Time | **26.8 ms** |
| Max Pause GC Time | **80.0 ms** |

可以看到，由于没有指定Xms，导致多发生了几次fullGC，吞吐量比指定时有所降低。

#### 6） CMS 1g

```
正在执行...
2021-01-22T13:49:35.107+0800: [GC (Allocation Failure) 2021-01-22T13:49:35.107+0800: [ParNew: 279616K->34944K(314560K), 0.0274062 secs] 279616K->86606K(1013632K), 0.0275035 secs] [Times: user=0.03 sys=0.22, real=0.03 secs] 
2021-01-22T13:49:35.192+0800: [GC (Allocation Failure) 2021-01-22T13:49:35.192+0800: [ParNew: 314560K->34944K(314560K), 0.0372298 secs] 366222K->158156K(1013632K), 0.0372850 secs] [Times: user=0.08 sys=0.08, real=0.04 secs] 
2021-01-22T13:49:35.286+0800: [GC (Allocation Failure) 2021-01-22T13:49:35.286+0800: [ParNew: 314560K->34942K(314560K), 0.0593255 secs] 437772K->238399K(1013632K), 0.0594666 secs] [Times: user=0.28 sys=0.05, real=0.06 secs] 
2021-01-22T13:49:35.403+0800: [GC (Allocation Failure) 2021-01-22T13:49:35.403+0800: [ParNew: 314558K->34944K(314560K), 0.0557517 secs] 518015K->315482K(1013632K), 0.0557967 secs] [Times: user=0.39 sys=0.05, real=0.06 secs] 
2021-01-22T13:49:35.510+0800: [GC (Allocation Failure) 2021-01-22T13:49:35.510+0800: [ParNew: 314560K->34943K(314560K), 0.0573014 secs] 595098K->394882K(1013632K), 0.0573480 secs] [Times: user=0.36 sys=0.05, real=0.06 secs] 
2021-01-22T13:49:35.568+0800: [GC (CMS Initial Mark) [1 CMS-initial-mark: 359939K(699072K)] 400802K(1013632K), 0.0004887 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.569+0800: [CMS-concurrent-mark-start]
2021-01-22T13:49:35.573+0800: [CMS-concurrent-mark: 0.004/0.004 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.573+0800: [CMS-concurrent-preclean-start]
2021-01-22T13:49:35.574+0800: [CMS-concurrent-preclean: 0.001/0.001 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.574+0800: [CMS-concurrent-abortable-preclean-start]
2021-01-22T13:49:35.619+0800: [GC (Allocation Failure) 2021-01-22T13:49:35.619+0800: [ParNew: 314559K->34943K(314560K), 0.0504383 secs] 674498K->466703K(1013632K), 0.0504798 secs] [Times: user=0.30 sys=0.03, real=0.05 secs] 
2021-01-22T13:49:35.721+0800: [GC (Allocation Failure) 2021-01-22T13:49:35.721+0800: [ParNew2021-01-22T13:49:35.772+0800: [CMS-concurrent-abortable-preclean: 0.003/0.198 secs] [Times: user=0.67 sys=0.06, real=0.20 secs] 
: 314559K->34943K(314560K), 0.0538238 secs] 746319K->537315K(1013632K), 0.0538960 secs] [Times: user=0.25 sys=0.03, real=0.05 secs] 
2021-01-22T13:49:35.775+0800: [GC (CMS Final Remark) [YG occupancy: 41146 K (314560 K)]2021-01-22T13:49:35.775+0800: [Rescan (parallel) , 0.0010871 secs]2021-01-22T13:49:35.777+0800: [weak refs processing, 0.0000740 secs]2021-01-22T13:49:35.777+0800: [class unloading, 0.0013232 secs]2021-01-22T13:49:35.778+0800: [scrub symbol table, 0.0009438 secs]2021-01-22T13:49:35.779+0800: [scrub string table, 0.0002156 secs][1 CMS-remark: 502372K(699072K)] 543519K(1013632K), 0.0038724 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.779+0800: [CMS-concurrent-sweep-start]
2021-01-22T13:49:35.781+0800: [CMS-concurrent-sweep: 0.002/0.002 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.781+0800: [CMS-concurrent-reset-start]
2021-01-22T13:49:35.784+0800: [CMS-concurrent-reset: 0.003/0.003 secs] [Times: user=0.03 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.837+0800: [GC (Allocation Failure) 2021-01-22T13:49:35.837+0800: [ParNew: 314559K->34943K(314560K), 0.0241715 secs] 698225K->493588K(1013632K), 0.0242847 secs] [Times: user=0.20 sys=0.00, real=0.02 secs] 
2021-01-22T13:49:35.861+0800: [GC (CMS Initial Mark) [1 CMS-initial-mark: 458645K(699072K)] 493876K(1013632K), 0.0002970 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.862+0800: [CMS-concurrent-mark-start]
2021-01-22T13:49:35.864+0800: [CMS-concurrent-mark: 0.003/0.003 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.864+0800: [CMS-concurrent-preclean-start]
2021-01-22T13:49:35.866+0800: [CMS-concurrent-preclean: 0.001/0.001 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:49:35.866+0800: [CMS-concurrent-abortable-preclean-start]
执行结束!共生成对象次数:9374
Heap
 par new generation   total 314560K, used 298797K [0x00000000c0000000, 0x00000000d5550000, 0x00000000d5550000)
  eden space 279616K,  94% used [0x00000000c0000000, 0x00000000d01ab850, 0x00000000d1110000)
  from space 34944K,  99% used [0x00000000d1110000, 0x00000000d332fe60, 0x00000000d3330000)
  to   space 34944K,   0% used [0x00000000d3330000, 0x00000000d3330000, 0x00000000d5550000)
 concurrent mark-sweep generation total 699072K, used 458645K [0x00000000d5550000, 0x0000000100000000, 0x0000000100000000)
 Metaspace       used 3511K, capacity 4500K, committed 4864K, reserved 1056768K
  class space    used 385K, capacity 388K, committed 512K, reserved 1048576K
```

共生成对象次数:9374

| **Throughput**    | **57.839%** |
| :---------------- | ----------- |
| Avg Pause GC Time | **35.6 ms** |
| Max Pause GC Time | **60.0 ms** |

* 可以看到使用cmsgc后，吞吐量甚至比并行gc还大一些。

* 同时看到，初始标记和重新标记两个STW阶段，占用时长之和只有1ms，服务暂停时间是很客观的。

```
2021-01-22T13:50:31.656+0800: [GC (CMS Initial Mark) [1 CMS-initial-mark: 372242K(699072K)] 407303K(1013632K), 0.0004278 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 

2021-01-22T13:50:31.656+0800: [CMS-concurrent-mark-start]
2021-01-22T13:50:31.660+0800: [CMS-concurrent-mark: 0.004/0.004 secs] [Times: user=0.03 sys=0.02, real=0.00 secs] 
2021-01-22T13:50:31.660+0800: [CMS-concurrent-preclean-start]
2021-01-22T13:50:31.662+0800: [CMS-concurrent-preclean: 0.001/0.001 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:50:31.662+0800: [CMS-concurrent-abortable-preclean-start]

2021-01-22T13:50:31.706+0800: [GC (Allocation Failure) 2021-01-22T13:50:31.707+0800: [ParNew2021-01-22T13:50:31.764+0800: [CMS-concurrent-abortable-preclean: 0.001/0.102 secs] [Times: user=0.28 sys=0.05, real=0.10 secs] 
: 314560K->34944K(314560K), 0.0583663 secs] 686802K->490604K(1013632K), 0.0584175 secs] [Times: user=0.23 sys=0.05, real=0.06 secs] 
2021-01-22T13:50:31.765+0800: [GC (CMS Final Remark) [YG occupancy: 41016 K (314560 K)]2021-01-22T13:50:31.765+0800: [Rescan (parallel) , 0.0004415 secs]2021-01-22T13:50:31.766+0800: [weak refs processing, 0.0000171 secs]2021-01-22T13:50:31.766+0800: [class unloading, 0.0004898 secs]2021-01-22T13:50:31.766+0800: [scrub symbol table, 0.0004801 secs]2021-01-22T13:50:31.767+0800: [scrub string table, 0.0001502 secs][1 CMS-remark: 455660K(699072K)] 496677K(1013632K), 0.0016788 secs] [Times: user=0.02 sys=0.00, real=0.00 secs] 
2021-01-22T13:50:31.767+0800: [CMS-concurrent-sweep-start]
2021-01-22T13:50:31.768+0800: [CMS-concurrent-sweep: 0.001/0.001 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
2021-01-22T13:50:31.768+0800: [CMS-concurrent-reset-start]
2021-01-22T13:50:31.772+0800: [CMS-concurrent-reset: 0.004/0.004 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
```

#### 7） G1 1g

| **Throughput**    | **80.263%** |
| :---------------- | ----------- |
| Avg Pause GC Time | **12.9 ms** |
| Max Pause GC Time | **30.0 ms** |

执行结束!共生成对象次数:7557

* 可以看出g1虽然是cms的加强版，但是在小内存应用中没什么优势。不过吞吐量显示是最大的。

### 2、 使用压测工具（wrk或sb） ， 演练gateway-server-0.0.1-SNAPSHOT.jar示例。

ab -c 30 -n 100000  http://localhost:8088/api/hello

#### xmx1g xms默认  并行gc

Requests per second:    5314.06 [#/sec] (mean)

#### xmx4g xms默认 并行gc

Requests per second:    6606.33 [#/sec] (mean)

#### xmx1g xms1g  并行gc

Requests per second:    6741.27 [#/sec] (mean)

#### xmx1g xms1g  串行gc

Requests per second:    5509.65 [#/sec] (mean)

#### xmx1g xms1g  cms gc

Requests per second:    4698.44 [#/sec] (mean)

#### xmx1g xms1g  g1 gc

Requests per second:    5339.88 [#/sec] (mean)

#### xmx4g xms4g  g1 gc

Requests per second:    4813.26 [#/sec] (mean)

从压测结果可以看出，

吞吐量：并行gc>g1>cms

所以在对暂停时间不敏感的业务系统中（比如文件迁移系统、定时任务服务等非直接向用户做响应的服务），可以优先考虑使用并行gc，提供系统的业务吞吐量。

### 3、 (选做)如果自己本地有可以运行的项目， 可以按照2的方式进行演练。

略。

### 4、 (必做)根据上述自己对于1和2的演示， 写一段对于不同GC和堆内存的总结， 提交到github。 

* Serial垃圾收集器

  **作用范围**：young区

  **算法**：复制

  **优点**：gc占用线程和内存较少，适用于小型应用。

  **缺点**：单线程gc效率低，导致暂停时间长，吞吐量低。

  **描述**：使用串行的gc策略，串行可以理解为gc线程为单线程，只能串行执行垃圾收集。意味着GC的效率比较低，吞吐量较低，暂时时间较长。但是由于算法简单，没有gc线程上下文切换等复杂机制，所以适用于极小堆内存的场景，比如小客户端程序。所以，JDK把Serial+Serial Old作为JVM client模式下的默认GC组合，Serial适用复制算法，Serial Old适用标记整理算法，分别执行年轻代和年老代的垃圾收集。

  **搭配**：Serial old

* Parallel scavenge 

  **作用范围**：young区

  **算法**：复制

  **优点**：多线程并行收集，服务吞吐量大。

  **缺点**：并行导致暂停时间相比并发收集的gc更长。

  **描述**：通过自身算法根据暂停时间目标和吞吐量目标动态调控gc策略。

  **搭配**：ps old JDK8默认

* Parallel Old

  **作用范围**：old区

  **算法**：标记整理

  **优点**：多线程并行收集，服务吞吐量大。

  **缺点**：并行导致暂停时间相比并发收集的gc更长，整理也会增加暂停时间。

  **描述**：通过自身算法根据暂停时间目标和吞吐量目标动态调控gc策略。

* parNew垃圾收集器

  **作用范围**：young区

  **算法**：复制

  **优点**：多线程并行收集

  **缺点**：与CMS搭配的算法较为落后，被G1等新的gc取代

  **描述**：serial的多线程版本

  **搭配**：cms

* CMS垃圾收集器

  **作用范围**：old区

  **算法**：并发的标记清除算法

  **优点**：最大暂停时间优先，省去了内存整理，执行效率高。

  **缺点**：容易产生内存碎片，退化为serialold，可能导致GC频繁。

  **描述**：将标记和清理分为多个阶段，只有初始标记和最终标记两个阶段需要STW，暂时时间较短。

* G1垃圾收集器

  **作用范围**：full

  **算法**：region分代回收

  **优点**：最大暂停时间优先，大内存时效果好

  **缺点**：算法较为复杂，小内存情况下可能不如cms或并行合适

  **描述**：将堆内存分为多个region。

* ZGC

* Shenandoah 

#### 总结

gc：

* 如果是内存很小的客户端应用，可以使用serial组合的gc。
* 如果是内存不到4G，重停顿时间。可以尝试使用cms。
* 如果内存不大，重吞吐量，可以使用ps并行gc。
* 如果是大内存，可以使用G1GC。
  * 如果是JDK13，oracleJDK可以使用ZGC，openJDK可以使用Shenandoah。
* 当然，不是一定的，要不断调整参数，依据压测结果，灵活选择。

堆：

 * 堆包括：
   * 1、所有引用类型对象（也包括class、method等对象实例）。
   * 2、字符串常量池



## 第4课

### 1、 （可选） 运行课上的例子， 以及 Netty 的例子， 分析相关现象

```
Thread.sleep(20);
-Xmx512m -Xms512m
```

---

#### HttpServer01  

```
 wfy@wfy0828  ~/work/workspace  wrk -c 40 -d 30s http://localhost:8801
Running 30s test @ http://localhost:8801
  2 threads and 40 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   916.41ms   86.13ms 987.20ms   97.52%
    Req/Sec     8.79      5.17    30.00     72.70%
  444 requests in 30.07s, 60.02KB read
  Socket errors: connect 0, read 1291, write 1, timeout 0
Requests/sec:     14.76
Transfer/sec:      2.00KB
```

30s内处理444个请求，平均延迟916ms。

####  HttpServer02

```
 wfy@wfy0828  ~/work/workspace  wrk -c 40 -d 30s http://localhost:8802
Running 30s test @ http://localhost:8802
  2 threads and 40 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    25.80ms    3.59ms  60.79ms   72.62%
    Req/Sec   117.34     39.76   245.00     69.30%
  7034 requests in 30.06s, 1.60MB read
  Socket errors: connect 0, read 45267, write 33, timeout 0
Requests/sec:    233.97
Transfer/sec:     54.39KB
```

30s内处理7034个请求，平均延迟25ms。

#### HttpServer03

```
 wfy@wfy0828  ~/work/workspace  wrk -c 40 -d 30s http://localhost:8803
Running 30s test @ http://localhost:8803
  2 threads and 40 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   152.35ms    9.07ms 170.76ms   77.74%
    Req/Sec    23.38     14.30    79.00     64.78%
  1343 requests in 30.10s, 273.51KB read
  Socket errors: connect 0, read 7836, write 15, timeout 0
Requests/sec:     44.62
Transfer/sec:      9.09KB
```

30s内处理1343个请求，平均延迟152ms。

#### NettyHttpServer 

除了自有代码逻辑，未额外设置线程sleep。

```
 wfy@wfy0828  ~/work/workspace  wrk -c 40 -d 30s http://127.0.0.1:8808/test
Running 30s test @ http://127.0.0.1:8808/test
  2 threads and 40 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.04ms   13.18ms 404.52ms   97.63%
    Req/Sec    45.41k    14.31k   77.87k    75.42%
  2696701 requests in 30.03s, 275.18MB read
Requests/sec:  89809.51
Transfer/sec:      9.16MB
```

30s内处理2696701个请求，平均延迟2ms!!!

#### 分析：

HttpServer01由于server端只有一个线程监听客户端的请求，所以只能串行执行。server线程接收client的请求后，需要处理请求，直到成功响应后，才能接入下一个请求。所以速度比较慢。

HttpServer03使用了线程池，线程数为CPU核数+2，所以速度有所提升。

HttpServer02是每接收到一个请求，就创建一个线程处理。因为，在不考虑线程数多至线程上下文切换成本过大的情况下，可能性能最好，当然 也可能是数据样本太少的原因。

NettyHttpServer使用了NIO模型，不同于阻塞型IO模型。**netty使用selector/epoll的NIO模型：1、避免了server应用进程向内核请求数据的阻塞；2、与内核共享了内存空间；3、fd没有了限制；4、通过事件回调解决了每次需要遍历selector中所有fd的问题。**所以，并发量大大增加。

### 2、 （必做） 写一段代码， 使用 HttpClient 或 OkHttp 访问 http://localhost:8801， 代码提交到Github。  

[OkHttpRequest.java](nio01/src/main/java/java0/nio01/OkHttpRequest.java)

