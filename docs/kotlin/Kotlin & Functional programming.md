# Kotlin & Functional programming

> Kotlin是一门集成面向对象（OOP）与函数式（FP）的多范式语言。

**函数式编程**（FP）基于一个前提：只用纯函数来构建程序。

**纯函数**就是**<u>没有副作用的函数</u>**，<u>**具备引用透明性**</u>。

副作用就是修改了某处的某些东西。

- 修改了外部变量的值
- IO 操作，如写数据到磁盘
- UI操作，如修改一个按钮的可操作状态

副作用的产生往往与**可变数据**及**共享状态**相关，一个带有副作用的函数的不良反应会让程序变得危险，也可能让代码变得难以测试。

举个例子：

```kotlin
sealed class Format
data class Print(val text: String): Format()
object NewLine: Format()

val string = listOf<Format>(Print("Hello"), NewLine, Print("Kotlin"))

fun unsafeInterpreter(str: List<Format>) {
    str.forEach {
        when (it) {
            is Print -> print(it.text)
            is NewLine -> println()
        }
    }
}
```

内部的副作用 `print`

1. **缺乏可测试性**

   在对`unsafeInterpreter`函数的代码逻辑进行测试时，没有采用`assert`断言，通过打印结果来反映格式转化的正确性，在复杂操作时，会让测试工作变得繁琐。

2. **难以被组合复用**

   `unsafeInterpreter`函数内部混杂了副作用及字符串格式转化的逻辑，当想要对转化后的结果进行复用时，就会产生问题。如果这里时持久化到数据库的操作，显然就不能被当作转化字符串的功能方法来使用。

- **纯函数消除副作用**

```kotlin
fun stringInterpreter(str: List<Format>) = str.fold("") { fullText, s ->
    when(s) {
        is Print -> fullText + s.text
        is NewLine -> fullText + "\n"
    }
}
```

使用`fold`实现了一个`stringInterpreter`函数，它会返回格式化结果的字符串值。只要传给它的参数一致，每次都可以获得相同的返回结果。

- **具有引用透明性**

> 一个表达式在程序中可以被它等价的值替换，而不影响结果。当谈论一个具体的函数时，如果它具备引用透明性，只要它输入相同，对应的计算结果也会相同。

在`unsafeInterpreter`函数中，它的返回结果值都是`Unit`，也可以看成相同的结果值，但它是有副作用的，因此，“计算结果”不仅针对返回结果值。假使一个函数具备引用透明性，那么它内部的行为不会改变外部的状态。如果`unsafeInterpreter`中的`print`操作，每次执行都会在控制台打印信息，所以具有副作用行为的函数也违背了引用透明性原则。

- **纯函数与局部可变性**

> 函数式编程倡导我们使用纯函数来编程，促进这一过程的一大语言特性就是不可变性。

当我们谈论引用透明性时，需要结合上下文来解读。局部可变性有时候能够让我们的程序设计变得更加自然，性能更好。所以函数式编程并不意味着拒绝可变性。相反，合理结合可变性和不可变性，能够发挥更好的作用。

## 函数式编程的应用

### 集合操作

> 函数式编程中，用的最多的还是集合操作。集合操作的链式调用比起普通的`for`循环更直观、写法更简单。

```kotlin
fun initBookList() = listOf(
    Book("Kotlin", "小明", 55, Group.Technology),
    Book("中国民俗", "小黄", 25, Group.Humanities),
    Book("娱乐杂志", "小红", 19, Group.Magazine),
    Book("灌篮", "小张", 20, Group.Magazine),
    Book("资本论", "马克思", 50, Group.Political),
    Book("Java", "小张", 30, Group.Technology),
    Book("Scala", "小明", 75, Group.Technology),
    Book("月亮与六便士", "毛姆", 25, Group.Fiction),
    Book("追风筝的人", "卡勒德", 30, Group.Fiction),
    Book("文明的冲突与世界秩序的重建", "塞缪尔·亨廷顿", 24, Group.Political),
    Book("人类简史", "尤瓦尔•赫拉利", 40, Group.Humanities)
    )

data class Book(
    val name: String,
    val author: String,
    //单位元，假设只能标价整数
    val price: Int,
    //group为可空变量，假设可能会存在没有（不确定）分类的图书
    val group: Group?)


enum class Group{
    //科技
    Technology,
    //人文
    Humanities,
    //杂志
    Magazine,
    //政治
    Political,
    //小说
    Fiction
}
```

先尝试用命令式的风格获取Technology类型的书名列表。

