- [x] 搭框架
- [ ] 验证

类型系统，每个Value都应该有一个类型 这里的类型指的是 IR 的类型

FunctionType -> 函数类型，存储了函数的形参的类型的list以及返回类型的type

ArrayType -> Array类型，一个ArrayType的对象代表了一个维度，多维的数组长这个样子

```java
ArrayType(
    ArrayType(
    ArrayType(intType,num_a)
    ,num_b)
    ,num_c)   //which represents int \[c]\[b]\[a]
```

IntegerType (IR 的类型)

​ I32 代表了 int32

​ I1 代表了 bool

PointerType

​ 用于和gep，load，store配套使用

​ contained 类型指的是指向的type的指针的类型，详情见下

```
https://www.youtube.com/watch?v=m8G_S5LwlTo
```

​		