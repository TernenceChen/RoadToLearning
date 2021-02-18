# kotlin分享

为什么要使用kotlin？

1. 降低崩溃率，据谷歌的数据，Google Play 排名前 1,000 的应用，发现使用 Kotlin 的应用与不使用 Kotlin 的应用相比，其用户崩溃率低 20%。中国电信营业厅应用团队的新项目大约有 50% 的代码是使用 Kotlin 编写。团队成功将空指针异常的出现概率 降低了 80% 之多。

2. 更简洁的语法，更少的代码量，据网易云音乐团队的统计，他们项目中java文件代码平均行数为130+，kotlin才80+。

3. [Google I/O Android App]: https://github.com/google/iosched#google-io-android-app

   [Android Architecture Components samples]:https://github.com/google/iosched#google-io-android-app

   这些谷歌提供的samples都越来越倾向使用kotlin编写，如果我们也使用kotlin有助于我们更好学习如果使用Jetpack开发，搭建更好的App架构



### 基本类型

Kotlin 的基本数值类型包括 Byte、Short、Int、Long、Float、Double 等。不同于 Java 的是，字符不属于数值类型，是一个独立的数据类型。

### 定义变量

```kotlin
//int i = 0;
var i: Int = 0
//自动类型推断
var i = 0
```

### 定义常量

```kotlin
//final int i = 0;
val i = 1 
//自动类型推断
val str = "string"
const val str = "string"
```

const必须修饰val,只允许在top-level级别和object中声明，编译成字节码时public final static，val为private final static

### 定义方法

```kotlin 
fun sum(a: Int, b: Int): Int {
    return a + b
}

fun sum(a: Int, b: Int) = a + b
```

kotlin中方法是一等公民，我们可以把方法写在kt文件最外层，不必写在类里,编译后，将自动产生一个当前文件名+Kt的类，而声明的方法将变成其静态方法

```kotlin
fun test() {
}
```

```
public final class com/example/myapplication/PersonKt {
  public final static test()V
   L0
    LINENUMBER 41 L0
    RETURN
   L1
    MAXSTACK = 0
    MAXLOCALS = 0
}
```

**扩展方法**

kotlin允许我们对类的方法进行扩展，扩展方法可以定义在任何位置，一般我们会创建一个Ext.kt来存放所有扩展方法

```kotlin
fun View.isShow(): Boolean {
	//this代表此对象实例
    return this.visibility == View.VISIBLE
}
```

扩展方法其实不是真的修改了类，实质上是一个静态方法，参数除了我们定义的外，还会自动加上一个被扩展类参数，当类实例调用扩展方法时，自身会作为参数传入方法中

```
public final static isShow(Landroid/view/View;)Z
    // annotable parameter count: 1 (visible)
    // annotable parameter count: 1 (invisible)
    @Lorg/jetbrains/annotations/NotNull;() // invisible, parameter 0
   L0
    ALOAD 0
    LDC "$this$isShow"
    INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull (Ljava/lang/Object;Ljava/lang/String;)V
   L1
    LINENUMBER 43 L1
    ALOAD 0
    INVOKEVIRTUAL android/view/View.getVisibility ()I
    IFNE L2
    ICONST_1
    GOTO L3
   L2
    ICONST_0
   L3
    IRETURN
   L4
    LOCALVARIABLE $this$isShow Landroid/view/View; L0 L4 0
    MAXSTACK = 2
    MAXLOCALS = 1
```

### 定义类

```kotlin 
class CheckInView : LinearLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)
}
```

### 构造方法与初始化语句

```kotlin
class Person(var name: String, var age: Int) {
	init {
        println("main constructor init")
    }
    constructor(name: String, age: Int, gender: String) : this(name, age) {

    }
}
```

写在类名后的是主构造方法，init代码块是初始化代码块，会在主构造方法中调用，`constructor`关键字声明的次构造方法。当一个类既有主构造函数又有次构造函数时，所有次构造函数都必须使用`this`关键字直接或间接的调用主构造函数。在Kotlin中没有new关键字，创建实例是与java类似，但不需要写new。

每个从构造方法由两部分组成：

- 一部分是对其他构造方法的委托
- 另一部分是由花括号包裹的代码块

```kotlin
val person = Person("Tom", 18)
```

### 延迟初始化：by lazy & lateinit

`lazy`主要是修饰用`val`声明的变量；`lateinit`主要修饰用`var`声明的变量，但不能用于基本数据类型

`lazy`的背后接受一个`lambda`并返回一个`Lazy<T>`实例的函数，第一次访问该属性时，会执行`lazy`对应的`lambda`表达式并记录，后续访问该属性时只是返回记录的结果。

系统会给`lazy`属性默认加上同步锁，`LazyThreadSafetyMode.SYNCHRONIZED`，它在同一时刻只允许一个线程对`lazy`属性进行初始化，所以它是线程安全的。但若能确认该属性可并行执行，没有线程安全问题，可以给`lazy`传递`LazyThreadSafetyMode.PUBLICATION`参数，还可以给`lazy`传递`LazyThreadSafetyMode.NONE`参数，这将不会有任何线程方面的开销，当然也不会有任何线程安全的保证。

单例模式（双重校验锁式）

```kotlin
class SingletonDemo private constructor() {
    companion object {
        val instance: SingletonDemo by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        SingletonDemo() }
    }
}
```

### 修饰符

变量、方法、类的默认修饰符是public。

