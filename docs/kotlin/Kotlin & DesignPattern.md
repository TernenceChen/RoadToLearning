# Kotlin & Design Pattern

> 进一步认识Kotlin语言特点，以及了解如何在实际代码设计中运用它们

## 创建型模式

> 工厂方法模式、抽象工厂模式以及构建者模式

## 行为型模式

> 观察者模式、策略模式、模板方法模式、迭代器模式、责任链模式及状态模式

### 观察者模式

> **观察者模式**定义了一个一对多的依赖关系，让一个或多个观察者对象监听一个主题对象。这样一来，当被观察者状态发生改变时，需要通知相应的观察者，使这些观察者对象能够自动更新。

1. 订阅者添加或删除对发布者的状态监听
2. 发布者状态改变时，将事件通知给监听它的所有观察者，然后观察者执行响应逻辑。

#### Observable

Kotlin引入了可被观察的委托属性。Delegates.observable()

它提供了三个参数，依次代表委托属性的元数据KProperty对象、旧值、新值。通过额外定义的接口，把不同响应逻辑封装成接口方法，实现解耦。

#### Vetoable

> 在被赋新值生效之前提前进行截获，然后判断是否接受它。

```kotlin
inline fun <T> vetoable(
    initialValue: T,
    crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Boolean
): ReadWriteProperty<Any?, T>
```

`initialValue` - 属性的初始值

`onChange` - 尝试更改属性值之前调用的回调。调用此回调时，属性的值尚未更改。如果回调返回true，则将属性的值设置为新值，如果回调返回false，则丢弃新值，并且该属性保留其旧值。

```kotlin
var value: Int by Delegates.vetoable(0) { prop, old, new ->
    new > 0
}

>>> value = 1
>>> println(value)
1

>>> value = -1
>>> println(value)
1
```

它的初始化值为0，只接受被正整数赋值。

### 策略模式、模板方法模式

> **策略模式**就是将不同的行为策略进行独立封装，与类在逻辑上解耦。
>
> **模板方法模式**在一个方法中定义一个算法的骨架，而将一些步骤延迟到子类中，使得子类在不改变算法结构的情况下，重新定义算法中的某些步骤。

##### 遵循开闭原则：策略模式

```kotlin
interface SwimStrategy {
    fun swim()
}

class Breaststroke: SwimStrategy {
    override fun swim() {
        println("I am breaststroking...")
    }
}

class Backstroke: SwimStrategy {
    override fun swim() {
        println("I am backstroke...")
    }
}

class Freestyle: SwimStrategy {
    override fun swim() {
        println("I am freestyle...")
    }
}

class Swimmer(val strategy: SwimStrategy) {
	fun swim() {
		strategy.swim()
	}
}

fun main(args: Array<String>) {
    val weekendShaw = Swimmer(Freestyle())
    weekendShaw.swim()
    val weekdaysShaw = Swimmer(Breaststroke())
    weekdaysShaw.swim()
}

// 运行结果
I am freestyle...
I am breaststroking...
```

##### 高阶函数抽象算法

```kotlin
fun breaststroke() {
    println("I am breaststroking...")
}

fun backstroke() {
     println("I am backstroking...")
}

fun freestyle() {
     println("I am freestyling...")
}

class Swimmer(val swimming: () -> Unit) {
	fun swim() {
		swimming()
	}
}

fun main(args: Array<String>) {
    val weekendShaw = Swimmer(::freestyle)
    weekendShaw.swim()
    val weekdaysShaw = Swimmer(::breaststroke)
    weekdaysShaw.swim()
}
```

##### 模板方法模式：高阶函数代替继承

### 迭代器模式

> 将遍历和实现分离开来，在遍历的同时不需要暴露对象的内部表示。

### 责任链模式

> 避免请求的发送者和接收者之间的耦合关系，将这个对象连成一条链，并沿着这条链传递该请求，直到有一个对象处理它为止。

#### 用orElse构建责任链

### 状态模式

> 状态模式允许一个对象在其内部状态改变时改变它的行为，对象看起来似乎修改了它的类。

- 状态决定行为，对象的行为由它内部的状态决定
- 对象的状态在运行期被改变时，它的行为也会因此而改变。

## 结构型模式

> 装饰者模式

### 装饰者模式

> 在不必改变原类文件和使用继承的情况下，动态地扩展一个对象的功能。该模式通过创建一个包装对象，来包裹真实的对象。

- 创建一个装饰类，包含一个需要被装饰类的实例
- 装饰类重写所有被装饰类的方法
- 在装饰类中对需要增强的功能进行扩展

#### 用类委托减少样板代码

**`by` 关键字**

```kotlin
interface MacBook {
    fun getCost(): Int
    fun getDesc(): String
    fun getProdDate(): String
}

class MacBookPro: MacBook {
    override fun getCost() = 10000
    override fun getDesc() = "MacBook Pro"
    override fun getProdDate() = "Late 2011"
}

//装饰类
class ProcessorUpgradeMacbookPro(val macbook: MacBook) : MacBook by macbook {
    override fun getCost() = macbook.getCost() + 219
    override fun getDesc() = macbook.getDesc() + ", +1G Memory"
}
```

通过Kotlin的类委托语法，我们实现了一个ProcessorUpgradeMacbookPro类，该类会把MacBook接口所有的方法都委托给构造参数对象macbook。

```kotlin
fun main(args: Array<String>) {
    val macBookPro = MacBookPro()
    val processorUpgradeMacbookPro = ProcessorUpgradeMacbookPro(macBookPro)
    println(processorUpgradeMacbookPro.getCost())
    println(processorUpgradeMacbookPro.getDesc())
}

// 运行结果
10219
MacBook Pro, +1G Memory
```

