package ir.values;

import ir.types.IntegerType;
import ir.types.Type;
import ir.types.Type.LabelType;
import ir.values.instructions.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * container基本块，func持有bb，bb不持有inst， 只持有inst的引用，inst的引用放在一个链表里 Basic blocks are Valuesbecause they
 * are referenced by instructions such as branch
 * <p>
 * The type of a BasicBlock is "Type::LabelTy" because the basic block represents a label to which a
 * branch can jump.
 */
public class BasicBlock extends Value {


  public BasicBlock(String name, Type type) {
    super(name, type);
    //当我们新建一个BasicBlock对象的时候实际上很快就要对其进行操作了，
    //所以这里直接new了2个container
    this.predecessor = new ArrayList<>();
    this.successor = new ArrayList<>();
  }


  @Deprecated //所有直接对外暴露的Value都应该通过ValueFactory生成
  public static BasicBlock create(Function func) {
    func.getBasicBlocks().add(new BasicBlock("", LabelType.getType()));
    return null;
  }

  public Function getParent() {
    return parent;
  }

  public void setParent(Function parent) {
    this.parent = parent;
  }

  public void setEntry(Instruction entry) {
    // you should not call this func directly,
    // this is made for Instruction class
    // 要是有友元就好了
    this.entry = entry;
  }

  //you should not call this func directly
  public void setLast(Instruction last) {
    this.last = last;
  }

  public Instruction getLast() {
    return last;
  }

  public Instruction getEntry() {
    return entry;
  }


  private Instruction entry;//链表头
  private Instruction last; //链表尾，在well form的bb里面是terminator
  protected Function parent;//它所属的函数
  protected ArrayList<BasicBlock> predecessor;//前驱
  protected ArrayList<BasicBlock> successor;//后继
}