| 修饰符    | 类成员       | 顶层声明     | 与Java比较                                   |
| --------- | ------------ | ------------ | -------------------------------------------- |
| public    | 所有地方可见 | 所有地方可见 | 与Java中public效果相同                       |
| internal  | 模块中可见   | 模块中可见   |                                              |
| protected | 子类中可见   |              | 含义一致，但作用域除了类及子类外，包内也可见 |
| private   | 类中可见     | 文件中可见   | 私有修饰类，只有类内可见                     |

对于var来说，在生成字节码时，其实是private的，但会自动生成getter、setter，如果是val，则只生成getter，如果修饰符是private，则不会生成getter、setter

```
 private I i
  public final getI()I
  ...
 
  public final setI(I)V
   ...
```

kotlin默认的类、方法都是final的，如果需要能被继承、重写，需要加上`open`修饰符，子类在重写方法时，应该加上`override`

```kotlin
open class Person(var name: String, var age: Int) {
    init {
        println("main constructor init")
    }
    constructor(name: String, age: Int, gender: String) : this(name, age) {
    }
    open fun say() {
        println("I am person")
    }
}

class Man(name: String, age: Int) : Person(name, age) {
    override fun say() {
        println("I am man")
    }
}
```

### companion object

Kotlin中并没有static这个修饰符。在Java中，我们会把static的变量、方法与普通的放在一起声明，但static是属于类的，普通变量、方法是属于类实例的，放一起比较混乱，所以引入companion object(伴生对象)来代替static

```kotlin
class StaticTest {
    //伴生对象是可以指定名字的，不过一般都省略掉。并且可以继承类、实现接口
    companion object{
        var STATIC_VAR = 0
        fun staticMethod(str: String?) {
            println(str)
        }
    }
    
    fun test(){
        println(STATIC_VAR)
    }
}
```

```
public final class com/oneplus/bbs/ui/StaticTest$Companion {
  // access flags 0x19
  public final static INNERCLASS com/oneplus/bbs/ui/StaticTest$Companion com/oneplus/bbs/ui/StaticTest Companion
  // compiled from: StaticTest.kt
}
```

### object

object 关键字可以表达两种含义：一种是对象表达式,用于声明匿名内部类,另一种是对象声明。

```kotlin
val handler = object : Handler(){
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
   }
}
```

```kotlin
object ObjectTest{
    fun test() {
        
    }
}
```
从字节码可以看到使用object声明的类，其实是一个饿汉模式的单例类，类加载的时候，会创建一个INSTANCE实例
```
public final class com/example/myapplication/ObjectTest {
  ...
  public final static Lcom/example/myapplication/ObjectTest; INSTANCE

  // access flags 0x8
  static <clinit>()V
   L0
    LINENUMBER 34 L0
    NEW com/example/myapplication/ObjectTest
    DUP
    INVOKESPECIAL com/example/myapplication/ObjectTest.<init> ()V
    ASTORE 0
    ALOAD 0
    PUTSTATIC com/example/myapplication/ObjectTest.INSTANCE : Lcom/example/myapplication/ObjectTest;
    RETURN
    MAXSTACK = 2
    MAXLOCALS = 1
}
```

### 数据类

在使用java时，我们经常会写一下DTO类，用来将后端返回的json转换成对象，DTO类除了声明成员变量之外，还需要写对应的getter、setter方法，所以会产生许多样板代码，在kotlin中，data class在编译后将自动产生 getter, setter, equals(), hashCode(), toString() 以及 copy() 等方法，大幅精简代码。
```kotlin
data class User(val id: Int, val name: String, val avatar: String, val birthday: Long) 
```
如果该类需要被序列化，java中我们也需要实现Parcelable接口，写非常多的样板代码，而在kotlin中，仅仅需要实现Parcelable接口，加上 `@Parcelize` 注解，当然 `@Parcelize`也可以用于非data class

```kotlin
@Parcelize
data class User(val id: Int, val name: String, val avatar: String, val birthday: Long) : Parcelable
```

### 内部类

在Java中，通过在内部类的语法上增加一个static关键字，把它变成嵌套类。这一点在Kotlin中，刚好是相反的思路，默认是一个嵌套类，必须加上inner关键字才是一个内部类，也就是说可以把静态的内部类看成是嵌套类。

内部类和嵌套类的区别在于，内部类包含着对其外部类实例的引用，在内部类中可以使用外部类中的属性；而嵌套类不包含对其外部类实例的引用，所以它无法调用其外部类的属性。

### 密封类

密封类会通过一个`sealed`修饰符将其创建的子类进行限制，该类的子类只能定义在父类或者与父类同一个文件内。

当我们使用when表达式时，不用去考虑非法的情况，也就是可以省略else分支。

```kotlin
sealed class Shape {

    class Circle(val radius: Double) : Shape()

    class Rectangle(val width: Double, val height: Double) : Shape()

    class Triangle(val base: Double, val height: Double) : Shape()

}

fun getArea(shape: Shape): Double = when (shape) {
    is Shape.Circle -> Math.PI * shape.radius * shape.radius
    is Shape.Rectangle -> shape.width * shape.height
    is Shape.Triangle -> shape.base * shape.height / 2.0
}
```



### 空安全

java中，声明了一个变量，这个变量可能是空的，这导致我们使用的时候，可能出现NPE，导致App crash。

```java
String str = null
int length = str.length
```

kotlin为了消除这种问题，设计了可空类型，声明可空类型需要在原来的类型后加上`?`

```kotlin
var str: String?
```

**安全调用操作符 `?.`**

安全调用操作符只在不为空时进行调用,为空时则返回null

```kotlin
val length = str?.length
```

这样调用的返回值也是可空类型，当str是空的时候，length也是空，所以length的类型是`Int?`

