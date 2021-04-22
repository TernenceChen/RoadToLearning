## Thread Pool

线程池（Thread Pool）是一种基于池化思想管理线程的工具。

使用线程池可以带来的好处：

- **降低资源消耗：**通过优化技术重复利用已创建的线程，降低线程创建和销毁造成的损耗。
- **提高响应速度：**任务到达时，无需等待线程创建即可立即执行
- **提高线程的可管理性：**线程是稀缺资源，如果无限制创建，不仅会消耗系统资源，还会因为线程的不合理分布导致资源调度失衡，降低系统的稳定性。使用线程池可以进行统一的分配、调优和监控。
- **提供更多更强大的功能：**线程池具备可拓展性，允许向其中增加更多的功能。比如延时定时线程池ScheduledThreadPoolExecutor，就允许任务延期执行或定期执行。

线程池解决的核心问题就是**资源管理问题**。线程池采用“池化”（Pooling）思想，池化，顾名思义，为了最大化收益并最小化风险，而将资源统一在一起管理的一种思想。

在并发环境下，系统不能够确定在任意时刻中，有多少任务需要执行，有多少资源需要投入。这种不确定性将带来以下问题：

- 频繁申请/销毁资源和调度资源，将带来额外的消耗，可能会非常巨大。
- 对资源无限申请缺少抑制手段，易引发系统资源耗尽的风险。
- 系统无法合理管理内部的资源分布，会降低系统的稳定性。

### 线程池核心设计与实现

#### 总体设计

线程池在Java中的体现是ThreadPoolExecutor类。ThreadPoolExecutor实现的顶层接口是Executor，顶层接口Executor提供了一种思想：将任务提交和任务执行进行解耦。用户无需关注如何创建线程，如何调度线程来执行任务，用户只需提供Runnable对象，将任务的运行逻辑提交到执行器(Executor)中，由Executor框架完成线程的调配和任务的执行部分。

ExecutorService接口增加了一些能力：（1）扩充执行任务的能力，补充可以为一个或一批异步任务生成Future的方法；（2）提供了管控线程池的方法，比如停止线程池的运行。

AbstractExecutorService则是上层的抽象类，将执行任务的流程串联了起来，保证下层的实现只需关注一个执行任务的方法即可。

最下层的实现类ThreadPoolExecutor实现最复杂的运行部分，ThreadPoolExecutor将会一方面维护自身的生命周期，另一方面同时管理线程和任务，使两者良好的结合从而执行并行任务。

![](D:%5Cternence%5CRoadToLearning%5Cimage%5CThreadPoolExecutor.png)

线程池在内部实际上构建了一个生产者消费者模型，将线程和任务两者解耦，并不直接关联，从而良好的缓冲任务，复用线程。

线程池的运行主要分成两部分：任务管理、线程管理。任务管理部分充当生产者的角色，当任务提交后，线程池会判断该任务后续的流转：1.直接申请线程执行该任务；2.缓冲到队列中等待线程执行；3.拒绝该任务。线程管理部分是消费者，它们被统一维护在线程池内，根据任务请求进行线程的分配，当线程执行完任务后，则会继续获取新的任务去执行，最终当线程获取不到任务的时候，线程就会被回收。

#### 声明周期管理

线程池运行的状态，并不是用户显式设置的，而是伴随着线程池的运行，由内部来维护。线程池内部使用一个变量维护两个值：运行状态(runState)和线程数量 (workerCount)。在具体实现中，线程池将运行状态(runState)、线程数量 (workerCount)两个关键参数的维护放在了一起，如下代码所示：

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
```

`ctl` 这个AtomicInteger类型，是对线程池的运行状态和线程池中有效线程的数量进行控制的一个字段， 它同时包含两部分的信息：线程池的运行状态 (runState) 和线程池内有效线程的数量 (workerCount)，高3位保存runState，低29位保存workerCount，两个变量之间互不干扰。用一个变量去存储两个值，可避免在做相关决策时，出现不一致的情况，不必为了维护两者的一致，而占用锁资源。

内部封装获取生命周期状态、获取线程池线程数量的计算方法：

```java
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

