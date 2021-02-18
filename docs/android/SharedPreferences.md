# SharedPreferences

> `Sharedpreferences`是Android平台上一个轻量级的存储类，用来保存应用程序的各种配置信息，其本质是一个以“键-值”对的方式保存数据的xml文件。

**问题：**

1. `SharedPreferences`是如何保证线程安全的，其内部的实现用到了哪些锁？
2. 进程不安全是否会导致数据丢失？
3. 数据丢失时，其最终的屏障——文件备份机制是如何实现的？
4. 如何实现进程安全的`SharedPreferences`？

除此之外，站在 **设计者的角度 **上，还有一些与架构相关，且同样值得思考的问题：

1. 为什么`SharedPreferences`会有这些缺陷，如何对这些缺陷做改进的尝试？
2. 为什么不惜推倒重来，推出新的`DataStore`组件来代替前者？
3. 令`Google`工程师掣肘，时隔今日，这些缺陷依然存在的最根本性原因是什么？

想要解除这些困惑，就要从`SharedPreferences`本身的设计与实现来讲起了。

![](https://github.com/TernenceChen/Learn-Notes/blob/master/image/image-20201020172117705.png)

### SharedPreferences的前世今生

#### 设计与实现：建立基本结构

`SharedPreferences`是`Android`平台上 **轻量级的存储类**，用来保存`App`的各种配置信息，其本质是一个以 **键值对**（`key-value`）的方式保存数据的`xml`文件，其保存在`/data/data/shared_prefs`目录下。

这个 **轻量级的存储类** 建立了最基础的模型，通过`xml`中的键值对，将对应的数据保存到本地的文件中。这样，每次读取数据时，通过解析`xml`文件，得到指定`key`对应的`value`；每次更新数据，也通过文件中`key`更新对应的`value`。

#### 读操作的优化

通过这样的方式，虽然建立了一个最简单的 **文件存储系统**，但是性能实在不敢恭维，每次读取一个`key`对应的值都要重新对文件进行一次读的操作。显然需要尽量避免笨重的`I/O`操作。

因此对读操作进行了简单的优化，当`SharedPreferences`对象第一次通过`Context.getSharedPreferences()`进行初始化时，对`xml`文件进行一次读取，并将文件内所有内容（即所有的键值对）缓到内存的一个`Map`中，这样，接下来所有的读操作，只需要从这个`Map`中取就可以了：

```java
final class SharedPreferencesImpl implements SharedPreferences {
  private final File mFile;             // 对应的xml文件
  private Map<String, Object> mMap;     // Map中缓存了xml文件中所有的键值对
}
```

虽然节省了`I/O`的操作，但另一个视角分析，当`xml`中数据量过大时，这种 **内存缓存机制** 有可能会产生 **高内存占用 **的风险。

这也正是很多开发者诟病`SharedPreferences`的原因之一。

但从事物的两面性上来看，**高内存占用** 真的是设计的问题吗？

不尽然，因为`SharedPreferences`的设计初衷是数据的 **轻量级存储** ，对于类似应用的简单的配置项（比如一个`boolean`或者`int`类型），即使很多也并不会对内存有过高的占用；而对于复杂的数据（比如复杂对象序列化后的字符串），开发者更应该使用类似`Room`这样的解决方案，而非一股脑存储到`SharedPreferences`中。

因此对于「`SharedPreferences`会导致内存使用过高」的说法，更客观的总结应该是：

虽然 **内存缓存机制** 表面上看起来好像是一种 **空间换时间** 的权衡，实际上规避了短时间内频繁的`I/O`操作对性能产生的影响，而通过良好的代码规范，也能够避免该机制可能会导致内存占用过高的副作用，所以这种设计是 **值得肯定** 的。

#### 写操作的优化

针对写操作，设计者同样有设计了一系列的接口，以达到优化性能的目的。

对键值对进行更新是通过`mSharedPreferences.edit().putString().commit()`进行操作的——`edit()`是什么，`commit()`又是什么，为什么不单纯的设计初`mSharedPreferences.putString()`这样的接口？

设计者希望，在复杂的业务中，有时候一次操作会导致多个键值对的更新，这时，与其多次更新文件，我们更倾向将这些更新 **合并到一次写操作** 中，以达到性能的优化。

因此，对于`SharedPreferences`的写操作，设计者抽象出了一个`Editor`类，不管某次操作通过若干次调用`putXXX()`方法，更新了几个`xml`中的键值对，只有调用了`commit()`方法，最终才会真正写入文件：

```java
// 简单的业务，一次更新一个键值对
sharedPreferences.edit().putString().commit();

// 复杂的业务，一次更新多个键值对，仍然只进行一次IO操作（文件的写入）
Editor editor = sharedPreferences.edit();
editor.putString();
editor.putBoolean().putInt();
editor.commit();   // commit()才会更新文件
```

了解到这一点，应该明白，通过简单粗暴的封装，以达到类似`SPUtils.putXXX()`这种所谓代码量的节省，从而忽略了`Editor.commit()`的设计理念和使用场景，往往是不可取的，从设计上来讲，这甚至是一种 **倒退** 。

另外一个值得思考的角度是，本质上文件的`I/O`是一个非常重的操作，直接放在主线程中的`commit()`方法某些场景下会导致`ANR`（比如数据量过大），因此更合理的方式是应该将其放入子线程执行。

因此设计者还为`Editor`提供了一个`apply()`方法，用于异步执行文件数据的同步，并推荐开发者使用`apply()`而非`commit()`。

看起来`Editor`+`apply()`方法对写操作做了很大的优化，但更多的问题随之而来，比如子线程更新文件，必然会引发 **线程安全问题**；此外，`apply()`方法真的能够像我们预期的一样，能够避免`ANR`吗？答案是并不能。

#### 数据的更新&文件数量的权衡

> 随着业务复杂度的上升，需要面对新的问题是，`xml`文件中的数据量愈发庞大，一次文件的写操作成本也愈发高昂。

xml中数据是如何更新的？可以简单理解为 **全量更新** ——通过上文，我们知道`xml`文件中的数据会缓存到内存的`mMap`中，每次在调用`editor.putXXX()`时，实际上会将新的数据存入在`mMap`，当调用`commit()`或`apply()`时，最终会将`mMap`的所有数据全量更新到`xml`文件里。

由此可见，`xml`中数据量的大小，的确会对 **写操作** 的成本有一定的影响，因此，设计者更建议将 **不同业务模块的数据分文件存储** ，即根据业务将数据存放在不同的`xml`文件中。

因此，不同的`xml`文件应该对应不同的`SharedPreferences`对象，如果想要对某个`xml`文件进行操作，就通过传不同的文件标识符，获取对应的`SharedPreferences`：

```java
@Override
public SharedPreferences getSharedPreferences(String name, int mode) {
  // name参数就是文件名，通过不同文件名，获取指定的SharedPreferences对象
}
```

因此，当`xml`文件过大时，应该考虑根据业务，细分为若干个小的文件进行管理；但过多的小文件也会导致过多的`SharedPreferences`对象，不好管理且易混淆。实际开发中，应根据业务的需要进行对应的平衡。

### 线程安全问题

> `SharedPreferences`是线程安全的吗？

毫无疑问，`SharedPreferences`是线程安全的，但这只是对成品而言，对于我们目前的实现，显然还有一定的差距，如何保证线程安全呢？

#### 保证复杂流程代码的可读性

为了保证`SharedPreferences`是线程安全的，`Google`的设计者一共使用了3把锁：

锁排序规则：

- 在EditorImpl.mLock之前获取SharedPreferencesImpl.mLock
- 在EditorImpl.mLock之前获取mWritingToDiskLock

```java
final class SharedPreferencesImpl implements SharedPreferences {
  // 1、使用注释标记锁的顺序
  // Lock ordering rules:
  //  - acquire SharedPreferencesImpl.mLock before EditorImpl.mLock
  //  - acquire mWritingToDiskLock before EditorImpl.mLock

  // 2、通过注解标记持有的是哪把锁
  @GuardedBy("mLock")
  private Map<String, Object> mMap;

  @GuardedBy("mWritingToDiskLock")
  private long mDiskStateGeneration;

  public final class EditorImpl implements Editor {
    @GuardedBy("mEditorLock")
    private final Map<String, Object> mModified = new HashMap<>();
  }
}
```

对于这样复杂的类而言，如何提高代码的可读性？`SharedPreferencesImpl`做了一个很好的示范：**通过注释明确写明加锁的顺序，并为被加锁的成员使用`@GuardedBy`注解**。

对于简单的 **读操作** 而言，我们知道其原理是读取内存中`mMap`的值并返回，那么为了保证线程安全，只需要加一把锁保证`mMap`的线程安全即可：

```java
public String getString(String key, @Nullable String defValue) {
    synchronized (mLock) {
        String v = (String)mMap.get(key);
        return v != null ? v : defValue;
    }
}
```

那么，对于 **写操作** 而言，我们也能够通过一把锁达到线程安全的目的吗？

#### 保证写操作的线程安全

对于写操作而言，每次`putXXX()`并不能立即更新在`mMap`中，这是理所当然的，如果开发者没有调用`apply()`方法，那么这些数据的更新理所当然应该被抛弃掉，但是如果直接更新在`mMap`中，那么数据就难以恢复。

因此，`Editor`本身也应该持有一个`mEditorMap`对象，用于存储数据的更新；只有当调用`apply()`时，才尝试将`mEditorMap`与`mMap`进行合并，以达到数据更新的目的。

因此，这里我们还需要另外一把锁保证`mEditorMap`的线程安全，笔者认为，不和`mMap`公用同一把锁的原因是，在`apply()`被调用之前，`getXXX`和`putXXX`理应是没有冲突的。

```java
public final class EditorImpl implements Editor {
  @Override
  public Editor putString(String key, String value) {
      synchronized (mEditorLock) {
          mEditorMap.put(key, value);
          return this;
      }
  }
}
```

而当真正需要执行`apply()`进行写操作时，`mEditorMap`与`mMap`进行合并，这时必须通过2把锁保证`mEditorMap`与`mMap`的线程安全，保证`mMap`最终能够更新成功，最终向对应的`xml`文件中进行更新。

文件的更新理所当然也需要加一把锁：

```java
// SharedPreferencesImpl.EditorImpl.enqueueDiskWrite()
synchronized (mWritingToDiskLock) {
    writeToFile(mcr, isFromSyncCommit);
}
```

最终，我们一共通过使用了3把锁，对整个写操作的线程安全进行了保证。

> **Note:** 可参考SharedPreferencesImpl.EditorImple类的apply()源码。 

#### 摆脱不掉的ANR

`apply()`方法设计的初衷是为了规避主线程的`I/O`操作导致`ANR`问题的产生，那么，`ANR`的问题真得到了有效的解决吗？

并没有。经过优化，`SharedPreferences`的确是线程安全的，`apply()`的内部实现也的确将`I/O`操作交给了子线程，可以说其本身是没有问题的，而其原因归根到底则是`Android`的另外一个机制。

在`apply()`方法中，首先会创建一个等待锁，根据源码版本的不同，最终更新文件的任务会交给`QueuedWork.addFinisher()`去执行，当文件更新完毕后会释放锁。

但当`Activity.onStop()`以及`Service`处理`onStop`等相关方法时，则会执行 `QueuedWork.waitToFinish()`等待所有的等待锁释放，因此如果`SharedPreferences`一直没有完成更新任务，有可能会导致卡在主线程，最终超时导致`ANR`。

> 什么情况下`SharedPreferences`会一直没有完成任务呢？ 比如太频繁无节制的`apply()`，导致任务过多，这也侧面说明了`SPUtils.putXXX()`这种粗暴的设计的弊端。

无论是 commit 还是 apply 都会产生 ANR，但从 Android 之初到 Android8.0，Google 一直没有修复此 bug，我们贸然处理会产生什么问题呢。Google 在 Activity 和 Service 调用 onStop 之前阻塞主线程来处理 SP，我们能猜到的唯一原因是尽可能的保证数据的持久化。因为如果在运行过程中产生了 crash，也会导致 SP 未持久化，持久化本身是 IO 操作，也会失败。

### 进程安全问题

#### 如何保证进程安全

`SharedPreferences`是否进程安全呢？让我们打开`SharedPreferences`的源码，看一下最顶部类的注释：

```java
/**
 * ...
 * This class does not support use across multiple processes.
 * ...
 */
public interface SharedPreferences {
  // ...
}
```

由此，由于没有使用跨进程的锁，`SharedPreferences`是进程不安全的，在跨进程频繁读写会有数据丢失的可能，这显然不符合我们的期望。

那么，如何保证`SharedPreferences`进程的安全呢？

实现思路很多，比如使用文件锁，保证每次只有一个进程在访问这个文件；或者对于`Android`开发而言，`ContentProvider`作为官方倡导的跨进程组件，其它进程通过定制的`ContentProvider`用于访问`SharedPreferences`，同样可以保证`SharedPreferences`的进程安全；等等。

#### 文件损坏 & 备份机制

`SharedPreferences`再次迎来了新的挑战。

由于不可预知的原因，`xml`文件的 **写操作** 异常中止，`Android`系统本身的文件系统虽然有很多保护措施，但依然会有数据丢失或者文件损坏的情况。

作为设计者，如何规避这样的问题呢？答案是对文件进行备份，`SharedPreferences`的写入操作正式执行之前，首先会对文件进行备份，将初始文件重命名为增加了一个`.bak`后缀的备份文件：

```java
// 尝试写入文件
private void writeToFile(...) {
  if (!backupFileExists) {
      !mFile.renameTo(mBackupFile);
  }
}
```

这之后，尝试对文件进行写入操作，写入成功时，则将备份文件删除：

```java
// 写入成功，立即删除存在的备份文件
// Writing was successful, delete the backup file if there is one.
mBackupFile.delete();
```

反之，若因异常情况（比如进程被杀）导致写入失败，进程再次启动后，若发现存在备份文件，则将备份文件重名为源文件，原本未完成写入的文件就直接丢弃：

```java
// 从磁盘初始化加载时执行
private void loadFromDisk() {
    synchronized (mLock) {
        if (mBackupFile.exists()) {
            mFile.delete();
            mBackupFile.renameTo(mFile);
        }
    }
  }
```

现在，通过文件备份机制，我们能够保证数据只会丢失最后的更新，而之前成功保存的数据依然能够有效。

### 小结

相对于对组件之间单纯进行 **好** 和 **不好** 的定义，通过辩证的方式去看待和学习，依然能够有所收获。

#### ANR问题分析和解决

问题直接来自于在系统在主线程的几个生命周期中去等待任务列表执行完成，那么android为什么要这样设计呢？android的应用是被托管运行的，应用在运行过程中有可能被系统回收、杀死、或者用户主动杀死，其实是在一个不确定的环境中运行，`apply`提交的任务，不是立即执行的，而是会加入到列表中，在未来的某一个时刻去执行，那么就存在不确定性了，有可能在执行之前应用进程被杀死了，那么写入任务就失败了。所以就在应用进程的存续时，抓紧找到一些时机去完成写入磁盘的事情，也就是在上面的几个生命周期方法中。

这个设计整体上是没有大问题的，但是 `QueuedWork.waitToFinish` 的方法在老版的实现上存在很大的缺陷，它使得主线程只是在等待，而没有做推动，这种情况下导致应用出现anr,进而被用户或者系统杀死进程，这样写入任务还是不能执行完成，还影响用户体验，这个是得不偿失的。

#### SharedPreferences实践推荐

1. 在工作线程中写入sp时，直接调用 `commit `就可以，不必调用 `apply `,这种情况下，`commit `的开销更小
2. 在主线程中写入sp时，不要调用 `commit`，要调用 `apply`
3. sp对应的文件尽量不要太大，按照模块名称去读写对应的sp文件，而不是一个整个应用都读写一个sp文件
4. sp的适合读写轻量的、小的配置信息，不适合保存大数据量的信息，比如长串的json字符串。
5. 当有连续的调用PutXxx方法操作时（特别是循环中），当确认不需要立即读取时，最后一次调用 `commit `或 `apply` 即可。

### QueuedWork

> 用于跟踪尚未完成的或尚未完成的全局任务的内部工具类，新任务通过方法 `queue` 加入。添加 `finisher` 的 `runnables` ，由 `waitToFinish` 方法保证执行，用于保证任务已被处理完成。

这个类用于 **SharedPreference** 编辑后修改异步写入到磁盘，所以设计一个在 **Activity.onPause** 或类似地方等待写入操作机制，而这个机制也能用于其他功能。所有排队的异步任务都在一个独立、专用的线程上处理。

### Remarks

#### Reference documents

[SharedPreferences ANR问题分析和解决 & Android 8.0的优化](https://www.jianshu.com/p/3f64caa567e5)

[剖析 SharedPreference apply引起的ANR问题](https://mp.weixin.qq.com/s?__biz=MzI1MzYzMjE0MQ==&mid=2247484387&idx=1&sn=e3c8d6ef52520c51b5e07306d9750e70&scene=21#wechat_redirect)

[通过ContentProvider实现SharedPreferences进程共享数据](https://www.jianshu.com/p/3e551e3d4a8d)

[Android使用读写锁实现多进程安全的SharedPreferences](https://blog.csdn.net/qq_27512671/article/details/101445642)