**Elvis 操作符 `?:`**

Elvis操作符可以在左侧表达式为空时,用右侧的表达式作为整个表达式的值,相当于为空时提供默认值

```kotlin
val b: Int? = null
val a = b?:0
val a = if(b != null) {b} else {0}
```

### 类型强转

```kotlin
val textView = view as TextView
```

如果经过类型判断，可以直接将view当作TextView使用

```kotlin
if (view is TextView) {
    //调用TextView的setText方法
    view.text = "text"
}
```

**安全的类型强转**

下面代码中，textView的类型其实为`TextView？`，因为如果view不是TextView类型，那textView将为空。

```kotlin
val textView = view as? TextView
```

### let、with、run、apply、also方法的使用

#### let

```kotlin
object.let {
	//在函数体内使用it替代object对象去访问其公有的属性和方法
   it.todo()
   ...
}
//另一种用途 判断object为null的操作
object?.let{//表示object不为null的条件下，才会去执行let函数体
   it.todo()
}
```

`let`函数底层的`inline`扩展函数 + `lambda`结构

```kotlin
@kotlin.internal.InlineOnly
public inline fun <T, R> T.let(block: (T) -> R): R = block(this)
```

`let`只有一个`lambda`函数块`block`作为参数的函数,调用T类型对象的`let`函数，则该对象为函数的参数。在函数块内可以通过 `it` 指代该对象。返回值为函数块的最后一行或指定`return`表达式。

`let`函数的`kotlin`和`Java`转化

```kotlin
 //kotlin
 
 fun main(args: Array<String>) {
    val result = "testLet".let {
        println(it.length)
        1000
    }
    println(result)
 }
 
 //java
 
 public final class LetFunctionKt {
   public static final void main(@NotNull String[] args) {
      String var2 = "testLet";
      int var4 = var2.length();
      System.out.println(var4);
      int result = 1000;
      System.out.println(result);
   }
}
```

没有使用`let`函数的实现

```kotlin
mVideoPlayer?.setVideoView(activity.course_video_view)
	mVideoPlayer?.setControllerView(activity.course_video_controller_view)
	mVideoPlayer?.setCurtainView(activity.course_video_curtain_view)
```

使用`let`函数后的实现

```kotlin
mVideoPlayer?.let {
	   it.setVideoView(activity.course_video_view)
	   it.setControllerView(activity.course_video_controller_view)
	   it.setCurtainView(activity.course_video_curtain_view)
}
```

#### with

```kotlin
 with(object){
   //todo
 }
```

`with`函数底层的`inline`扩展函数+`lambda`结构

```kotlin
@kotlin.internal.InlineOnly
public inline fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()
```

`with`是将某对象作为函数的参数，在函数块内可以通过 `this` 指代该对象。返回值为函数块的最后一行或指定`return`表达式。

```kotlin
with(recyclerView) {
 	adapter = myAdapter
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
}
```

`with`函数的`kotlin`和`Java`转化

```kotlin
//kotlin

fun main(args: Array<String>) {
    val user = User("Kotlin", 1, "1111111")

    val result = with(user) {
        println("my name is $name, I am $age years old, my phone number is $phoneNum")
        1000
    }
    println("result: $result")
}

//java

public static final void main(@NotNull String[] args) {
    User user = new User("Kotlin", 1, "1111111");
    String var4 = "my name is "+user.getName()+", I am "+user.getAge()+" years old, my phone number is "+user.getPhoneNum();
    System.out.println(var4);
    int result = 1000;
    String var3 = "result: "+result;
    System.out.println(var3);
}
```

`with`函数适用于调用一个类的多个方法时，可以省去类名重复，直接调用类的方法既可以。

没有使用`with`的实现

```java
@Override
public void onBindViewHolder(ViewHolder holder, int position) {

   ArticleSnippet item = getItem(position);
		if (item == null) {
			return;
		}
		holder.tvNewsTitle.setText(StringUtils.trimToEmpty(item.titleEn));
		holder.tvNewsSummary.setText(StringUtils.trimToEmpty(item.summary));
		String gradeInfo = item.gradeInfo;
		String wordCount = item.length;
		String reviewNum = item.numReviews;
		String extraInfo = gradeInfo + " | " + wordCount + " | " + reviewNum;
		holder.tvExtraInfo.setText(extraInfo);
		...
}
```

使用`with`的实现

```kotlin
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        with(item) {
            holder.tvNewsTitle.text = StringUtils.trimToEmpty(titleEn)
            holder.tvNewsSummary.text = StringUtils.trimToEmpty(summary)
            holder.tvExtraInf.text = "$gradeInfo | $length | $numReviews"
            ...
        }
    }
```

#### run

```kotlin
object.run{
//todo
}
```

`run`函数的`inline`+`lambda`结构

```kotlin
@kotlin.internal.InlineOnly
public inline fun <R> run(block: () -> R): R = block()
```

run适用于let,with函数任何场景。因为run函数是let,with两个函数结合体，准确来说它弥补了let函数在函数体内必须使用it参数替代对象，在run函数中可以像with函数一样可以省略，直接访问实例的公有属性和方法，另一方面它弥补了with函数传入对象判空问题，在run函数中可以像let函数一样做判空处理。

`run`函数的`kotlin`和`java`转化

```kotlin
//kotlin

fun main(args: Array<String>) {
    val user = User("Kotlin", 1, "1111111")

    val result = user.run {
        println("my name is $name, I am $age years old, my phone number is $phoneNum")
        1000
    }
    println("result: $result")
}

//java
public static final void main(@NotNull String[] args) {
    User user = new User("Kotlin", 1, "1111111");
    String var5 = "my name is "+user.getName()+", I am "+user.getAge()+" years old, my phone number is "+user.getPhoneNum();
    System.out.println(var5);
    int result = 1000;
    String var3 = "result: "+result;
    System.out.println(var3);
}
```

