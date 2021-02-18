# Archive

### Android RecyclerView

- **RecyclerView Inconsistency detected崩溃**

  java.lang.IndexOutOfBoundsException: Inconsistency detected. Invalid view holder adapter positionViewHolder，

  **原因：**

  ​		在进行数据移除或数据增加时，务必要保证Adapter中的数据和修改的数据保持一致。如果更新集合后，调用Adapter的notify方法时，Adapter的更新预期结果和实际集合更新结果不通过，就会出现异常。

  ​		对外部数据集做了两个操作：先移除数据，然后添加数据，之后notify数据集。这里，添加数数据时（Adapter的内部数据集内容还处在外部数据集移除数据之前），造成了内部和外部数据集不一致。

  **处理方法：**

  1. 写一个继承LinearLayoutManager的包装类，在onLayoutChildren()方法里try-catch捕获该异常。

  2. 在进行数据移除和数据增加时，务必保证RecyclerView的Adapter中的数据集和移除/添加等操作后的数据集保持一致

     RecyclerView#Adapter内部的数据集，可以称为是内部数据集，后者是传进去的Adapter的，可以被称为是外部数据集。更新Recycler View数据时，需要保证外部数据集和内部数据集实时保持一致。

     外部数据集同步到内部数据集，使用如下方法：

     - notifyItemRangeRemoved();

     - notifyItemRangeInserted();

     - notifyItemRangeChanged();

     - notifyDataSetChanged();

     每一次对外部数据集做改动时，都需要紧接着主动对外部数据集和内部数据集做一次同步操作。

  3. 使用notifyDataSetChanged同步外部数据集和内部数据集。

     如果对外部数据集做两次以上的操作，却只调用notifyDataSetChanged同步一次，也会出现Inconsistency detected崩溃。