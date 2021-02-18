# DataStore

> Jetpack的成员DataStore，主要用来替换SharedPreferences，DataStore是基于Flow实现的，一种新的数据存储方案。

DataStore提供了两种实现方式：

- Proto DataStore：存储类的对象（typed objects），通过protocol buffers将对象 **序列化** 存储在本地。
- Preferences DataStore：以键值对的形式存储在本地和 SharedPreferences 类似，但是 DataStore 是基于 Flow 实现的，不会阻塞主线程，并且保证类型安全。

### Preferences DataStore

> Preferences DataStore主要是为了解决SharedPreferences的坑。

- 那些年我们所经历的 SharedPreferences 坑？

- 为什么需要 DataStore？它为我们解决了什么问题？

- 如何在项目中使用 DataStore？

- 如何迁移 SharedPreferences 到 DataStore？

- MMKV、DataStore、SharedPreferences 的不同之处?

#### 那些年我们所经历的 SharedPreferences 坑？

- 通过 `getXXX()` 方法获取数据，可能会导致主线程阻塞

- SharedPreference 不能保证类型安全

- SharedPreference 加载的数据会一直留在内存中，浪费内存

- `apply()` 方法虽然是异步的，可能会发生 ANR，在 8.0 之前和 8.0 之后实现各不相同

- `apply()` 方法无法获取到操作成功或者失败的结果

##### `getXXX()` 方法可能会导致主线程阻塞

所有 `getXXX()` 方法都是同步的，在主线程调用 `get` 方法，必须等待 SP 加载完毕，会导致主线程阻塞。

```java
val sp = getSharedPreferences("ByteCode", Context.MODE_PRIVATE) // 异步加载 SP 文件内容
sp.getString("jetpack", ""); // 等待 SP 加载完毕
```

调用 `getSharedPreferences()` 方法，最终会调用  `SharedPreferencesImpl#startLoadFromDisk()` 方法开启一个线程异步读取数据。
 **frameworks/base/core/java/android/app/SharedPreferencesImpl.java**

```java
private final Object mLock = new Object();
private boolean mLoaded = false;
private void startLoadFromDisk() {
    synchronized (mLock) {
        mLoaded = false;
    }
    new Thread("SharedPreferencesImpl-load") {
        public void run() {
            loadFromDisk();
        }
    }.start();
}
```

开启一个线程异步读取数据，当我们正在读取一个比较大的数据，还没读取完，接着调用 `getXXX()` 方法。

```java
public String getString(String key, @Nullable String defValue) {
    synchronized (mLock) {
        awaitLoadedLocked();
        String v = (String)mMap.get(key);
        return v != null ? v : defValue;
    }
}

private void awaitLoadedLocked() {
    ......
    while (!mLoaded) {
        try {
            mLock.wait();
        } catch (InterruptedException unused) {
        }
    }
    ......
}
```

在同步方法内调用了 `wait()` 方法，会一直等待 `getSharedPreferences()` 方法开启的线程读取完数据才能继续往下执行，如果读取几 KB 的数据还好，假设读取一个大的文件，势必会造成主线程阻塞。

##### SP 不能保证类型安全

调用 `getXXX()` 方法的时候，可能会出现 ClassCastException 异常，因为使用相同的 key 进行操作的时候，`putXXX` 方法可以使用不同类型的数据覆盖掉相同的 key。

```java
val key = "jetpack"
val sp = getSharedPreferences("ByteCode", Context.MODE_PRIVATE) // 异步加载 SP 文件内容

sp.edit { putInt(key, 0) } // 使用 Int 类型的数据覆盖相同的 key
sp.getString(key, ""); // 使用相同的 key 读取 Sting 类型的数据
```

使用 Int 类型的数据覆盖掉相同的 key，然后使用相同的 key 读取 Sting 类型的数据，编译正常，但是运行会出现以下异常。

**java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String**

##### SP加载的数据会一直留在内存中

