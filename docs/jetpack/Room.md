## Room

> `Room`是一个数据持久化库，它是Architecture Component的一部分。它让`SQLiteDatabase`的使用变得简单，大大减少了重复的代码，并且把SQL查询的检查放在了编译时。

#### 从SQLite迁移到Room

##### 第一步：依赖

```groovy
dependencies {
  def room_version = "2.2.6"

  implementation "androidx.room:room-runtime:$room_version"
  kapt "androidx.room:room-compiler:$room_version"

  // optional - Kotlin Extensions and Coroutines support for Room
  implementation "androidx.room:room-ktx:$room_version"

  // optional - Test helpers
  testImplementation "androidx.room:room-testing:$room_version"
}
```

`Room`包含3个组件：

- `Database`：包含数据库持有者，并作为应用已保留的持久关系型数据的底层连接的主要接入点。

  使用 `@Database`注释的类应满足以下条件：

  - 是扩展 `RoomDatabase` 的抽象类。
  - 在注释中添加与数据库关联的实体列表。
  - 包含具有 0 个参数且返回使用 `@Dao` 注释的类的抽象方法。

  在运行时，可以通过调用 `Room.databaseBuilder() `或 `Room.inMemoryDatabaseBuilder()` 获取 `Database` 的实例。

- `Entity`：表示数据库中的表，属性会与数据库表column进行映射。
- `DAO`：包含用于访问数据库的方法，实现具体的增删改查。

**Note: **如果应用在单个进程进行，在实例化 `RoomDatabase` 对象时应该遵循单例设计模式。

如果在多个进程中进行，需要在数据库构建器调用中包含 `enableMultiInstanceInvalidation` 。这样每个进程中都有一个 `RoomDatabase` 实例，可以在一个进程中使共享数据库文件失效，并且这种失效会自动传播到其他进程中 `AppDatabase` 的实例。

##### 第二步：model类更新为`Entity`

通过为model类添加 `@Entity` 注解创建表，类的成员对应表中相应的字段。因此`entity`类应该是不包含逻辑的轻量的model类。

- 用 `@Entity` 注解并用`tableName` 属性设置表的名称；
- 使用 `@PrimaryKey` 注解把一个成员设置为主键；
- 使用 `@ColumnInfo(name = "colunm_name")` 注解设置成员对应的列名。如果成员变量名本身就可以作为列名，也可以不设置，还可以通过`defaultValue`属性设置此列的默认值；
- 如果有多个构造方法，可以使用 `@Ignore` 注解告知`Room`忽略标记的元素。

##### 第三步：创建`DAO`

`DAO`负责定义操作数据库的方法，通过使用注解来定义查询。

- 插入：创建`DAO`方法并使用 `@Insert` 对其注释时，`Room`会生成一个实现，该实现在单个事务中将所有参数插入数据库中；

- 更新：使用 `@Update` 会修改数据库中以参数形式给出的一组实体；

- 删除：使用 `@Delete` 从数据库中删除一组以参数形式给出的尸体；

- 查询：`@Query` 是`DAO`类中使用的主要注释。它允许对数据库执行读 / 写操作。`ROOM`会验证查询的返回值，以确保当返回的对象中的字段名称与查询响应中的对应列名称匹配。

  - 简单查询：

    ```kotlin
        @Dao
        interface MyDao {
            @Query("SELECT * FROM user")
            fun loadAllUsers(): Array<User>
        }
    ```

  - 将参数传递查询：`Room`会将绑定参数与方法参数进行匹配，还可以在查询中传递多个参数或多次引用这些参数。

    ```kotlin
        @Dao
        interface MyDao {
            @Query("SELECT * FROM user WHERE age > :minAge")
            fun loadAllUsersOlderThan(minAge: Int): Array<User>
        }
    ```

  - `Room`还支持各种查询方法的返回类型

    - **使用流进行响应式查询：**可以使用Kotlin的`Flow`功能确保应用的界面保持在最新状态，如需在基础数据发生变化时，使界面自动更新，可以使用返回Flow对象的查询方法

      ```kotlin
      	@Query("SELECT * FROM User")
          fun getAllUsers(): Flow<List<User>>
      ```

      只要表中的任何数据发生变化，返回的 `Flow` 对象就会再次触发查询并重新发出整个结果集。

      使用 `Flow` 的响应式查询有一个重要限制：只要对表中的任何行进行更新（无论该行是否在结果集中），`Flow` 对象就会重新运行查询。通过将 `distinctUntilChanged()`运算符应用于返回的 `Flow` 对象，可以确保仅在实际查询结果发生更改时通知界面：

      ```kotlin
      	@Dao
          abstract class UsersDao {
              @Query("SELECT * FROM User WHERE username = :username")
              abstract fun getUser(username: String): Flow<User>
      
              fun getUserDistinctUntilChanged(username:String) =
                     getUser(username).distinctUntilChanged()
          }
      ```

    - **使用Kotlin协程进行异步查询：**可以将`suspend` Kotlin关键字添加到DAO方法中，以使用Kotlin协程功能使这些方法成为异步方法。这样可以确保不会在主线程执行这些方法。

    - **使用LiveData进行可观察查询：**可以在查询方法中使用`LiveData`类型的返回值。当数据库更新时，`Room`会生成更新`LiveData`所必需的所有代码。

##### 第四步：创建数据库

需要定义一个继承了`RoomDatabase`的抽象类，并使用 `@Database` 来注解。列出它所包含的`Entity`以及操作它们的`DAO`。

可以通过`Migration`类进行增量迁移，每个 `Migration` 子类通过替换 `Migration.migrate()`方法定义 `startVersion` 和 `endVersion` 之间的迁移路径。当应用更新需要升级数据库版本时，Room 会从一个或多个 `Migration` 子类运行 `migrate()` 方法，以在运行时将数据库迁移到最新版本：

迁移可以处理多个版本（例如，如果您在从版本3升级到版本5而不是版本4时有更快的选择路径）。如果Room打开版本3的数据库，且最新版本> = 5，则Room将使用可以从3迁移到5而不是3迁移到4和4迁移到5的迁移对象。

```kotlin
database = Room.databaseBuilder(context.getApplicationContext(),
        UsersDatabase.class, DATABASE_NAME)
        .addMigrations(MIGRATION_1_2)
        .build();
```

通过`addMigrations` 添加自定义`Migration`去处理版本升级。

#### 使用Room引用复杂数据：

有时需要使用自定义数据类型，其中包含想要存储到某个数据库列中的值。如需为自定义类型添加此类支持，需要提供一个`@TypeConverter`，它可以在自定义类与Room可以保留的已知类型之间来回转换。

例如，需保留`Date`的实例，可以将等效的Unix时间戳存储在数据库中：

```kotlin
    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? {
            return value?.let { Date(it) }
        }

        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time?.toLong()
        }
    }
```

定义了两个函数，一个用于将`Date`对象转换为`Long`对象，另一个用于执行从`Long`到`Date`的反向转换。

将`@TypeConverters`注释添加到`AppDatabase`类中，以便`Room`可以使用为该`AppDatabase` 中的每个实体和`DAO`定义的转换器。



#### 附录

[使用Room将数据保存到本地数据库](https://developer.android.com/training/data-storage/room)