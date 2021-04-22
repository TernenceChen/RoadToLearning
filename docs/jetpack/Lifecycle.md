## Lifecycle

> Lifecycle，顾名思义，它是用于帮助开发者管理Activity和Fragment 的生命周期，它是LiveData和ViewModel的基础。

### Lifecycle之前

- activity的生命周期内有大量管理组件的代码，难以维护
- 无法保证组件会在Activity / Fragment停止后不执行启动

### Lifecycle使用

> Lifecycle是一个库，也包含Lifecycle这样一个类，Lifecycle类用于存储有关组件的生命周期状态的信息，并允许其他对象观察此状态。

#### 引入依赖

```
implementation 'androidx.appcompat:appcompat:1.2.0'
```

#### 使用方法

Lifecycle的使用很简单：

- 生命周期拥有者：使用`getLifecycle()`获取`Lifecycle`实例，然后再用`addObserve()`添加观察者；
- 观察者实现`LifecycleObserver`，方法上使用`OnLifecycleEvent`注解关注对应生命周期，生命周期触发时就会执行对应方法。

##### 基本使用

1. 实现接口`LifecycleObserver`，`LifecycleObserver`用于标记一个类是生命周期观察者。表示类具有`Lifecycle`。
2. 在方法上加上`@OnLifecycleEvent`注解，且`value`分别是`Lifecycle.Event.ON_RESUME`、`Lifecycle.Event.ON_PAUSE`，这个效果就是在onResume时执行`value`是`Lifecycle.Event.ON_RESUME`的方法，在onPause时执行`value`是`Lifecycle.Event.ON_PAUSE`的方法。

##### Lifecycle

`Lifecycle` 是一个类，用于存储有关组件（如 Activity 或 Fragment）的生命周期状态的信息，并允许其他对象观察此状态。

`Lifecycle` 使用两种主要枚举跟踪其关联组件的生命周期状态：

##### **事件**

从框架和`Lifecycle`类分派的生命周期事件。这些事件映射到Activity和Fragment中的回调事件。

**状态**

由`Lifecycle`对象跟踪的组件的当前状态。

![image-20210322114704964](C:%5CUsers%5C602116%5CAppData%5CRoaming%5CTypora%5Ctypora-user-images%5Cimage-20210322114704964.png)

类可以通过向其方法添加注解来监控组件的生命周期状态。然后，也可以通过调用`Lifecycle`类的`addObserver()`方法并传递观察者的实例来添加观察者。

##### LifecycleOwner

`LifecycleOwner`是单一方法接口，表示类具有`Lifecycle`。它具有一种方法（即`getLifecycle()`），该方法必须由该类实现。

此接口从各个类（如 `Fragment` 和 `AppCompatActivity`）抽象化 `Lifecycle`的所有权，并允许编写与这些类搭配使用的组件。任何自定义应用类均可实现 `LifecycleOwner` 接口。

实现`LifecycleObserver`的组件可与实现`LifecycleOwner`的组件完美配合，因为所有者可以提供生命周期，而观察者可以注册以观察生命周期。

`Lifecycle`类允许其他对象查询当前状态。

##### 自定义LifecycleOwner

如果需要自定义类并使其成为`LifecycleOwner`，可以使用`LifecycleRegistry`类，但需要将事件转发到该类。

### 生命周期感知型组件的最佳做法

- 使界面控制器尽可能保持精简。它们不应试图获取自己的数据，而应使用`ViewModel`执行此操作，并观察`LiveData`对象以将更改体现到视图中。
- 设法编写数据驱动型界面，对于此类界面，界面控制器的责任是随着数据更改而更新视图，或者将用户操作通知给`ViewModel`。
- 将数据逻辑放在 `ViewModel`类中。`ViewModel`应充当界面控制器与应用其余部分之间的连接器。不过要注意，`ViewModel`不负责获取数据（例如，从网络获取）。但是，`ViewModel`应调用相应的组件来获取数据，然后将结果提供给界面控制器。
- 使用**数据绑定**在视图与界面控制器之间维持干净的接口。这样一来，您可以使视图更具声明性，并尽量减少需要在 Activity 和 Fragment 中编写的更新代码。
- 如果界面很复杂，不妨考虑创建 `presenter` 类来处理界面的修改。这可能是一项艰巨的任务，但这样做可使界面组件更易于测试。
- 避免在 `ViewModel` 中引用 `View` 或 `Activity` 上下文。如果 `ViewModel` 存在的时间比 Activity 更长（在配置更改的情况下），Activity 将泄漏并且不会获得垃圾回收器的妥善处置。
- 使用**Kotlin 协程**管理长时间运行的任务和其他可以异步运行的操作。

### 生命周期感知型组件的用例

生命周期感知型组件可使您在各种情况下更轻松地管理生命周期。

- 在粗粒度和细粒度位置更新之间切换。使用生命周期感知型组件可在位置应用可见时启用细粒度位置更新，并在应用位于后台时切换到粗粒度更新。借助生命周期感知型组件 `LiveData`，应用可以在用户使用位置发生变化时自动更新界面。
- 停止和开始视频缓冲。使用生命周期感知型组件可尽快开始视频缓冲，但会推迟播放，直到应用完全启动。此外，应用销毁后，您还可以使用生命周期感知型组件终止缓冲。
- 开始和停止网络连接。借助生命周期感知型组件，可在应用位于前台时启用网络数据的实时更新（流式传输），并在应用进入后台时自动暂停。
- 暂停和恢复动画可绘制资源。借助生命周期感知型组件，可在应用位于后台时暂停动画可绘制资源，并在应用位于前台后恢复可绘制资源。

### 处理ON_STOP事件

如果 `Lifecycle`属于 `AppCompatActivity` 或 `Fragment`，那么调用 `AppCompatActivity` 或 `Fragment` 的 `onSaveInstanceState()` 时，`Lifecycle` 的状态会更改为 `CREATED`并且会分派 `ON_STOP` 事件。

通过 `onSaveInstanceState()` 保存 `Fragment` 或 `AppCompatActivity` 的状态后，其界面被视为不可变，直到调用 `ON_START`。如果在保存状态后尝试修改界面，很可能会导致应用的导航状态不一致，因此应用在保存状态后运行 `FragmentTransaction` 时，`FragmentManager` 会抛出异常。

`LiveData` 本身可防止出现这种极端情况，方法是在其观察者的关联 `Lifecycle`还没有至少处于 `STARTED` 状态时避免调用其观察者。在后台，它会在决定调用其观察者之前调用 `isAtLeast()`。