通过 `getSharedPreferences()` 方法加载的数据，最后会将数据存储在静态的成员变量中。

```java
// 调用 getSharedPreferences 方法，最后会调用 getSharedPreferencesCacheLocked 方法
public SharedPreferences getSharedPreferences(File file, int mode) {
    ......
    final ArrayMap<File, SharedPreferencesImpl> cache = getSharedPreferencesCacheLocked();
    return sp;
}

// 通过静态的 ArrayMap 缓存 SP 加载的数据
private static ArrayMap<String, ArrayMap<File, SharedPreferencesImpl>> sSharedPrefsCache;

// 将数据保存在 sSharedPrefsCache 中
private ArrayMap<File, SharedPreferencesImpl> getSharedPreferencesCacheLocked() {
    ......
    
    ArrayMap<File, SharedPreferencesImpl> packagePrefs = sSharedPrefsCache.get(packageName);
    if (packagePrefs == null) {
        packagePrefs = new ArrayMap<>();
        sSharedPrefsCache.put(packageName, packagePrefs);
    }

    return packagePrefs;
}
```

通过静态的 ArrayMap 缓存每一个 SP 文件，而每个 SP 文件内容通过 Map 缓存键值对数据，这样数据会一直留在内存中，浪费内存。

##### `apply()` 方法是异步的，可能会发生 ANR

`apply()` 方法是异步的，本身是不会有任何问题，但是当生命周期处于  `handleStopService()` 、 `handlePauseActivity()` 、 `handleStopActivity()`  的时候会一直等待 `apply()` 方法将数据保存成功，否则会一直等待，从而阻塞主线程造成 ANR，分析一下为什么异步方法还会阻塞主线程，先来看看 `apply()` 方法的实现。

**frameworks/base/core/java/android/app/SharedPreferencesImpl.java**

```java
public void apply() {
    final long startTime = System.currentTimeMillis();

    final MemoryCommitResult mcr = commitToMemory();
    final Runnable awaitCommit = new Runnable() {
            @Override
            public void run() {
                mcr.writtenToDiskLatch.await(); // 等待
                ......
            }
        };
    // 将 awaitCommit 添加到队列 QueuedWork 中
    QueuedWork.addFinisher(awaitCommit);

    Runnable postWriteRunnable = new Runnable() {
            @Override
            public void run() {
                awaitCommit.run();
                QueuedWork.removeFinisher(awaitCommit);
            }
        };
    // 8.0 之前加入到一个单线程的线程池中执行
    // 8.0 之后加入 HandlerThread 中执行写入任务
    SharedPreferencesImpl.this.enqueueDiskWrite(mcr, postWriteRunnable);
}
```

- 将一个 awaitCommit 的  Runnable 任务，添加到队列 QueuedWork 中，在 awaitCommit 中会调用 `await()` 方法等待，在 `handleStopService` 、 `handleStopActivity` 等等生命周期会以这个作为判断条件，等待任务执行完毕

- 将一个 postWriteRunnable 的  Runnable 写任务，通过 `enqueueDiskWrite` 方法，将写入任务加入到队列中，而写入任务在一个线程中执行

**注意：在 8.0 之前和 8.0 之后 `enqueueDiskWrite()` 方法实现逻辑各不相同**

在 8.0 之前调用 `enqueueDiskWrite()` 方法，将写入任务加入到 **单个线程的线程池** 中执行，如果 `apply()` 多次的话，任务将会依次执行，效率很低。

```java
// android-7.0.0_r34: frameworks/base/core/java/android/app/SharedPreferencesImpl.java
private void enqueueDiskWrite(final MemoryCommitResult mcr,
                              final Runnable postWriteRunnable) {
    ......
    QueuedWork.singleThreadExecutor().execute(writeToDiskRunnable);
}

// android-7.0.0_r34: frameworks/base/core/java/android/app/QueuedWork.java
public static ExecutorService singleThreadExecutor() {
    synchronized (QueuedWork.class) {
        if (sSingleThreadExecutor == null) {
            sSingleThreadExecutor = Executors.newSingleThreadExecutor();
        }
        return sSingleThreadExecutor;
    }
}
```