对比`with`的实现，使用`run`函数优化

```kotlin
override fun onBindViewHolder(holder: ViewHolder, position: Int){
   
  getItem(position)?.run{
      holder.tvNewsTitle.text = StringUtils.trimToEmpty(titleEn)
	   holder.tvNewsSummary.text = StringUtils.trimToEmpty(summary)
	   holder.tvExtraInf = "难度：$gradeInfo | 单词数：$length | 读后感: $numReviews"
       ...   
   }
}
```

#### apply

```kotlin
object.apply{
//todo
}
```

`apply`函数的`inline`+`lambda`结构

```kotlin
@kotlin.internal.InlineOnly
public inline fun <T> T.apply(block: T.() -> Unit): T {
    block()
    return this
}
```

从结构上看，`apply`函数和`run`函数很像，唯一不同点就是各自返回的值不一样，`run`函数是以闭包形式返回最后一行代码的值，而`apply`函数返回的是传入对象的本身。

`apply`函数的`kotlin`和`Java`转化

```kotlin
//kotlin

fun main(args: Array<String>) {
    val user = User("Kotlin", 1, "1111111")

    val result = user.apply {
        println("my name is $name, I am $age years old, my phone number is $phoneNum")
        1000
    }
    println("result: $result")
}

//java

public final class ApplyFunctionKt {
   public static final void main(@NotNull String[] args) {
      User user = new User("Kotlin", 1, "1111111");
      String var5 = "my name is "+user.getName()+", I am "+user.getAge()+" years old, my phone number is "+user.getPhoneNum();
      System.out.println(var5);
      String var3 = "result: " + user;
      System.out.println(var3);
   }
}
```

`apply`一般用于一个对象实例初始化的时候，需要对对象中的属性进行赋值。整体作用功能和`run`函数很像，唯一不同点就是它返回的值是对象本身，而`run`函数是一个闭包形式返回，返回的是最后一行的值。

没有使用`apply`函数的实现

```kotlin
mSheetDialogView = View.inflate(activity, R.layout.biz_exam_plan_layout_sheet_inner, null)
mSheetDialogView.course_comment_tv_label.paint.isFakeBoldText = true
mSheetDialogView.course_comment_tv_score.paint.isFakeBoldText = true
mSheetDialogView.course_comment_tv_cancel.paint.isFakeBoldText = true
mSheetDialogView.course_comment_tv_confirm.paint.isFakeBoldText = true
mSheetDialogView.course_comment_seek_bar.max = 10
mSheetDialogView.course_comment_seek_bar.progress = 0
```

使用`apply`函数的实现

```kotlin
mSheetDialogView = View.inflate(activity, R.layout.biz_exam_plan_layout_sheet_inner, null).apply{
   course_comment_tv_label.paint.isFakeBoldText = true
   course_comment_tv_score.paint.isFakeBoldText = true
   course_comment_tv_cancel.paint.isFakeBoldText = true
   course_comment_tv_confirm.paint.isFakeBoldText = true
   course_comment_seek_bar.max = 10
   course_comment_seek_bar.progress = 0
}
```

#### also

```kotlin
object.also{
//todo
}
```

`also`函数的`inline` + `lambda`结构

```kotlin
@kotlin.internal.InlineOnly
@SinceKotlin("1.1")
public inline fun <T> T.also(block: (T) -> Unit): T {
    block(this)
    return this
}
```

`also`函数和`let`很像，唯一的区别就是返回值不一样，`let`是以闭包的形式返回，返回函数体内最后一行的值，如果最后一行空就返回一个`Unit`类型的默认值。而`also`函数返回的则是传入对象的本身

```
while (inputStream.read(buffer).also { length = it } != -1) {
 	os.write(buffer, 0, length)
}
```

| 函数名 | 定义inline的结构                                             | 函数体内使用的对象       | 返回值       | 是否是扩展函数 | 适用的场景                                                   |
| ------ | ------------------------------------------------------------ | ------------------------ | ------------ | -------------- | ------------------------------------------------------------ |
| let    | fun <T, R> T.let(block: (T) -> R): R = block(this)           | it指代当前对象           | 闭包形式返回 | 是             | 适用于处理不为null的操作场景                                 |
| with   | fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block() | this指代当前对象或者省略 | 闭包形式返回 | 否             | 适用于调用同一个类的多个方法时，可以省去类名重复，直接调用类的方法即可，经常用于Android中RecyclerView中onBinderViewHolder中，数据model的属性映射到UI上 |
| run    | fun <T, R> T.run(block: T.() -> R): R = block()              | this指代当前对象或者省略 | 闭包形式返回 | 是             | 适用于let,with函数任何场景。                                 |
| apply  | fun T.apply(block: T.() -> Unit): T { block(); return this } | this指代当前对象或者省略 | 返回this     | 是             | 1、适用于run函数的任何场景，一般用于初始化一个对象实例的时候，操作对象属性，并最终返回这个对象。 2、动态inflate出一个XML的View的时候需要给View绑定数据也会用到. 3、一般可用于多个扩展函数链式调用 4、数据model多层级包裹判空处理的问题 |
| also   | fun T.also(block: (T) -> Unit): T { block(this); return this } | it指代当前对象           | 返回this     | 是             | 适用于let函数的任何场景，一般可用于多个扩展函数链式调用      |

