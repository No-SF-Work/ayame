package ir;

import ir.values.BasicBlock;

/**
 * Builder Pattern， 记得在新建Inst的时候存一份引用到module里的表里面， 方便进行变换， 新的Inst应该全部通过访问Builder的方式产生
 **/
public class IRBuilder {

  private IRBuilder() {
  }

  public static IRBuilder builder = new IRBuilder();

  public static IRBuilder getInstance() {
    return builder;
  }

  public void setBB(BasicBlock bb) {
    curBB = bb;
  }


  private BasicBlock curBB;
}