```kotlin
fun getTechnologyBookList(books: List<Book>) : List<String>{
    val result = mutableListOf<String>()
    for (book in books){
        if (book.group == Group.Technology){
            result.add(book.name)
        }
    }
    return result
}
```

如果要使用函数式的风格来实现这个功能的话，可以通过`filter`与`map`函数来实现。

```kotlin
fun getTechnologyBookListFp(books: List<Book>) =
    books.filter { it.group == Group.Technology }.map { it.name }
```

```kotlin
>>>> 打印结果
[Kotlin, Java, Scala]
```

如果用函数式的风格，代码会比使用for循环更容易理解，并且更简洁。上面的函数式代码实现的功能一目了然：**先过滤出`group` 等于 Technology的书本，然后把书本转换成书本的名字**。

如果有一个复杂一点的需求，把书按照分组分类放好。

```kotlin
fun groupBooks(books: List<Book>){
    val groupBooks = mutableMapOf<Group?, MutableList<Book>>()
    for (book in books){
        if (groupBooks.containsKey(book.group)){
            val subBooks = groupBooks[book.group] ?: mutableListOf()
            subBooks.add(book)
        }else{
            val subBooks = mutableListOf<Book>()
            subBooks.add(book)
            groupBooks[book.group] = subBooks
        }
    }

    for (entry in groupBooks){
        println(entry.key)
        println(entry.value.joinToString(separator = "") { "$it\n" })
        println("——————————————————————————————————————————————————————————")
    } 
}
```

如果要用函数式的方式来实现一下这段函数，我们可以使用操作符**groupBy**实现这个功能

```kotlin
fun groupBooksFp(books: List<Book>){
    books.groupBy { it.group }.forEach { (key, value) ->
        println(key)
        println(value.joinToString(separator = "") { "$it\n" })
        println("——————————————————————————————————————————————————————————")
    }
}
```

输出结果：

```
Technology
Book(name=Kotlin, author=小明, price=55, group=Technology)
Book(name=Java, author=小张, price=30, group=Technology)
Book(name=Scala, author=小明, price=75, group=Technology)

——————————————————————————————————————————————————————————
Humanities
Book(name=中国民俗, author=小黄, price=25, group=Humanities)
Book(name=人类简史, author=尤瓦尔•赫拉利, price=40, group=Humanities)

——————————————————————————————————————————————————————————
Magazine
Book(name=娱乐杂志, author=小红, price=19, group=Magazine)
Book(name=灌篮, author=小张, price=20, group=Magazine)

——————————————————————————————————————————————————————————
Political
Book(name=资本论, author=马克思, price=50, group=Political)
Book(name=文明的冲突与世界秩序的重建, author=塞缪尔·亨廷顿, price=24, group=Political)

——————————————————————————————————————————————————————————
Fiction
Book(name=月亮与六便士, author=毛姆, price=25, group=Fiction)
Book(name=追风筝的人, author=卡勒德, price=30, group=Fiction)

——————————————————————————————————————————————————————————
```

集合还有很多其它的函数（`flatMap`、`find`等）

### 高阶函数

> 值构造器：通过传入一个具体的值，然后构造出另一个具体的值。
>
> 类型构造器：通过传入一个具体的类型变量，然后构造出另一个具体的类型。

高阶函数的定义，**入参是函数或出参是函数的函数就是高阶函数**。

```kotlin
//入参是函数的高阶函数
fun fooIn(func: () -> Unit){
    println("foo")
    func()
}

//出参是函数的高阶函数
fun fooOut() : () -> Unit{
    println("hello")
    return { println(" word!")}
}
```

以上就是两种最简单的两种形式的高阶函数。

对于前面集合操作的函数，以`filter`为例，`filter`函数是怎么实现的呢？

```kotlin
public inline fun <T> Iterable<T>.filter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList<T>(), predicate)
}
```

```kotlin
public inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterTo(destination: C, predicate: (T) -> Boolean): C {
    //具体实现
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}
```

在调用`filter`函数的时候，会经历如下步骤：

- `filetr`方法接收一个叫`predicate`的函数参数。

- 然后`filter`会调用`filterTo`方法，`filterTo`方法第一个入参是一个可变集合，第二个入参和`filter`的`predicate`是一样的。

- 当`filter`调用`filterTo`的时候会创建一个名为`destination`的`ArrayList`对象，这个对象的作用就是一个收集器。在`filter`中，会遍历目标集合。

- 如果`predicate`方法返回`true`，则会把当前元素添加到收集器中。便利完毕会返回`destination`收集器。