### 高阶函数

高阶函数：任何以lambda或者函数引用作为参数的函数，或者返回值为lambda或函数引用的函数，或者两者都满足的函数都是高阶函数。

高阶函数是将函数用作参数或返回值的函数。Kotlin的函数是一等公民，因此函数本身也具有自己 的类型 。

```kotlin
fun test(body: () -> String): String {
   return body()
}
```

在上面代码中，body: () -> String就是一个参数，类型是 () ->String，()代表没有参数，->String代表返回值是String

在调用该方法时，我们需要传入一个函数作为参数

```kotlin
//传入普通函数
fun body(): String {
    return "test"
}
test(this::body)

//传入匿名函数
test(fun():String{return "test"})
```

如果是匿名函数，我们可以再进一步简化成lambda表达式

```kotlin
test({return "test"})
```

kotlin中，如果最后一个参数是lambda函数，可以写到`()`外面，如果lambda是唯一参数，写到外面后,`()`就可以省略，所以也可以写成

```kotlin
test {return "test"}
```

我们再回去看上面with函数的参数，发现第二个参数是block: T.() -> R，这是一个特殊的函数，叫带接收者的函数，可以通过指定的接收者对象来调用一个函数字面值。在函数字面值内部，你可以调用接收者对象的方法而无需使用任何额外的修饰符，这一点非常类似于扩展函数。我们写一个不带泛型的简单例子

```kotlin
class Receiver {
   fun doSomething() { }
}

fun test(receiver: Receiver, block: Receiver.() -> Unit) {
    receiver.block()
}
//调用
test(Receiver(), fun Receiver.(){ doSomething() })

//简写成lambda
test(Receiver(), { doSomething() })
```

例如标准库中的filter函数将一个判断式函数作为参数，就是一个高阶函数：

```kotlin
list.filter{ x > 0}
```

`filter`函数的声明，以一个判断式作为参数

```kotlin
public inline fun String.filter(predicate: (Char) -> Boolean): String {
    return filterTo(StringBuilder(), predicate).toString()
}

public inline fun <C : Appendable> CharSequence.filterTo(destination: C, predicate: (Char) -> Boolean): C {
    for (index in 0 until length) {
        val element = get(index)
        // 调用作为参数传递给“predicate”的函数
        if (predicate(element)) destination.append(element)
    }
    return destination
}

// 传递一个lambda作为“predicate”参数
>>> println("ab1c".filter{ it in 'a'..'z'})
abd
```

在Java中使用函数类

```kotlin
/* Kotlin 声明*/
fun processTheAnswer(f: (Int) -> Int) {
    println(f(42))
}

>>> processTheAnswer(number -> number + 1)
43
```

在Java中，可以传递一个实现了函数接口中的`invoke`方法的匿名类的实例：

```java
>>> processTheAnswer(new Function1<Integer, Integer>() {
    @Override
    public Integer invoke(Integer number) {
        System.out.println(number);
        return number + 1;
    }
});
```

在Java中可以很容易的使用Kotlin标准库中以lambda作为参数的扩展函数。但必须要显示地传递一个接收者对象作为第一个参数：

```java
List<String> strings = new ArrayList();
strings.add("42");
CollectionsKt.forEach(strings, s -> {
    System.out.println(s);
    return Unit.INSTANCE;
});
```

### inline关键字

我们查看上面with方法的源码时，会发现，方法声明上有个`inline`

```kotlin
public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return receiver.block()
}
```

我们可以尝试自己定义一个inline，看会是什么结果

```kotlin
private  inline fun print() {
    print("test")
}

private fun test() {
    print()
}
```

```
private final test()V
   ...
   L4
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    ALOAD 3
    INVOKEVIRTUAL java/io/PrintStream.print (Ljava/lang/Object;)V
   ...	
```

可以看到test方法并没有调用print方法，而是直接将print逻辑直接复制到test中，减少了方法的调用，优化代码，但同时Android Studio也出现下列提示

> Expected performance impact from inlining is insignificant. Inlining works best for functions with parameters of functional types

我们修改下代码，提示消失，字节码里面，print逻辑依然直接移到test。

```kotlin
private inline fun print(block: ()->Unit) {
        print("test")
        block()
}

private fun test() {
        print { Log.i("TAG", "block")}
}
```

```haskell
private final test()V
   ...
   L4
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    ALOAD 3
    INVOKEVIRTUAL java/io/PrintStream.print (Ljava/lang/Object;)V
   ...
   L8
    LINENUMBER 46 L8
    LDC "TAG"
    LDC "block"
    INVOKESTATIC android/util/Log.i (Ljava/lang/String;Ljava/lang/String;)I
    POP
```



如果我们去掉inline，发现字节码如下

```
private fun print(block: ()->Unit) {
   print("test")
   block()
}

private fun test() {
   print { Log.i("TAG", "block")}
}
```



```
private final print(Lkotlin/jvm/functions/Function0;)V
   ...
   L4
    LINENUMBER 42 L4
    ALOAD 1
    INVOKEINTERFACE kotlin/jvm/functions/Function0.invoke ()Ljava/lang/Object; (itf)
    POP
 	...
  
private final test()V
   L0
    LINENUMBER 46 L0
    ALOAD 0
    GETSTATIC com/example/myapplication/MainActivity$test$1.INSTANCE : Lcom/example/myapplication/MainActivity$test$1;
    CHECKCAST kotlin/jvm/functions/Function0
    INVOKESPECIAL com/example/myapplication/MainActivity.print (Lkotlin/jvm/functions/Function0;)V
 
```

