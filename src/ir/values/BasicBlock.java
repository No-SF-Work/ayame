package ir.values;

import ir.types.Type;
import ir.values.instructions.Instruction;

import java.util.ArrayList;
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
    //所以这里直接new了3个container
    this.predecessor = new ArrayList<>();
    this.successor = new ArrayList<>();
    this.instructions = new LinkedList<>();
  }

  public void addInstruction(Instruction inst) {
    this.instructions.addLast(inst);
  }

  /// remove self and its instructions from the module
  public void killMe() {
    for (Instruction instruction : instructions) {
      instruction.killMe();
    }
    parent.getBasicBlocks().remove(this);
  }

  public LinkedList<Instruction> getInstructions() {
    return instructions;
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

  //
  public Instruction getTerminator() {
    return terminator;
  }

  private Instruction terminator; //终结符，在well form的bb里面是最后一条指令
  protected LinkedList<Instruction> instructions;//按执行顺序
  protected Function parent;//它所属的函数
  protected ArrayList<BasicBlock> predecessor;//前驱
  protected ArrayList<BasicBlock> successor;//后继
}