private static int runStateOf(int c)     { return c & ~CAPACITY; }	// 计算当前运行状态
private static int workerCountOf(int c)  { return c & CAPACITY; }	// 计算当前线程数量
private static int ctlOf(int rs, int wc) { return rs | wc; }		// 通过状态和线程数生成ctl
```

ThreadPoolExecutor的运行状态有5种，分别为：

| 运行状态   | 状态描述                                                     |
| :--------- | :----------------------------------------------------------- |
| RUNNING    | 接受新任务并处理排队的任务                                   |
| SHUTDOWN   | 关闭状态，不再接受新提交的任务，而是处理阻塞队列中排队的任务 |
| STOP       | 不接受新任务，不处理排队任务以及中断进行中的任务             |
| TIDYING    | 所有任务都已终止，workerCount为零，转换为状态TIDYING的线程将运行terminated()挂钩方法 |
| TERMINATED | 在terminated()方法执行完后进入该状态                         |

RunState随时间单调增加，但不必达到每个状态。

![ThreadPoolLifeCycle](D:%5Cternence%5CRoadToLearning%5Cimage%5CThreadPoolLifeCycle.png)

#### 任务执行机制

##### 任务调度

任务调度是线程池的主要入口，当用户提交了一个任务，接下来这个任务将如何执行都是由这个阶段决定的。

首先，所有任务的调度都是由execute方法完成的，这部分完成的工作是：检测现在线程池的运行状态、运行线程数、运行策略，决定接下来执行的流程，是直接申请线程执行，或是缓冲到队列中执行，亦或是直接拒绝该任务。其执行过程如下：

1. 首先检测线程池运行状态，如果不是RUNNING，则直接拒绝，线程池要保证在RUNNING的状态下执行任务。
2. 如果workerCount < corePoolSize，则创建并启动一个线程来执行新提交的任务。
3. 如果workerCount >= corePoolSize，且线程池内的阻塞队列未满，则将任务添加到该阻塞队列中。
4. 如果workerCount >= corePoolSize && workerCount < maximumPoolSize，且线程池内的阻塞队列已满，则创建并启动一个线程来执行新提交的任务。
5. 如果workerCount >= maximumPoolSize，并且线程池内的阻塞队列已满, 则根据拒绝策略来处理该任务, 默认的处理方式是直接抛异常。

![](D:%5Cternence%5CRoadToLearning%5Cimage%5CTaskSchedulingProcess.png)

##### 任务缓冲

任务缓冲模块是线程池能够管理任务的核心部分。线程池的本质是对任务和线程的管理，而做到这一点最关键的思想就是将任务和线程两者解耦，不让两者直接关联，才可以做后续的分配工作。线程池中是以生产者消费者模式，通过一个阻塞队列来实现的。阻塞队列缓存任务，工作线程从阻塞队列中获取任务。

阻塞队列（BlockingQueue）是一个支持两个附加操作的队列。这两个附加的操作是：在队列为空时，获取元素的线程会等待队列变为非空。当队列满时，存储元素的线程会等待队列可用。阻塞队列常用于生产者和消费者的场景，生产者是往队列里添加元素的线程，消费者是从队列里拿元素的线程。阻塞队列就是生产者存放元素的容器，而消费者也只从容器里拿元素。

![](D:%5Cternence%5CRoadToLearning%5Cimage%5CBlockingQueue.png)

使用不同的队列可以实现不一样的任务存取策略。

| 名称                  | 描述                                                         |
| --------------------- | ------------------------------------------------------------ |
| ArrayBlockingQueue    | 一个由数组实现的有界阻塞队列。该队列对元素FIFO（先进先出）进行排序。支持公平锁和非公平锁 |
| LinkedBlockingQueue   | 一个由链表结构组成的有界队列。该队列对元素FIFO（先进先出）进行排序。队列的默认长度为Integer.MAX_VALUE，所以默认创建的该队列由容量危险。 |
| PriorityBlockingQueue | 一个支持线程优先级排序的无界队列。默认自然序进行排序，也可以自定义实现compareTo()方法来指定元素排序规则，不能保证同优先级元素的顺序。 |
| DelayQueue            | 一个实现PriorityBlockingQueue实现延迟获取的无界队列。在创建元素时，可以指定多久才可以从队列中获取当前元素。只有延时期满后才能从队列中获取元素。 |
| SynchronousQueue      | 一个不存储元素的阻塞队列。每一个put操作必须等待take操作，否则不能添加元素。支持公平锁和非公平锁。SynchronousQueue的一个使用场景是在线程池。Executors.newCachedThreadPool()就是用了SynchronousQueue，这个线程池根据需要（新任务到来时）创建新的线程，如果有空闲线程则会重复使用。线程空闲了60秒就会被回收。 |
| LinkedTransferQueue   | 一个由链表结构组成的无界阻塞队列，相当于其他队列，LinkedTransferQueue队列多了transfer和tryTransfer方法。 |
| LinkedBlockingDeque   | 一个由链表结构组成的双向阻塞队列。队列头部和尾部都可以添加和移除元素，多线程并发时，可以将锁的竞争最多降到一般。 |

##### 任务申请

任务的执行有两种可能：一种是任务直接由新创建的线程执行。另一种是线程从任务队列中获取任务然后执行，执行完任务的空闲线程会再次去从队列中申请任务再去执行。第一种情况仅出现在线程初始创建的时候，第二种是线程获取任务绝大多数的情况。

线程需要从任务缓存模块中不断地取任务执行，帮助线程从阻塞队列中获取任务，实现线程管理模块和任务管理模块之间的通信。这部分策略由getTask方法实现，其执行流程如下图所示：

![](D:%5Cternence%5CRoadToLearning%5Cimage%5CGetTask.png)

getTask这部分进行了多次判断，为的是控制线程的数量，使其符合线程池的状态。如果线程池现在不应该持有那么多线程，则会返回null值。工作线程Worker会不断接收新任务去执行，而当工作线程Worker接收不到任务的时候，就会开始被回收。

##### 任务拒绝

任务拒绝模块是线程池的保护部分，线程池有一个最大的容量，当线程池的任务缓存队列已满，并且线程池中的线程数目达到maximumPoolSize时，就需要拒绝掉该任务，采取任务拒绝策略，保护线程池。

拒绝策略是一个接口，其设计如下：

```java
public interface RejectedExecutionHandler {
    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
}
```

用户可以通过实现这个接口去定制拒绝策略，也可以选择JDK提供的四种已有拒绝策略，其特点如下：

| 名称                                   | 描述                                                         |
| -------------------------------------- | ------------------------------------------------------------ |
| ThreadPoolExecutor.AbortPolicy         | 丢弃任务并抛出RejectedExecutionException异常。这是线程池默认的拒绝策略，在任务不能再提交的时候，抛出异常，及时反馈程序运行状态。如果是比较关键的业务，推荐使用此拒绝策略，这样在系统不能承载更大的并发量的时候，能够及时的通过异常发现。 |
| ThreadPoolExecutor.DiscardPolicy       | 静默丢弃任务，不抛出异常。使用此策略可能会使我们无法发现系统的异常状态。建议是一些无关紧要的业务采用此策略。 |
| ThreadPoolExecutor.DiscardOldestPolicy | 丢弃队列最前面的任务，然后重新提交被拒绝的任务。是否要采用此种拒绝策略，还得根据实际业务是否允许丢弃老任务来衡量。 |
| ThreadPoolExecutor.CallerRunsPolicy    | 由调用线程（提交任务的线程）处理该任务。这种情况是需要让所有任务都执行完毕，适合大量计算的任务类型去执行，多线程仅仅是增大吞吐量的手段，最终必须要让每个任务都执行完毕。 |

#### Worker线程管理

##### Worker线程

线程池为了掌握线程的状态并维护线程的生命周期，设计了线程池内的工作线程Worker。

```java
private final class Worker extends AbstractQueuedSynchronizer implements Runnable{
    final Thread thread;//Worker持有的线程
    Runnable firstTask;//初始化的任务，可以为null
}
```

Worker这个工作线程，实现了Runnable接口，并持有一个线程thread，一个初始化的任务firstTask。thread是在调用构造方法时通过ThreadFactory来创建的线程，可以用来执行任务；firstTask用它来保存传入的第一个任务，这个任务可以有也可以为null。如果这个值是非空的，那么线程就会在启动初期立即执行这个任务，也就对应核心线程创建时的情况；如果这个值是null，那么就需要创建一个线程去执行任务列表（workQueue）中的任务，也就是非核心线程的创建。

![](D:%5Cternence%5CRoadToLearning%5Cimage%5CWorker.png)

线程池需要管理线程的生命周期，需要在线程长时间不运行的时候进行回收。线程池使用一张Hash表去持有线程的引用，这样可以通过添加引用、移除引用这样的操作来控制线程的生命周期。这个时候重要的就是如何判断线程是否在运行。

Worker是通过继承AQS，使用AQS来实现独占锁这个功能。没有使用可重入锁ReentrantLock，而是使用AQS，为的就是实现不可重入的特性去反应线程现在的执行状态。

1.lock方法一旦获取了独占锁，表示当前线程正在执行任务中。 2.如果正在执行任务，则不应该中断线程。 3.如果该线程现在不是独占锁的状态，也就是空闲的状态，说明它没有在处理任务，这时可以对该线程进行中断。 4.线程池在执行shutdown方法或tryTerminate方法时会调用interruptIdleWorkers方法来中断空闲的线程，interruptIdleWorkers方法会使用tryLock方法来判断线程池中的线程是否是空闲状态；如果线程是空闲状态则可以安全回收。

在线程回收过程中就使用到了这种特性，回收过程如下图所示：

![](D:%5Cternence%5CRoadToLearning%5Cimage%5CThreadPoolGC.png)

##### Worker线程增加