可以看到其实我们的函数参数被封装成Function0类型对象，这也就意味着需要占用内存。如果短时间内 lambda 表达式被多次调用，大量的对象实例化就会产生内存流失。也证实一件事了，其实kotlin中的lambda是一个语法糖，在kotlin中定义了Function0、Function1、Function2...Function22等23个接口来封装lambda中的逻辑。

当一个函数被声明为inline时，它的函数体是内联的。换句话说，函数体会被直接替换到函数被调用的地方，而不是正常被调用。但不是所有lambda的函数都可以被内联。如果要内联的函数很大，将它的字节码copy到每一个调用点将会极大的增加字节码的长度。

### infix关键字

kotlin中创建map可以使用下列语句

```kotlin
val map = mapOf(1 to "one", 2 to "two", 3 to "three")
```

```to```其实是一个方法，声明时加了关键字```infix```

```kotlin
public infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)
```

**infix函数需要几个条件:**

* 只有一个参数

* 在方法前必须加infix关键字

* 必须是成员方法或者扩展方法


模仿写一个不带泛型的infix方法
```kotlin
infix fun Int.add(that: Int): Int {
    return this + that
}

println(100 add 200)
```

### operator关键字

在java中，我们对比两个字符串是否相等，调用equals方法

```java
strA.equals(strB)
```

但kotlin中却可以直接使用`==`比较

```kotlin
strA == strB
```

在Android Studio中点击`==`，却发现跳转到

```kotlin
public open operator fun equals(other: Any?): Boolean
```

Collection对`+`进行重载，所以两个list能相加

```kotlin
val list1 = listOf<Int>(1, 2, 3)
val list2 = listOf<Int>(4, 5, 6)
val list3 = list1 + list2
```

点击”+“就会跳转到下列方法

```kotlin
public operator fun <T> Collection<T>.plus(elements: Iterable<T>): List<T> {
    if (elements is Collection) {
        val result = ArrayList<T>(this.size + elements.size)
        result.addAll(this)
        result.addAll(elements)
        return result
    } else {
        val result = ArrayList<T>(this)
        result.addAll(elements)
        return result
    }
}
```

operator函数需要的条件与infix一样，我们可以对自己定义的类进行操作符重载

```kotlin
class OperatorTest(private val value:Int) {
    public operator fun plus(other: OperatorTest): OperatorTest {
        return OperatorTest(this.value + other.value)
    }
}

val o1 = OperatorTest(5)
val o2 = OperatorTest(6)
val o3 = o1 + o2
```



### 集合

Kotlin 中的集合按照可变性分类可以分为：

- 可变集合
- 不可变集合

按照类型分类可以分为：

- List集合
- Map集合
- Set集合

结合在一起就是说List，Map，Set又都可以分为可变和不可变两种。
 具体来说
 对于List

- List ——声明不可变List集合
- MutableList——声明可变List集合

对于Map

- Map——声明不可变Map集合
- MutableMap——声明可变Map集合

对于Set

- Set——声明不可变Set集合
- MutableSet——声明可变Set集合

除此之外还有四个基本接口

- Iterable ——所有集合的父类
- MutableIterable —— 继承于Iterabl接口，支持遍历的同时可以执行删除操作
- Collection —— 继承于Iterable接口，仅封装了对集合的只读方法
- MutableCollection ——继承于Iterable,Collection，封装了添加或移除集合中元素的方法

此外，记住List ，MutableList，Set，MutableSet 归根到底都是Collection的子类。

### 集合的创建

Kotlin并没有提供创建集合的函数，但我们可以使用如下方法来创建相应的集合

#### List

创建一个不可变的list

``` val mList = listOf<Int>(1, 2, 3)```

创建一个可变的list

``` val mList = mutableListOf<Int>(1, 2, 3)```

这里mutableListOf初始化了三个值，如果没有这三个值就相当于一个空集合，比如
 ```kotlin
  val mList = mutableListOf<Int>()
 ```



```kotlin
Log.i(TAG, "mList size:" + mList.size)
mList.add(1)
mList.add(2)
Log.i(TAG, "mList size:" + mList.size)
```

打印结果为

> com.kotlin.collection.example I/MainActivity: mList size:0
>  com.kotlin.collection.example I/MainActivity: mList size:2

这样就可以给需要初始值为空的列表进行赋值了，比如ListView的Adapter初始值为空的情况。
 此外还有两个声明List集合的方法

- emptyList()——创建一个空集合
- listOfNotNull ()——  创建的集合中不能插入null值

#### Set

创建一个不可变的Set

> val mList = setOf<Int>(1,2,3)

创建一个可变的Set

> val mList = mutableSetOf<Int>(1,2,3)

此外还有如下方法

- emptySet() ——创建一个空的set集合
- linkedSetOf()——  创建set链表集合

#### Map

创建一个不可变的Map

> val mList = mapOf(Pair("key1", 1), Pair("key2", 2))

或者

> val mList = mapOf("key1" to 1, "key2" to 2)

创建一个可变的Map

> val mList = mutableMapOf("key1" to 1, "key2" to 2)

推荐使用to的方式创建
 此外还有

- emptyMap()——创建一个空map
- hashMapOf()——创建一个hashMap
- linkedMapOf()——创建一个linkedMap
- sortedMapOf()——创建一个sortedMap

以上就是三种集合常见的创建方式，下面再来说说集合中的操作符，使用合适的操作符可以极大的减小你的代码量

### 操作符

#### 总数操作

##### any

说明：如果至少有一个元素符合判断条件，则返回true，否则false,例：