通过 `Executors.newSingleThreadExecutor()` 方法创建了一个 **单个线程的线程池**，因此任务是串行的，通过 `apply()` 方法创建的任务，都会添加到这个线程池内。

在 8.0 之后将写入任务加入到 LinkedList 链表中，在 HandlerThread 中执行写入任务。

```java
// android-10.0.0_r14: frameworks/base/core/java/android/app/SharedPreferencesImpl.java
private void enqueueDiskWrite(final MemoryCommitResult mcr,
                              final Runnable postWriteRunnable) {
    ......
    QueuedWork.queue(writeToDiskRunnable, !isFromSyncCommit);
}

// android-10.0.0_r14: frameworks/base/core/java/android/app/QueuedWork.java

private static final LinkedList<Runnable> sWork = new LinkedList<>();

public static void queue(Runnable work, boolean shouldDelay) {
    Handler handler = getHandler(); // 获取 handlerThread.getLooper() 生成 Handler 对象
    synchronized (sLock) {
        sWork.add(work); // 将写入任务加入到 LinkedList 链表中

        if (shouldDelay && sCanDelay) {
            handler.sendEmptyMessageDelayed(QueuedWorkHandler.MSG_RUN, DELAY);
        } else {
            handler.sendEmptyMessage(QueuedWorkHandler.MSG_RUN);
        }
    }
}
```

在 8.0 之后通过调用 `handlerThread.getLooper()` 方法生成 Handler，任务都会在 HandlerThread 中执行，所有通过 `apply()` 方法创建的任务，都会添加到 LinkedList 链表中。

当生命周期处于 `handleStopService()` 、 `handlePauseActivity()` 、 `handleStopActivity()` 的时候会调用 `QueuedWork.waitToFinish()` 会等待写入任务执行完毕。

```java
public void handlePauseActivity(IBinder token, boolean finished, boolean userLeaving,
        int configChanges, PendingTransactionActions pendingActions, String reason) {
        ......
        // 确保写任务都已经完成
        QueuedWork.waitToFinish();
        ......
    }
}
```

正如你所看到的在 `handlePauseActivity()` 方法中，调用了 `QueuedWork.waitToFinish()` 方法，会等待所有的写入执行完毕，Google 在 8.0 之后对这个方法做了很大的优化，一起来看一下 8.0 之前和 8.0 之后的区别。

**注意：在 8.0 之前和 8.0 之后 `waitToFinish()` 方法实现逻辑各不相同**

在 8.0 之前 `waitToFinish()` 方法只做了一件事，会一直等待写入任务执行完毕。

**frameworks/base/core/java/android/app/QueuedWork.java**

```java
private static final ConcurrentLinkedQueue<Runnable> sPendingWorkFinishers =
        new ConcurrentLinkedQueue<Runnable>();
        
public static void waitToFinish() {
    Runnable toFinish;
    while ((toFinish = sPendingWorkFinishers.poll()) != null) {
        toFinish.run(); // 相当于调用 `mcr.writtenToDiskLatch.await()` 方法
    }
}
```

- `sPendingWorkFinishers` 是 ConcurrentLinkedQueue 实例，`apply` 方法会将写入任务添加到 `sPendingWorkFinishers` 队列中，在 **单个线程的线程池** 中执行写入任务，线程的调度并不由程序来控制，也就是说当生命周期切换的时候，任务不一定处于执行状态

- `toFinish.run()` 方法，相当于调用 `mcr.writtenToDiskLatch.await()` 方法，会一直等待

- `waitToFinish()` 方法就做了一件事，会一直等待写入任务执行完毕，其它什么都不做，当有很多写入任务，会依次执行，当文件很大时，效率很低，造成 ANR 就不奇怪了

