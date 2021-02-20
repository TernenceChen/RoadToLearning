## Collection

> 为了解决需要在任意时刻和任意位置创建任意数量的对象，Java提供了一套相当完整的集合类（collection classes）来解决这个问题。其中基本的类型有List、Set、Queue和Map。这些类型也被称作容器类。

Java集合类都可以自动地调整自己的大小。但也有一些其它特性，例如，Set对于每个值都只保存一个对象，Map是一个关联数组，允许将某些对象与其他对象关联起来。因此，与数组不同，在编程时，可以将任意数量的对象放置在集合中，而不用关心集合应该有多大。

### 基本概念

Java集合类库采用“持有对象”（holding objects）的思想，并将其分为两个不同的概念，表示为类库的基本接口：

1. **集合（Collection）** ：一个独立元素的序列，这些元素都服从一条或多条规则。**List** 必须以插入的顺序保存元素， **Set** 不能包含重复元素， **Queue** 按照*排队规则*来确定对象产生的顺序（通常与它们被插入的顺序相同）。
2. **映射（Map）** ： 一组成对的“键值对”对象，允许使用键来查找值。 **ArrayList** 使用数字来查找对象，因此在某种意义上讲，它是将数字和对象关联在一起。 **map** 允许我们使用一个对象来查找另一个对象，它也被称作*关联数组*（associative array），因为它将对象和其它对象关联在一起；或者称作*字典*（dictionary），因为可以使用一个键对象来查找值对象，就像在字典中使用单词查找定义一样。 **Map** 是强大的编程工具。

### 迭代器

在任何集合中，都必须有某种方式可以插入元素并再次获取它们。

迭代器是一个对象，它在一个序列中移动并选择该序列中的每个对象，通常被称为轻量级对象：创建它的代价小。

1. 使用 `iterator()` 方法要求集合返回一个 **Iterator**。 **Iterator** 将准备好返回序列中的第一个元素。
2. 使用 `next()` 方法获得序列中的下一个元素。
3. 使用 `hasNext()` 方法检查序列中是否还有元素。
4. 使用 `remove()` 方法将迭代器最近返回的那个元素删除。

迭代器统一了对集合的访问方式。

使用**Iterable** 接口，可以产生 **Iterator** 的任何东西。

**ListIterator** 是一个更强大的 **Iterator** 子类型，它只能由各种 **List** 类生成。 **Iterator** 只能向前移动，而 **ListIterator** 可以双向移动。它可以生成迭代器在列表中指向位置的后一个和前一个元素的索引，并且可以使用 `set()` 方法替换它访问过的最近一个元素。可以通过调用 `listIterator()` 方法来生成指向 **List** 开头处的 **ListIterator** ，还可以通过调用 `listIterator(n)` 创建一个一开始就指向列表索引号为 **n** 的元素处的 **ListIterator** 。

#### LinkedList

**LinkedList** 也像 **ArrayList** 一样实现了基本的 **List** 接口，但它在 **List** 中间执行插入和删除操作时比 **ArrayList** 更高效。然而,它在随机访问操作效率方面却要逊色一些。

**LinkedList 还添加了一些方法，使其可以被用作栈、队列或双端队列（deque）** 。

- `getFirst()` 和 `element()` 是相同的，它们都返回列表的头部（第一个元素）而并不删除它，如果 **List** 为空，则抛出 **NoSuchElementException** 异常。 `peek()` 方法与这两个方法只是稍有差异，它在列表为空时返回 **null** 。
- `removeFirst()` 和 `remove()` 也是相同的，它们删除并返回列表的头部元素，并在列表为空时抛出 **NoSuchElementException** 异常。 `poll()` 稍有差异，它在列表为空时返回 **null** 。
- `addFirst()` 在列表的开头插入一个元素。
- `offer()` 与 `add()` 和 `addLast()` 相同。 它们都在列表的尾部（末尾）添加一个元素。
- `removeLast()` 删除并返回列表的最后一个元素。

#### 集合Set

**Set** 不保存重复的元素。 

如果试图将相同对象的多个实例添加到 **Set** 中，那么它会阻止这种重复行为。 **Set** 最常见的用途是测试归属性，可以很轻松地询问某个对象是否在一个 **Set** 中。

因此，查找通常是 **Set** 最重要的操作，因此通常会选择 **HashSet** 实现，该实现针对快速查找进行了优化。

**Set** 具有与 **Collection** 相同的接口，因此没有任何额外的功能。实际上， **Set** 就是一个 **Collection** ，只是行为不同。**Set** 根据对象的“值”确定归属性。

#### 映射Map

**Map** 与数组和其他的 **Collection** 一样，可以轻松地扩展到多个维度：只需要创建一个 **Map** ，其值也是 **Map** (这些 **Map** 的值可以是其他集合，甚至是其他的 **Map** )。因此，能够很容易地将集合组合起来以快速生成强大的数据结构。

#### 队列Queue

队列是一个典型的“先进先出”（FIFO）集合。 即从集合的一端放入事物，再从另一端去获取它们，事物放入集合的顺序和被取出的顺序是相同的。队列通常被当做一种可靠的将对象从程序的某个区域传输到另一个区域的途径。

**LinkedList** 实现了 **Queue** 接口，并且提供了一些方法以支持队列行为，因此 **LinkedList** 可以用作 **Queue** 的一种实现。

#### 集合与迭代器

**Collection** 是所有序列集合共有的根接口。即因为要表示其他若干个接口的共性而出现的接口。

使用接口的一个理由是它可以使我们创建更通用的代码。通过针对接口而非具体实现来编写代码，我们的代码可以应用于更多类型的对象。

### 小结

Java提供了许多保存对象的方法：

1. 数组将数字索引与对象相关联。它保存类型明确的对象，因此在查找对象时不必对结果做类型转换。它可以是多维的，可以保存基本类型的数据。虽然可以在运行时创建数组，但是一旦创建数组，就无法更改数组的大小。
2. **Collection** 保存单一的元素，而 **Map** 包含相关联的键值对。使用 Java 泛型，可以指定集合中保存的对象的类型，因此不能将错误类型的对象放入集合中，并且在从集合中获取元素时，不必进行类型转换。各种 **Collection** 和各种 **Map** 都可以在你向其中添加更多的元素时，自动调整其尺寸大小。集合不能保存基本类型，但自动装箱机制会负责执行基本类型和集合中保存的包装类型之间的双向转换。
3. 像数组一样， **List** 也将数字索引与对象相关联，因此，数组和 **List** 都是有序集合。
4. 如果要执行大量的随机访问，则使用 **ArrayList** ，如果要经常从表中间插入或删除元素，则应该使用 **LinkedList** 。
5. 队列和堆栈的行为是通过 **LinkedList** 提供的。
6. **Map** 是一种将对象（而非数字）与对象相关联的设计。 **HashMap** 专为快速访问而设计，而 **TreeMap** 保持键始终处于排序状态，所以没有 **HashMap** 快。 **LinkedHashMap** 按插入顺序保存其元素，但使用散列提供快速访问的能力。
7. **Set** 不接受重复元素。 **HashSet** 提供最快的查询速度，而 **TreeSet** 保持元素处于排序状态。 **LinkedHashSet** 按插入顺序保存其元素，但使用散列提供快速访问的能力。
8. 不要在新代码中使用遗留类 **Vector** ，**Hashtable** 和 **Stack** 。