> val list = listOf(1, 2, 3, 4, 5)
>  list.any { it > 10 }

结果为false

##### all

说明：如果集合中所有的元素都符合判断条件，则返回true否则false,例：

> val list = listOf(1, 2, 3, 4, 5)

list.all { it < 10 }

结果为true

##### count

说明：返回集合中符合判断条件的元素总数。例：

> list.count { it <3 }

结果为2

##### fold

说明：在一个初始值的基础上从第一项到最后一项通过一个函数累计所有的元素。例：

> list.fold(0) { total, next -> total + next }

结果为15   (计算方式：0+1+2+3+4+5，第一个数0 为fold中的0，也就是初始值)

##### foldRight

说明：与fold一样，但是顺序是从最后一项到第一项。例：

> list.foldRight(0) { total, next -> total + next }

结果也为15

##### reduce

说明：与fold一样，但是没有一个初始值。通过一个函数从第一项到最后一项进行累计。例：

> list.reduce{ total, next -> total + next}

结果为15

##### reduceRight

说明：与foldRight一样，只不过没有初始值。例：

> list.reduceRight { total, next -> total + next }

结果也为15

##### forEach

说明：遍历所有元素，并执行给定的操作（类似于Java 中的for循环）。例：

> list.forEach{ Log.i(TAG,it.toString()) }

结果为：1 2 3 4 5

##### forEachIndexed

说明：与forEach作用一样，但是同时可以得到元素的index。例：

> list.forEachIndexed { index, i -> Log.i(TAG, "index:" + index + " value:" + i.toString()) }

结果为
 index:0 value:1
 index:1 value:2
 index:2 value:3
 index:3 value:4
 index:4 value:5

##### max

说明：返回集合中最大的一项，如果没有则返回null。例：

> Log.i(TAG, list.max().toString())

结果为:5

##### min

说明：返回集合中最小的一项，如果没有则返回null。例：

> Log.i(TAG, list.min().toString())

结果为:1

##### maxBy

说明：根据给定的函数返回最大的一项，如果没有则返回null。例：

> Log.i(TAG, list.maxBy { it-10 }.toString())

结果为 ：5 （因为从1到5这5个元素中只有5减去10后的值最大，所以返回元素5，注意返回的不是计算结果）

##### minBy

说明：返回最小的一项，如果没有则返回null。例：

> Log.i(TAG, list.minBy { it-10 }.toString())

结果为：1

##### sumBy

说明：返回所有每一项通过函数转换之后的数据的总和。例：

> list.sumBy { it + 2 }

结果为：25 （每个元素都加2，最后求和）

#### 过滤操作

##### drop

说明：返回去掉前n个元素的列表。例：

> val list = listOf(1, 2, 3, 4, 5)
>  var s = list.drop(2)
>  s.forEach {
>  Log.i(TAG, it.toString())
>  }

结果为 ：3 4 5（去掉了前两个元素）

##### dropWhile

说明：返回根据给定函数从第一项开始去掉指定元素的列表。例：

> list.dropWhile { it < 3 }

结果为：3 4 5

##### dropLastWhile

说明：同dropWhile，但是是从最后一项开始。例：

> list.dropLastWhile { it >3 }

结果为:1 2 3

##### filter

说明：过滤所有符合给定函数条件的元素。例：

> list.filter { it > 2 }

结果为：3 4 5

##### filterNot

说明：过滤所有不符合给定函数条件的元素。例：

> list.filterNot{ it > 2 }

结果为：1 2

##### filterNotNull

说明：过滤所有元素中不是null的元素。例：

> list.filterNotNull()

结果为：1 2 3 4 5

##### slice

说明：过滤集合中指定index的元素（其实就是获取指定index的元素)。例：

> list.slice(listOf(0,1,2))

结果为：1 2 3

##### take

说明：返回从第一个开始的n个元素。例：

> list.take(2)

结果为：1 2

##### takeLast

说明：返回从最后一个开始的n个元素。例：

> list.takeLast(2)

结果为：4 5

##### takeWhile

说明：返回从第一个开始符合给定函数条件的元素。例：

> list.takeWhile { it<3 }

结果为：1 2

#### 映射操作

##### flatMap

说明：遍历所有的元素，为每一个创建一个集合，最后把所有的集合放在一个集合中。例：

> list.flatMap { listOf(it, it + 1) }

结果为： 1 2 2 3 3 4 4 5 5 6（每个元素都执行加1后作为一个新元素）

##### groupBy

说明：返回一个根据给定函数分组后的map。例：

> list.groupBy { if (it >3) "big" else "small" }

结果为：
 small=[1, 2, 3]
 big=[4, 5]

##### map

说明：返回一个每一个元素根据给定的函数转换所组成的集合。例：

> list.map { it * 2 }

结果为：2 4 6 8 10

##### mapIndexed

说明：返回一个每一个元素根据给定的包含元素index的函数转换所组成的集合。例：

> list.mapIndexed { index, it -> index + it }

结果为：1 3 5 7 9

##### mapNotNull

说明：返回一个每一个非null元素根据给定的函数转换所组成的集合。例：

> list.mapNotNull  { it * 2 }

结果为：2 4 6 8 10

#### 顺序操作

##### reversed

说明：返回一个与指定集合相反顺序的集合。例：

> list.reversed()

结果为：5 4 3 2 1

##### sorted

说明：返回一个自然排序后的集合。例：

> val list = listOf(1, 2, 6, 4, 5)
>  var s = list.sorted()

结果为 1 2 4 5 6

##### sortedBy

说明：返回一个根据指定函数排序后的集合。例：