在 8.0 之后 `waitToFinish()` 方法做了很大的优化，当生命周期切换的时候，会主动触发任务的执行，而不是一直在等着。

**frameworks/base/core/java/android/app/QueuedWork.java**

```java
private static final LinkedList<Runnable> sFinishers = new LinkedList<>();
public static void waitToFinish() {
    ......
    try {
        processPendingWork(); // 主动触发任务的执行
    } finally {
        StrictMode.setThreadPolicy(oldPolicy);
    }

    try {
        // 等待任务执行完毕
        while (true) {
            Runnable finisher;

            synchronized (sLock) {
                finisher = sFinishers.poll(); // 从 LinkedList 中取出任务
            }

            if (finisher == null) { // 当 LinkedList 中没有任务时会跳出循环
                break;
            }

            finisher.run(); // 相当于调用 `mcr.writtenToDiskLatch.await()`
        }
    } 
    
    ......
}
```

在 `waitToFinish()` 方法中会主动调用 `processPendingWork()` 方法触发任务的执行，在 HandlerThread 中执行写入任务。

另外还做了一个很重要的优化，当调用 `apply()` 方法的时候，执行磁盘写入，都是全量写入，在 8.0 之前，调用 N 次 `apply()` 方法，就会执行 N 次磁盘写入，在 8.0 之后，`apply()` 方法调用了多次，只会执行最后一次写入，通过版本号来控制的。

SharedPreferences 的另外一个缺点就是 `apply()` 方法无法获取到操作成功或者失败的结果，而 `commit()` 方法是可以接收 MemoryCommitResult 里面的一个 boolean 参数作为结果，来看一下它们的方法签名。

```java
public void apply() { ... }

public boolean commit() { ... }
```

##### SP 不能用于跨进程通信

在创建 SP 实例的时候，需要传入一个 `mode`，如下所示：

```java
val sp = getSharedPreferences("ByteCode", Context.MODE_PRIVATE) 
```

Context 内部还有一个 `mode` 是 `MODE_MULTI_PROCESS`

```java
public SharedPreferences getSharedPreferences(File file, int mode) {
    if ((mode & Context.MODE_MULTI_PROCESS) != 0 ||
        getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.HONEYCOMB) {
        // 重新读取 SP 文件内容
        sp.startReloadIfChangedUnexpectedly();
    }
    return sp;
}
```

在这里就做了一件事，当遇到 `MODE_MULTI_PROCESS` 的时候，会重新读取 SP 文件内容，并不能用 SP 来做跨进程通信。

#### DataStore 解决了什么问题

> Preferences DataStore 主要用来替换 SharedPreferences，Preferences DataStore 解决了 SharedPreferences 带来的所有问题

**Preferences DataStore 相比于 SharedPreferences 优点**

- DataStore 是基于 Flow 实现的，所以保证了在主线程的安全性
- 以事务方式处理更新数据，事务有四大特性（原子性、一致性、 隔离性、持久性）
- 没有 `apply()` 和 `commit()` 等等数据持久的方法
- 自动完成 SharedPreferences 迁移到 DataStore，保证数据一致性，不会造成数据损坏
- 可以监听到操作成功或者失败结果

另外 Jetpack DataStore 提供了 Proto DataStore 方式，用于存储类的对象（typed objects ），通过 protocol buffers 将对象序列化存储在本地。

**Note:**

Preferences DataStore 只支持 `Int` , `Long` , `Boolean` , `Float` , `String` 键值对数据，适合存储简单、小型的数据，并且**不支持局部更新**，如果修改了其中一个值，整个文件内容将会被重新序列化，如果要局部更新，建议使用Room。

#### 在项目中使用Preferences DataStore

Preferences DataStore主要应用在MVVM当中的Repository层，在项目中使用Preferences DataStore只需要4步。

1. 需要添加Preferences DataStore依赖

   ```groovy
   implementation "androidx.datastore:datastore-preferences:1.0.0-alpha01"
   ```

