## Android Handler解析

> Android的消息机制主要是指Handler的运行机制，Handler的运行需要底层的MessageQueue和Looper的支撑。

- ### Handler实现机制

- ### 在任何地方new Handler都是什么线程下？

- ### Handler发消息给子线程，looper怎么启动？

- ### 在子线程中创建Handler报错是为什么?

- ### 如何在子线程创建Looper？

- ### 为什么通过Handler能实现线程的切换？



#### Handler机制

Handler机制中有四个对象，Handler、Message、MessageQueue、Looper。

Handler负责消息的发送和处理，Message是消息对象，类似于链表中的一个结点，MessageQueue是消息队列，用于存放消息对象的数据结构，Looper是消息队列的处理者。

当我们在某个线程中调用new Handler()的时候，使用当前线程的 Looper 创建 Handler，当前线程的Looper存在于线程局部变量ThreadLocal中。在使用Handler之前，需要调用Looper.prepare()方法实例化Looper，并将Looper放置在当前线程的局部变量ThreadLocal中，此时调用Looper的构造方法，并在初始化方法中，初始化MessageQueue，然后调用`Looper.loop()`开启消息循环。我们可以使用`Looper.getMainLooper()`获取到主线程的Looper，并使用它来创建Handler。

当实例化Looper的时候，同时会实例化一个MessageQueue，MessageQueue实例化的时候，会调用Native层的方法nativeInit()在Native层实例化一个MessageQueue还有Looper。 Java 层的 Looper 和 Native 层的 Looper 之间使用 epoll 进行通信。当调用 Looper 的`loop()`方法的时候会启动一个循环来对消息进行处理。Java 层的 MessageQueue中没有消息的时候，Native 层的 Looper 会使其进入睡眠状态，当有消息到来的时候再将其唤醒起来处理消息，以节省 CPU。

##### handler内存泄漏及解决办法：

如果 Handler 不是静态内部类，Handler 会持有 Activity 的匿名引用。当 Activity 要被回收时，因为 Handler 在做耗时操作没有被释放，Handler Activity 的引用不能被释放导致 Activity 没有被回收停留在内存中造成内存泄露。

解决方法是：

1. 将 Handler 设为静态内部类；
2. 使 Handler 持有 Activity 的弱引用；
3. 在 Activity 生命周期 `onDestroy()` 中调用 `Handler.removeCallback()` 方法。

**Handler.post() 的逻辑在哪个线程执行的，是由 Looper 所在线程还是 Handler 所在线程决定的？**

这里的 Handler 所在的线程指的是调用 Handler 的 `post()` 方法时 Handler 所在的线程

post所在的线程由Looper所在的线程决定，最终逻辑是在`Looper.loop()`方法中，从MessageQueue中拿出Message，并执行其逻辑。这是在 Looper 中执行的，因此由 Looper 所在线程决定。

不论是调用`send()`还是`post()`，最后都会执行到`enqueueMessage()`

**Looper 和 Handler 一定要处于一个线程吗？子线程中可以用 MainLooper 去创建 Handler吗？**

Looper和Handler不需要在一个线程中。默认情况下会通过ThreadLocal去拿当前线程对应的Looper，但是我们可以显式的指定一个Looper来创建Handler。比如，想要在子线程中发送消息到主线程，可以通过

```java
Handler handler = new Handler(Looper.getMainLooper());
```

**Handler.post() 方法发送的是同步消息吗？可以发送异步消息吗？**

用户层面发送的都是同步消息，不能发送异步消息；异步消息只能由系统发送。

**MessageQueue.next() 会因为发现了延迟消息，而进行阻塞。那么为什么后面加入的非延迟消息没有被阻塞呢？**

调用 `MessageQueue.next()` 方法的时候会调用 Native 层的 `nativePollOnce()` 方法进行精准时间的阻塞。在 Native 层，将进入 `pullInner()` 方法，使用 `epoll_wait` 阻塞等待以读取管道的通知。如果没有从 Native 层得到消息，那么这个方法就不会返回。此时主线程会释放 CPU 资源进入休眠状态。

当我们加入消息的时候，会调用 `MessageQueue.enqueueMessage()` 方法，添加完 Message 后，如果消息队列被阻塞，则会调用 Native 层的 `nativeWake()` 方法去唤醒。它通过向管道中写入一个消息，结束上述阻塞，触发上面提到的 `nativePollOnce()` 方法返回，好让加入的 Message 得到分发处理。

**MessageQueue.enqueueMessage() 方法的原理，如何进行线程同步的？**

`MessageQueue.enqueueMessage()` 使用 synchronized 代码块去进行同步。

##### next() 是如何处理延迟消息的？

`MessageQueue.next()`方法中，通过循环遍历，不停的获取当前时间戳来于msg.when比较，直到小于当前时间戳为止。