> val list = listOf(1, 2, 6, 4, 5)
>  var s = list.sortedBy { it - 2 }

结果为 1 2 4 5 6

##### sortedDescending

说明：返回一个降序排序后的集合。例：

> list.sortedDescending()

结果为：5 4 3 2 1

##### sortedByDescending

说明：返回一个根据指定函数降序排序后的集合。例：

> list.sortedByDescending { it % 2 }

结果为： 1 3 5 2 4

#### 生产操作

##### partition

说明：把一个给定的集合分割成两个，第一个集合是由原集合每一项元素匹配给定函数条件返回true的元素组成，第二个集合是由原集合每一项元素匹配给定函数条件返回false的元素组成。例：

```php
val list = listOf(1, 2, 3, 4, 5)
var s = list.partition { it > 2 }
s.first.forEach {
    Log.i(TAG, it.toString())
}
s.second.forEach {
    Log.i(TAG, it.toString())
}
```

结果为：
 3 4 5
 1 2

##### plus

说明：返回一个包含原集合和给定集合中所有元素的集合，因为函数的名字原因，我们可以使用+操作符。例：

> list + listOf(6, 7)

结果为： 1 2 3 4 5 6 7

##### zip

说明：返回由pair组成的List，每个pair由两个集合中相同index的元素组成。这个返回的List的大小由最小的那个集合决定。例：

> list.zip(listOf(7, 8))

结果为：(1, 7) (2, 8)

##### unzip

说明：从包含pair的List中生成包含List的Pair。例：



```php
var s = listOf(Pair(1, 2), Pair(3, 4)).unzip()
s.first.forEach {
    Log.i(TAG, it.toString())
}
s.second.forEach {
    Log.i(TAG, it.toString())
}
```

结果为：1 2 3 4

#### 元素操作

##### contains

说明：指定元素可以在集合中找到，则返回true，否则false。例：

> list.contains(1)

结果为：true

##### elementAt

说明：返回给定index对应的元素，如果index数组越界则会抛出IndexOutOfBoundsException。例：

> list.elementAt(1)

结果为：2

##### elementAtOrElse

说明：返回给定index对应的元素，如果index数组越界则会根据给定函数返回默认值。例：

> list.elementAtOrElse(5, { it + 2 })

结果为：7

##### elementAtOrNull

说明：返回给定index对应的元素，如果index数组越界则会返回null。例：

> list.elementAtOrNull(5)

结果为：null

##### first

说明：返回符合给定函数条件的第一个元素。例：

> list.first { it > 2 }

结果为：3

##### firstOrNull

说明：返回符合给定函数条件的第一个元素，如果没有符合则返回null。例：

> list.first { it > 8 }

结果为：null

##### indexOf

说明：返回指定元素的第一个index，如果不存在，则返回-1。例：

> list.indexOf(2)

结果为：1

##### indexOfFirst

说明：返回第一个符合给定函数条件的元素的index，如果没有符合则返回-1。例：

> list.indexOfFirst { it % 2 == 0 }

结果为：1

##### indexOfLast

说明：返回最后一个符合给定函数条件的元素的index，如果没有符合则返回-1。例：

> list.indexOfLast { it % 2 == 0 }

结果为：3

##### last

说明：返回符合给定函数条件的最后一个元素。例：

> list.last { it % 2 == 0 }

结果为：4

##### lastIndexOf

说明：返回指定元素的最后一个index，如果不存在，则返回-1。例：

> list.lastIndexOf(4)

结果为：3

##### lastOrNull

说明：返回符合给定函数条件的最后一个元素，如果没有符合则返回null。例：

> list.lastOrNull { it > 6 }

结果为：null

##### single

说明：返回符合给定函数的单个元素，如果没有符合或者超过一个，则抛出异常。例：

> list.single { it > 4 }

结果为：5

##### singleOrNull

说明：返回符合给定函数的单个元素，如果没有符合或者超过一个，则返回null。例：

> list.singleOrNull { it > 5}

结果为：null

### 协程

jvm中线程是由操作系统提供支持并进行调度的，一个jvm中的线程就是一个Windows\Linux。但常见操作系统中并没有协程的概念，目前编程语言的协程应该都是开发语言层面的技术。例如kotlin的协程，其实就是基于线程池的框架。

所以线程池的优点

* 降低系统资源消耗，通过重用已存在的线程，降低线程创建和销毁造成的消耗；

* 提高系统响应速度，当有任务到达时，通过复用已存在的线程，无需等待新线程的创建便能立即执行；

* 方便线程并发数的管控。因为线程若是无限制的创建，可能会导致内存占用过多而产生OOM，并且会造成cpu过度切换（cpu切换线程是有时间成本的，需要保持当前执行线程的现场，并恢复要执行线程的现场）。

* 提供更强大的功能，延时定时线程池。

这些kotlin的协程都有，而且为了避免回调地狱，协程还提供了让我们用同步代码的方式去写异步逻辑。在要使用协程需要导入额外的库

```groovy
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:x.x.x'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:x.x.x'
```

```kotlin
class MainActivity : AppCompatActivity() {
    private val mainScope = MainScope()
    private val ioScope = CoroutineScope(Dispatchers.IO)
    fun loadData() = mainScope.launch {
        showLoading()  //ui thread
        val result = withContext(ioScope.coroutineContext){
            fetchFromDb()
        }
        showData(result)   // ui thread
    }

    private fun showData(result: List<String>) {

    }

    private suspend fun fetchFromDb(): List<String> {
        delay(100)
        return listOf("a", "b", "c")
    }

    private fun showLoading() {

    }
}    
```





  