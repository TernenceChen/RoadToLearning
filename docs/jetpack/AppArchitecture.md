## Note架构分享

不应在应用组件中存储任何应用数据或状态，并且应用组件不应相互依赖。

Google在应用架构指南中，提出两个常见的架构原则：

**分离关注点**：基于界面的类应仅包含处理界面和操作系统交互的逻辑。应使这些类尽可能保持精简，这样可以避免许多与生命周期相关的问题。

**通过模型驱动界面**：最好是持久性模型。模型是负责处理应用数据的组件。独立于应用中View对象和应用组件，因此不受应用的生命周期以及相关的关注点的影响。

为什么最好是持久性模型呢？

- 如果Android操作系统销毁应用以释放资源，用户不会丢失数据。
- 当网络连接不稳定或不可用时，应用会继续工作。

应用所基于的模型类应明确定义数据管理职责，这样将使应用更可测试且更一致。



### 概览

![image-20210421105545669](https://raw.githubusercontent.com/TernenceChen/RoadToLearning/master/image/image-20210421105545669.png?token=AKPZIOFRKFP3YMFP4LJDUEDAQD47E)

每个组件仅依赖于其下一级的组件。

### 构建界面

**ViewModel** 为特定的界面组件提供数据，并包含数据处理业务逻辑，以与模型进行通信。ViewModel可以调用其他组件来加载数据，还可以转发用户请求来修改数据。ViewModel不了解界面组件，因此不受配置更改的影响。

**LiveData** 是一种可观察的数据存储器。应用中的其他组件可以使用此存储器监控对象的更改，而无需在它们之间创建明确且严格的依赖路径。LiveData组件还遵循应用组件的生命周期状态，并包括清理逻辑以防止对象泄漏和过多的内存消耗。

LiveData字段**具有生命周期感知能力**，因此当不再需要引用时，会自动清理它们。这意味着，除非Activity/Fragment处于活跃状态（即，已接收onStart(）但尚未接收onStop()），否则它不会调用onChanged()回调。当Destroy时，LiveData还会自动移除观察者。

每次更新数据时，都会调用到onChanged()回调并刷新界面。



### 获取数据

随着应用的扩大，ViewModel会变得越来越难维护，ViewModel类承担了太多的责任，这就违背了分离关注点的原则。

ViewModel会将数据获取过程委托给一个新的模块，即Repository。

**Repository**模块会处理数据操作。

<u>可以将存储区视为不同数据源（持久性模型、网络服务和缓存）之间的媒介。</u>

存储区模块的重要作用：从应用的其余部分中提取数据源。

可采用**依赖注入**模式使类能够定义其依赖项而不构造它们。在运行时，另一个类负责提供这些依赖项。

#### 保留数据

持久性模式 **Room**持久性库

Room是一个对象映射库，可利用最少的样板代码实现本地数据持久性。在编译时，它会根据数据架构验证每个查询，这样损坏的SQL查询会导致编译时错误而不是运行时失败。Room可以抽象化处理原始SQL表格和查询的一些底层实现细节。

它还允许观察对数据库数据（包括集合和连接查询）的更改，并使用LiveData对象公开这类更改。



**单一可信来源**

不同端点可能返回相同的数据是一种很常见的现象。

Repository实现将网络服务响应保存在数据库中。这样一来，对数据库的更改将会触发对活跃LiveData对象的回调。使用此模型时，**数据库会充当单一可信来源**，应用的其他部分则使用Repository对其进行访问。**无论是否使用磁盘缓存，都建议存储区将某个数据源指定为应用其余部分的单一可信来源。**

#### 最佳做法

##### 避免将应用的入口点（如Activity、Service和BroadcastRecevier）指定为数据源



##### 在应用的各个模块之间设定明确定义的职责界限



##### 尽量少的公开每个模块中的代码



##### 考虑如何使每个模块可独立测试



##### 专注于应用的独特核心，以使其从其他应用中脱颖而出



##### 保留尽可能多的相关数据和最新数据



##### 将一个数据源指定为单一可信来源

每当应用需要访问这部分数据时，这部分数据都应一律源于此单一可信来源

#### 显示正在执行的操作

在某些操作中，界面务必要向用户显示当前正在执行某项网络操作。将界面操作与实际数据分离开是一种很好的做法，因为数据可能会因各种原因而更新。

**在界面中显示一致的数据更新状态（无论更新数据的请求来自何处）**

- 更改获取数据方法以返回一个LiveData类型的对象。此对象将包含网络操作的状态
- 在Repository类中再提供一个可以返回操作状态的公共函数。



基于业务去做模块化划分，

不管是MVC, MVP, MVVM，最核心的点都是将数据与视图进行分层。



> 大致将业务逻辑分为两个方面
>
> 界面交互逻辑：视图层的交互逻辑，需要根据业务实现的一些手势控制操作，一般在视图层实现
>
> 数据逻辑：也就是常说的业务逻辑，属于强业务逻辑，比如根据不同类型获取不同数据、展示不同界面，加上Data Mapper一系列操作。



Android开发应该具备数据层和视图层，MVVM模式下将业务逻辑放到ViewModel处理，但是在一个界面足够复杂的时候，那对应的ViewModel代码可能会有成千上百行，很臃肿可读性也非常差。最重要的一点这些业务会很难编写单元测试用例。



关于这点，可以单独去写一个use case处理，use case通常放在ViewModel与数据层之间，业务逻辑以及Data Mapper都应该放在use case中，每一个行为对应一个use case。这样就解决了ViewModel臃肿的问题，同时更方便编写测试用例。但流程很单一并且后期改动的可能也不太大的情况就没必要去写一个use case，DataMapper放在数据层即可。



逻辑不直接依赖View，而是被View订阅的关系。ViewModel只拿数据，而不关心数据是如何处理得到的。

与UI相关的数据逻辑都在Data Mapper做过滤，到XML这已经与数据一一对应。



MVVM是通过数据驱动UI的，数据的重要性在MVVM架构中得到了提高，成为主导因素，在这个架构模式中，关注点就放在了处理数据，保证数据的正确性上了。MVVM从整个架构来看的话，

View层调用ViewModel获取数据

ViewModel调用Repository获取数据

Repository是数据仓库，根据实际业务，通过DAO访问本地数据库或者访问服务器

ViewModel中的LiveData类型数据得到更新

View层的观察者Observer的回调方法onChanged()中收到新的数据，更新UI

![image-20210422111836939](https://raw.githubusercontent.com/TernenceChen/RoadToLearning/master/image/image-20210422111836939.png?token=AKPZIOE3Y5HZ23YML7GGHOLAQD5A6)

**数据驱动UI，通俗点讲就是当数据改变时对应的UI也要跟着变，反过来说就是当需要改变UI只需要改变对应的数据即可。**

解决数据，UI一致性问题。



ViewModel从Repository拿到数据暂存到ViewModel对应的ObservableFiled即可实现数据驱动UI，但前提是从Repository拿到的数据是可以直接用的，如果在Activity或者Adapter做数据二次处理再notify UI，就已经违背了数据驱动UI核心思想了。所以实现数据驱动UI必须要有合理的分层（**UI层拿到的数据无需处理，可以直接用**），Data Mapper恰好可以解决这个问题，同时也可规避大量编写BindAdapter的现状。



<u>在利用LiveData执行 UI <--> ViewModel通信时，ViewModel层利用末端操作符来消费来自数据层的数据流。使用`Flow.asLiveData()`扩展函数将Flow转化为LiveData，它共享了Flow的底层订阅，同时根据观察者的生命周期管理订阅。此外，LiveData可以为后续添加的观察者提供最新的数据，其订阅在配置发生变更的时候依旧能够生效。</u>



**Note首页以及搜索页面**

使用Paging3实现首页以及搜索页面的列表分页

PagingSource 为单一的数据源

PagingData 为单词分页数据的容器

Pager 用来构建`Flow<PagingData>`的类， 实现数据加载完成的回调

PagingDataAdapter 分页加载数据的RecyclerView的适配器



ViewModel使用Pager中提供的`Flow<PagingData>` 监听数据刷新

每当RecyclerView即将滚动到底部的时候，就会有新的数据的到来，最后，PagingDataAdapter展示数据

![image-20210421175029939](https://raw.githubusercontent.com/TernenceChen/RoadToLearning/master/image/image-20210421175029939.png?token=AKPZIOHWMIZXZUM7ZXDL3MLAQD5AE)



**数据变化，UI更新的关键点 Flow**

当数据库中任意一个数据有更新时，无论哪一行数据的更改，都会重新执行query操作并再次派发Flow。



数据流以协程为基础构建

**提供方** 会生成添加到数据流中的数据。得益于协程，还可以异步生成数据。

**中介** 可以修改发送到数据流的值，或修正数据流本身

**使用方**则使用数据流中的值。

在 Android 系统中，数据源或存储库通常是界面数据的提供方，其将 `View` 用作最终显示数据的使用方。

可以结合使用Flow和Room，以接受有关数据库更改的通知。在使用DAO时，返回Flow类型以获取实时更新。

调用了 `CoroutinesRoom.createFlow()`，它包含四个参数: 数据库、一个用于标识我们是否正处于事务中的变量、一个需要监听的数据库表的列表 (在本例中列表里只有 word_table) 以及一个 Callable 对象。Callable.call() 包含需要被触发的查询的实现代码。 会发现这里同数据请求调用一样使用了不同的 `CoroutineContext`。同数据插入调用一样，这里的分发器来自构建数据库时您所提供的执行器或者来自默认使用的 `Architecture Components IO` 执行器。

```kotlin
@JvmStatic
fun <R> createFlow(
        db: RoomDatabase,
        inTransaction: Boolean,
        tableNames: Array<String>,
        callable: Callable<R>
): Flow<@JvmSuppressWildcards R> = flow {
    // 观察者通道从跟踪器接收信号以发出查询
    val observerChannel = Channel<Unit>(Channel.CONFLATED)
    val observer = object : InvalidationTracker.Observer(tableNames) {
        override fun onInvalidated(tables: MutableSet<String>) {
            observerChannel.offer(Unit)
        }
    }
    observerChannel.offer(Unit) // 进行第一次查询的初始信号
    val flowContext = kotlin.coroutines.coroutineContext
    val queryContext = if (inTransaction) db.transactionDispatcher else db.queryDispatcher
    withContext(queryContext) {
        db.invalidationTracker.addObserver(observer)
        try {
            // 迭代直到取消，将观察者信号转换为查询要发送到流的结果
            for (signal in observerChannel) {
                val result = callable.call()
                withContext(flowContext) { emit(result) }
            }
        } finally {
            db.invalidationTracker.removeObserver(observer)
        }
    }
}
```



当使用PagingSource时，返回的是DataSource.Factory.asPagingSourceFactory()，在DataSource.Factory的create方法中返回一个LimitOffsetDataSource对象，在LimitOffsetDataSource的构造方法中，调用了

```kotlin
db.getInvalidationTracker().addWeakObserver(mObserver)
```

这里也是一个典型的观察者模式范型的表现。

`addObserver()`中将`observer`,`tableIds`, `tableNames`封装为一个`ObserverWrapper`对象，然后添加到`mObserverMap`中，如果是第一次加入Map的observer对象，调用onAdded()，

当且仅当一个表首次被监听时，我们会创建一组trigger，对这个表的数据更新中状态通过invalidated进行，invalidated的值为0或1，1表示数据有更新。

InvalidationTracker中有一个mRefreshRunbable，通过checkUpdatedTable函数，得到所有invalidated=1的tableId，之后在mObserverMap中通知观察者



DAO层业务代码委托RoomDatabase通过beginTransaction，endTransaction完成DB的事务提交，这些函数被调用时，顺带触发InvalidationTracker内部对表的异步查询，查询到有数据更新（invalidated=1）的表名信息，将这些表明信息带给Observer#onInvalidated的参数中。利用InvalidationTracker建立的这套机制，实现了DAO层LiveData的持有数据或Flow数据实时更新的特性。



（这个Map是`SafeIterableMap` 里面有一个WeakHasMap，需要遍历Map的时候，获取的 `Iterator` 就会被放到一个 `WeakHashMap` 中，当Map需要 `remove` 元素的时候只要通知 `WeakHashMap` 中所有的迭代器，这样使用的好处是不需要我们再维护删除已经被GC的迭代器，`WeakHashMap` 可以帮助清理掉key已经被回收的entry。）



StateFlow是一个状态容器式可观察数据流，可以向其收集器发出当前状态更新和新状态更新。还可通过其value属性读取当前状态值。如需更新状态并将其发送到数据流，可以为MutableStateFlow类的value属性分配一个新值。

与使用Flow构建器构建的冷数据流不同，`StateFlow` 是热数据流：从数据流收集数据不会触发任何提供方代码。`StateFlow` 始终处于活跃状态并存于内存中，而且只有在垃圾回收根中未涉及对它的其他引用时，它才符合垃圾回收条件。

当新使用方开始从数据流中收集数据时，它将接收信息流中的最后状态及任何后续状态。

`StateFlow` 和 `LiveData` 具有相似之处。两者都是可观察的数据容器类，并且在应用架构中使用时，两者都遵循相似模式。但是两者的行为确是不同的。

- StateFlow需要将初始状态传递给构造函数，而LiveData不需要。
- 当View变为STOPPED状态时，LiveData.observe()会自动取消注册使用方，而从StateFlow或任何其他数据流收集数据则不会取消注册使用方。

当 `View` 进入后台，触发收集数据流的协程处于挂起状态时，底层提供方仍然处于活跃状态。如需在 View 不可见时停止监听 `uiState` 的变化，可以使用asLiveData()函数将数据流转换为LiveData，或者手动cancel掉。