##### Handler 的 `dispatchMessage()` 分发消息的处理流程？

使用 Handler 的时候我们会覆写 Handler 的 `handleMessage()` 方法。当我们调用该 Handler 的 `send()` 或者 `post()` 发送一个消息的时候，发送的信息会被包装成 Message，并且将该 Message 的 target 指向当前 Handler，这个消息会被放进 Looper 的 MQ 中。然后在 Looper 的循环中，取出这个 Message，并调用它的 target Handler，也就是我们定义的 Handler 的 `dispatchMessage()` 方法处理消息，此时会调用到 Handler 的  `handleMessage()` 方法处理消息，并回调 Callback.

##### Handler 为什么要有 Callback 的构造方法？

当 Handler 在消息队列中被执行的时候会直接调用 Handler 的 `dispatchMessage()` 方法回调 Callback.

##### Handler构造方法中通过 `Looper.myLooper()` 是如何获取到当前线程的 Looper 的？

从ThreadLocal中获取，`ThreadLocal.get()`方法，会通过当前线程对象获取到ThreadLocalMap，如果获取到的map不会null，再从`Entry.getEntry`获取Looper对象。否则去重新创建一个。

**Looper.loop() 的源码流程?**

1. 获取到 Looper 和消息队列；
2. for 无限循环，阻塞于消息队列的 `next()` 方法；
3. 取出消息后调用 `msg.target.dispatchMessage(msg)` 进行消息分发。

##### Android 如何保证一个线程最多只能有一个 Looper？如何保证只有一个 MessageQueue

通过保证只有一个 Looper 来保证只有一个 MessageQueue. 在一个线程中使用 Handler 之前需要使用 `Looper.prepare()` 创建 Looper，它会从ThreadLocal中获取，如果发现 ThreadLocal中已经存在 Looper，就抛异常。

##### Handler 消息机制中，一个 Looper 是如何区分多个 Handler 的？

根据消息的分发机制，Looper 不会区分 Handler，每个 Handler 会被添加到 Message 的 target 字段上面，Looper 通过调用 `Message.target.handleMessage()` 来让 Handler 处理消息。





#### EventBus线程切换原理

主要有两个问题：

1. 如何去判断当前发送事件的线程是否是主线程
2. 如何在接受事件时指定线程并执行

##### 如何判断是否在主线程发送

EventBus在初始化的时候会初始化一个MainThreadSupport对象，它会去获取主线程的Looper对象并存起来。在发送消息的时候，EventBus会取出当前线程的Looper对象对象与主线程Looper对象做比较，如果相同，说明是在主线程发送消息，如果不同，说明是在子线程发送消息。

```java
public interface MainThreadSupport {

    boolean isMainThread();

    Poster createPoster(EventBus eventBus);

    class AndroidHandlerMainThreadSupport implements MainThreadSupport {

        private final Looper looper;

        public AndroidHandlerMainThreadSupport(Looper looper) {
            this.looper = looper;
        }

        @Override
        public boolean isMainThread() {
            return looper == Looper.myLooper();
        }

        @Override
        public Poster createPoster(EventBus eventBus) {
            return new HandlerPoster(eventBus, looper, 10);
        }
    }
}
```

##### 怎么在指定的线程执行订阅者的方法

在找到订阅者之后，判断不同线程情况下执行订阅方法的逻辑基本都在postToSubscription()方法里面。

其中，invokeSubscriber()这个方法其实就是拿到订阅者的信息，直接执行订阅方法了(通过反射获取)。subscription对象中有一个SubscriberMethod对象，而SubscriberMethod这个对象基本上包含了订阅者的执行线程、订阅方法、是否粘性事件、优先级等等信息。

当threadMode是POSTING时，直接在当前线程执行，不做判断，也就是从哪个线程发送，就从哪个线程执行订阅方法；

**在主线程执行**

当threadMode为MAIN时，如果在主线程发送，直接在当前线程执行，没有问题。

如果不在主线程发送，会有一个**mainThreadPoster**将包含订阅者信息的对象加入队列。这个mainThreadPoster其实是Handler的子类，它利用Handler的消息机制，发送消息并在主线程接收消息，获取到订阅者的信息后在主线程处理事件，从而实现在子线程发送消息，在主线程处理事件。

**在子线程执行**

当threadMode为BACKGROUND时，如果不在主线程发送，直接执行，没有问题。

如果在主线程发送，这里有一个**backgroundPoster**将包含订阅者信息的对象加入队列。BackgroundPoster其实是Runnable的子类，在自己的run方法中不断从队列中取出订阅者对象，执行订阅方法。EventBus维护了一个线程池，BackgroundPoster会将自己丢到线程池中，执行自己的run方法，从而实现在在主线程发送事件，在子线程中执行订阅方法。