2. 构建DataStore

   ```java
   private val PREFERENCE_NAME = "DataStore"
   var dataStore: DataStore<Preferences> = context.createDataStore(
       name = PREFERENCE_NAME
   ```

3. 从Preferences DataStore中读取数据

   Preferences DataStore 以键值对的形式存储在本地，所以首先我们应该定义一个 Key.

   ```java
   val KEY_BYTE_CODE = preferencesKey<Boolean>("ByteCode")
   ```

   这里和使用SharedPreferences 的有点不一样，在 Preferences DataStore 中 Key 是一个 `Preferences.Key<T>` 类型，只支持 `Int` , `Long` , `Boolean` , `Float` , `String`，源码如下所示：

   ```java
   inline fun <reified T : Any> preferencesKey(name: String): Preferences.Key<T> {
       return when (T::class) {
           Int::class -> {
               Preferences.Key<T>(name)
           }
           String::class -> {
               Preferences.Key<T>(name)
           }
           Boolean::class -> {
               Preferences.Key<T>(name)
           }
           Float::class -> {
               Preferences.Key<T>(name)
           }
           Long::class -> {
               Preferences.Key<T>(name)
           }
           ...... // 如果是其他类型就会抛出异常
       }
   }
   ```

   当我们定义好 Key 之后，就可以通过 `dataStore.data` 来获取数据

   ```java
   override fun readData(key: Preferences.Key<Boolean>): Flow<Boolean> =
       dataStore.data
           .catch {
               // 当读取数据遇到错误时，如果是 `IOException` 异常，发送一个 emptyPreferences 来重新使用
               // 但是如果是其他的异常，最好将它抛出去，不要隐藏问题
               if (it is IOException) {
                   it.printStackTrace()
                   emit(emptyPreferences())
               } else {
                   throw it
               }
           }.map { preferences ->
               preferences[key] ?: false
           }
   ```

   - Preferences DataStore 是基于 Flow 实现的，所以通过 `dataStore.data` 会返回一个 `Flow<T>`，每当数据变化的时候都会重新发出

   - `catch` 用来捕获异常，当读取数据出现异常时会抛出一个异常，如果是 `IOException` 异常，会发送一个 `emptyPreferences()` 来重新使用，如果是其他异常，最好将它抛出去

4. 向 Preferences DataStore 中写入数据

   在 Preferences DataStore 中是通过 `DataStore.edit()` 写入数据的，`DataStore.edit()` 是一个 suspend 函数，所以只能在协程体内使用，每当遇到 suspend 函数以挂起的方式运行，并不会阻塞主线程。

   **以挂起的方式运行，不会阻塞主线程** ：也就是协程作用域被挂起, 当前线程中协程作用域之外的代码不会阻塞。

   创建一个 suspend 函数，然后调用 `DataStore.edit()` 写入数据。

   ```java
   override suspend fun saveData(key: Preferences.Key<Boolean>) {
       dataStore.edit { mutablePreferences ->
           val value = mutablePreferences[key] ?: false
           mutablePreferences[key] = !value
       }
   }
   ```

#### 迁移SharedPreferences 到 DataStore

迁移 SharedPreferences 到 DataStore 只需要 2 步。

- 在构建 DataStore 的时候，需要传入一个 SharedPreferencesMigration

```
dataStore = context.createDataStore(
    name = PREFERENCE_NAME,
    migrations = listOf(
        SharedPreferencesMigration(
            context,
            SharedPreferencesRepository.PREFERENCE_NAME
        )
    )
)
复制代码
```

- 当 DataStore 对象构建完了之后，需要执行一次读取或者写入操作，即可完成 SharedPreferences 迁移到  DataStore，当迁移成功之后，会自动删除 SharedPreferences 使用的文件

**Note：** 只从 SharedPreferences 迁移一次，因此一旦迁移成功之后，应该停止使用 SharedPreferences。

