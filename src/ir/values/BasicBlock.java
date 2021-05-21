package ir.values;

import ir.types.Type;
import ir.values.instructions.Instruction;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * container基本块，func持有bb，bb不持有inst， 只持有inst的引用，inst的引用放在一个链表里
 */
public class BasicBlock extends Value {


  public BasicBlock(String name, Type type) {
    super(name, type);
    //当我们新建一个BasicBlock对象的时候实际上很快就要对其进行操作了，
    //所以这里直接new了3个container
    this.predecessor = new ArrayList<>();
    this.successor = new ArrayList<>();
    this.instructions = new LinkedList<>();
  }

  public void addInstruction(Instruction inst) {
  this.instructions.addLast(inst);
  }

  public BasicBlock create() {
    return null;
  }

  public Function getParent() {
    return parent;
  }

  public void setParent(Function parent) {
    this.parent = parent;
  }

  private LinkedList<Instruction> instructions;
  private Function parent;//它所属的函数
  private ArrayList<BasicBlock> predecessor;//前驱
  private ArrayList<BasicBlock> successor;//后继
}
