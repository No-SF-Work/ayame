**还没定好，应该还会改**
可以对着这个玩多了解下
https://mapping-high-level-constructs-to-llvm-ir.readthedocs.io/en/latest/a-quick-primer/index.html
- [ ] 移位在哪做？
- [ ] 多线程指令怎么加
- [ ] 考虑对arm汇编的翻译。
- [ ] 做方便转换到NEON SIMD的指令
- [ ] 考虑到llvm ir翻译的便捷性

### binaryops

Add
Sub 
Rsb  //逆向减法指令，用于把操作数2减去操作数1，并将结果存放到目的寄存器中
Mul
Div
Mod   得做转换

Lt <
Le <=
Ge >=
Gt >
Eq ==
Ne !=
And &&
Or ||

### terminator insts

Br  
Jump
Return

### memoryops 

Gep
Load
Store
Alloca

### other

Call
Phi
Memop
Memphi 
Const
Global
Param


Undef //未定义