> 这里可以看出，`filter`函数返回的是一个新集合，不会影响调用集合本身。

以`books.filter { it.group == Group.Technology }`为例：

Function1

```kotlin
public interface Function1<in P1, out R> : Function<R> {
    /** Invokes the function with the specified argument. */
    public operator fun invoke(p1: P1): R
}
```

脱糖后的代码

```kotlin
  books.filter(object : Function1<Book, Boolean>{
        override fun invoke(p1: Book): Boolean {
            return p1.group == Group.Technology
        }

    })


inline fun Iterable<Book>.filter(predicate: Function1<Book, Boolean>): List<Book> {
    return filterTo(ArrayList<Book>(), predicate)
}

inline fun Collection<Book>.filterTo(destination: MutableCollection<Book>, predicate:Function1<Book, Boolean>): MutableCollection<Book> {
    for (element in this) {
        val isAdd = predicate.invoke(element)
        if (isAdd) destination.add(element)
    }
return destination
}
```

可以看到，脱糖处理后的`filter`函数和我们常用的一种设计模式是非常类似的，这个`filter`函数我们可以看作是策略模式的一种实现，而传入的`predicate`实例就是我们的一种策略。

> 策略模式(Strategy Pattern)：定义一系列算法，将每一个算法封装起来，并让它们可以相互替换。策略模式让算法独立于使用它的客户而变化，也称为政策模式(Policy)。
>
> 策略模式是对算法的封装，它把算法的责任和算法本身分割开，委派给不同的对象管理。策略模式通常把一个系列的算法封装到一系列的策略类里面，作为一个抽象策略类的子类。用一句话来说，就是“准备一组算法，并将每一个算法封装起来，使得它们可以互换”。

函数式编程可读性更强更易于维护的原因之一就是：**在函数式编程的过程中，我们会被动使用大量的设计模式**。就算我们不刻意去定义/使用，我们也会大量使用类似设计模式中的策略模式和观察者模式去实现我们的代码。

## 函数式设计的通用结构

### Option

> Kotlin的可空类型，某种程度上就是利用类型代替Checked Exception来防止NPE问题。

空安全是通过可空变量/常量实现的。在我们使用可空变量/常量的时候，编译器会强制我们要做空检查才能使用。一般我们会使用`?`这个语法糖实现，当然也可以在使用前先判空，判空后Kotlin会自动帮我们进行智能转换，会把可空变量转换成非空变量。

Kotlin里面的Option就是用来处理可空变量的。

```kotlin
fun main(args: Array<String>) {
    fooOption("hello word!!!")
    println("——————————————————————")
    fooOption(null)
}

fun fooOption(str: String?){
    val optionStr = Option.fromNullable(str)
    val printStr = optionStr.getOrElse { "str is null" }
    println(printStr)
    optionStr.exists {
        println("str is not null! str = $it")
        true
    }
}

>>>> 打印结果
hello word!!!
str is not null! str = hello word!!!
——————————————————————
str is null
```

> `Option.fromNullable(str)`可以用语法糖`str.toOption()`代替

`Option.getOrElse`函数的作用是：如果对象不为空，返回对象本身，为空则返回一个默认值。

`Option.exists`函数的作用是：如果对象不为空，则执行`Lambda`（函数、闭包）里面的代码。

#### 如何使用Option写出结构更合理的代码

```kotlin
fun main(args: Array<String>) {
    val buyBook1 = BookStore().buyABook1(CreateCard(1), 1)
    if (buyBook1 != null) {
        println("book name = ${buyBook1.name}, author = ${buyBook1.author}")
        if (buyBook1.group != null) {
            println("book group = ${buyBook1.group}")
        }
    }

    println("\n——————————— createCard is null, bookCode 1 ———————————————")

    val bookStoreOption2 = BookStore()
    buyBook2ForStore(bookStoreOption2, null, 1)

    println("\n—————————————————————————— bookCode 1 ————————————————————————————————")

    buyBook2ForStore(bookStoreOption2, CreateCard(1), 1)

    println("\n—————————————————————————— bookCode 20————————————————————————————————")
    buyBook2ForStore(bookStoreOption2, CreateCard(1), 20)

}

fun buyBook2ForStore(store: BookStore, createCard: CreateCard?, bookCode: Int) {
    val buyBook2 = store.buyABook2(createCard, bookCode)
    val bookOption = buyBook2.map {
        println("book name = ${it.name}, author = ${it.author}")
        it.group
    }

    println("start buyBook2ForStore")
    bookOption.exists {
        println("book group = $it")
        true
    }

}

class BookStore {
    private val bookCollection = initBookCollection()

    fun buyABook1(cc: CreateCard?, bookCode: Int): Book? {
        val result = bookCollection[bookCode]
        if (cc != null && result != null) {
            cc.charge(result.price)
        }
        return result
    }

    fun buyABook2(cc: CreateCard?, bookCode: Int): Option<Book> {
        val lcOption = cc.toOption()
        val bookOption = bookCollection[bookCode].toOption()
        lcOption.map2(bookOption) {
            it.a.charge(it.b.price)
        }
        return bookOption
    }

}
```

