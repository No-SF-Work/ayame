package ir.values;

import ir.types.Type;
import ir.values.instructions.Instruction;

import java.util.LinkedList;

/**
 * container基本块，func持有bb，bb不持有inst，只持有inst的引用，inst的引用放在一个链表里
 */
public class BasicBlock extends Value {


  public BasicBlock(String name, Type type) {
    super(name, type);
  }

  public void addInstruction(Instruction inst) {
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

  private LinkedList<Instruction> holdInstructions;
  private Function parent;

}