`buyABook2`和`buyABook1`的主要区别是，`buyABook2`返回的是一个非空的`Option<Book>`对象。

```kotlin
	val buyBook1 = BookStore().buyABook1(CreateCard(1), 1)
    if (buyBook1 != null) {
        println("book name = ${buyBook1.name}, author = ${buyBook1.author}")
        if (buyBook1.group != null) {
            println("book group = ${buyBook1.group}")
        }
    }
```

对比`buyBook2ForStore`这个函数，我们看下不使用Option的调用函数。

用`Option`后，代码反而变得更多了。

我们这样理解这个函数：

- 调用调用`store.buyABook2`方法获取一个`Option`对象。
- 把`Option`转换成`Option`对象。
- 如果`Option`对象存在（不为空）的话，则处理事件。

但随着`Book`中的`Group`其实是更加复杂的对象，比如增加其他的信息，并且都是可控变量。

`buyBook1`可能要写成这样：

```kotlin
    val buyBook2 = BookStoreOption().buyABook1(CreateCard(1), 1)
    if (buyBook2 != null) {
        println("book name = ${buyBook2.name}, author = ${buyBook2.author}")
        if (buyBook2.group != null) {
            println("book group = ${it.group}")
            if (buyBook2.group.name != null) {
                println("book group name = ${it.group.name}")
            }
        }
    }
```

不适用`Option`，只能一层层使用`if`去判断。**代码嵌套会使复杂度提高**

如果使用`Option`，则可以这样实现：

```kotlin
    val buyBook3 = store.buyABook2(createCard, bookCode)
    buyBook3.map {
        println("book name = ${it.name}, author = ${it.author}")
        it.group
    }
        .map {
            println("book group = $it")
            it?.name
        }
        .exists {
            println("book name = $it")
            true
        }
```

`Option`在处理大量可空值的时候，能以线性的方式去处理。

在函数式编程中，我们使用函数式的通用结构的话，天然就是在使用类似设计模式的方式去编写代码。函数式的通用结构和设计模式的作用是类似的，但是函数式结构能提供更高程度的抽象。

## Monoid

#### 什么是Monoid

Monoid的概念：

- 一个抽象类型A；
- 一个满足结合律的二元操作`append`，接收任何两个A类型的参数，然后返回一个A类型的结果；
- 一个单元元`zero`，它同样也是A类型的一个值。

`monoid`如何满足两个数学法则：

- 结合律：`append(a, append(b, c)) == append(append(a, b), c)`，这个等式对于任何A类型的值（a, b ,c）都成立。
- 同一律：`append(a, zero) == a`或 `append(zero, a) == a`，单位元`zero`与任何A类型的值（a）的`append`操作，结果都等于a。

Monoid是一种纯代数结构，在函数式编程中经常出现，操作列表、连接字符、循环中进行累加操作都可以被解析成Monoid。主要作用是：**将问题拆分成小部分然后并行计算和将简单的部分组装成复杂的计算**。

`Monoid`

```kotlin
/**
 * ank_macro_hierarchy(arrow.typeclasses.Monoid)
 */
interface Monoid<A> : Semigroup<A>, MonoidOf<A> {
  /**
   * A zero value for this A
   */
  fun empty(): A

  /**
   * Combine an [Collection] of [A] values.
   */
  fun Collection<A>.combineAll(): A =
    if (isEmpty()) empty() else reduce { a, b -> a.combine(b) }

  /**
   * Combine an array of [A] values.
   */
  fun combineAll(elems: List<A>): A = elems.combineAll()

  companion object
}
```

`Semigroup`

```kotlin
/**
 * ank_macro_hierarchy(arrow.typeclasses.Semigroup)
 */
interface Semigroup<A> {
  /**
   * Combine two [A] values.
   */
  fun A.combine(b: A): A

  operator fun A.plus(b: A): A =
    this.combine(b)

  fun A.maybeCombine(b: A?): A = Option.fromNullable(b).fold({ this }, { combine(it) })
}
```

简单使用Monoid

```kotlin
fun main() {
    val monoid = Int.monoid()
    val sum = listOf(1, 2, 3, 4, 5).foldMap(monoid, ::identity)

    println("sum = $sum")

}

>>>> 结果打印
sum = 15
```

`Int.monoid()`在这里的作用就是定义了元素之间的结合关系

`Int.monoid()`给我们返回了一个`IntMonoid`。我们再来看看`IntMonoid`的定义:

```kotlin
interface IntSemigroup : Semigroup<Int> {
  override fun Int.combine(b: Int): Int = this + b
}

interface IntMonoid : Monoid<Int>, IntSemigroup {
  override fun empty(): Int = 0
}

interface IntSemiring : Semiring<Int> {
  override fun zero(): Int = 0
  override fun one(): Int = 1

  override fun Int.combine(b: Int): Int = this + b
  override fun Int.combineMultiplicate(b: Int): Int = this * b
}
```

`IntMonoid`的定义也非常简单，主要是定义了0值，1值和它们两两结合的操作。

再来看一下在BootStore新增一下功能时：

- 支持批量购买
- 销售和支付解耦
- 支持拒绝策略

```kotlin
class ChargeMonoid(private val cc: CreateCard) : Monoid<Charge> {

    override fun empty(): Charge = Charge(cc, 0, 0)

    override fun Charge.combine(b: Charge): Charge =
        if (cc == b.createCard) {
            Charge(cc, b.price + price)
        } else {
            this
        }
    
}
```

```kotlin
class MonoidBookStore {

    private val bookCollection = initBookCollection()

    /**
     * bookId可以传入多个
     */
    fun buyBooks(cc: CreateCard, vararg bookIds: Int) : Pair<List<Book>, Charge>{
        val purchases = bookIds
            .map { buyABook(cc, it).orNull() }
            .filterNotNull()
        val (books, charges) = purchases.unzip()

        //使用ChargeMonoid折叠列表
        val totalCharge =  charges.foldMap(Charge.monoid(cc), ::identity)
        return books to totalCharge
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun buyABook(cc: CreateCard, bookId: Int) : Option<Pair<Book, Charge>> {
        val book = bookCollection[bookId]
        return book.toOption().map { Pair(it, Charge(cc, it.price)) }
    }
}
```

> ::identity是Kotlin的一个语法糖，在这里的作用是，返回折叠后的Charge对象。::identity的作用是返回本身。

`fold`

```kotlin
  fun <A> Kind<F, A>.fold(MN: Monoid<A>): A = MN.run {
    foldLeft(empty()) { acc, a -> acc.combine(a) }
  }
```

`foldMap`

```kotlin
  fun <A, B> Kind<F, A>.foldMap(MN: Monoid<B>, f: (A) -> B): B = MN.run {
    foldLeft(MN.empty()) { b, a -> b.combine(f(a)) }
  }
```

```kotlin
fun main() {
    val cc = CreateCard(12423)
    val (b1, c1) = MonoidBookStore().buyBooks(cc, 1, 2)
    val (b2, c2) = MonoidBookStore().buyBooks(cc, 4, 5)
    val (b3, c3) = MonoidBookStore().buyBooks(cc, 6, 7)

    val books = listOf(b1, b2, b3).flatten()
    val charge = listOf(c1, c2, c3).foldMap(Charge.monoid(cc),::identity)

    printBuyBooks(books, charge )
}

>>>> 打印结果
—————————————————第一张信用卡———————————————————
希望购买书本名字： [Kotlin, 中国民俗, 灌篮, 资本论, Java, Scala]
pay 105 yuan
支付成功，支付金额 = 105; 剩余额度 = 395
```

Monoid的主要作用就是，定义了相同类型（群）的对象的合并规律。

## 函数式编程带来的性能问题

> 使用高阶函数是会带来一些性能损失的，因为每个Lambda表达式都是一个对象。从上文的分析可知，在JVM中，Lambda表达式其实是通过函数接口实现的。所以我们在使用lambda的时候，就相当于new了一个新对象出来。而且由于Lambda在访问外部变量时，会捕获变量的原因，捕获变量也会带来一定的内存开销。

我们可以通过内联函数来解决这些问题。

#### 内联函数 inline

当一个函数被声明为`inline`时，它的函数体是内联的。换句话说，函数体会被直接替换到函数被调用的地方，而不是被正常调用。