package ir.values;

import ir.types.Type.LabelType;
import ir.values.instructions.Instruction;

import java.util.ArrayList;
import util.IList;
import util.IList.INode;

/**
 * container基本块，func持有bb，bb不持有inst， 只持有inst的引用，inst的引用放在一个链表里 Basic blocks are Valuesbecause they
 * are referenced by instructions such as branch
 * <p>
 * The type of a BasicBlock is "Type::LabelTy" because the basic block represents a label to which a
 * branch can jump.
 */
public class BasicBlock extends Value {


  public BasicBlock(String name) {
    super(name, LabelType.getType());
    //当我们新建一个BasicBlock对象的时候实际上很快就要对其进行操作了，
    //所以这里直接new了2个container
    this.predecessor_ = new ArrayList<>();
    this.successor_ = new ArrayList<>();
    this.idoms = new ArrayList<>();
    this.domers = new ArrayList<>();
    this.dominanceFrontier = new ArrayList<>();
    list_ = new IList<>(this);
    node_ = new INode<>(this);
  }

  //插入到parent的末尾
  public BasicBlock(String name, Function parent) {
    super(name, LabelType.getType());
    this.predecessor_ = new ArrayList<>();
    this.successor_ = new ArrayList<>();
    this.idoms = new ArrayList<>();
    this.domers = new ArrayList<>();
    this.dominanceFrontier = new ArrayList<>();

    list_ = new IList<>(this);
    node_ = new INode<>(this);
    node_.setParent(parent.getList_());
    this.node_.insertAtEnd(parent.getList_());
  }

  public Function getParent() {
    return node_.getParent().getVal();
  }

  public ArrayList<BasicBlock> getPredecessor_() {
    return predecessor_;
  }

  public ArrayList<BasicBlock> getSuccessor_() {
    return successor_;
  }

  public void setSuccessor_(ArrayList<BasicBlock> successor_) {
    this.successor_ = successor_;
  }

  public IList<Instruction, BasicBlock> getList() {
    return list_;
  }

  public BasicBlock getIdomer() {
    return idomer;
  }

  public ArrayList<BasicBlock> getIdoms() {
    return idoms;
  }

  public ArrayList<BasicBlock> getDomers() {
    return domers;
  }

  public void setIdomer(BasicBlock idomer) {
    this.idomer = idomer;
  }

  public void setIdoms(ArrayList<BasicBlock> idoms) {
    this.idoms = idoms;
  }

  public void setDomers(ArrayList<BasicBlock> domers) {
    this.domers = domers;
  }

  public boolean isDirty() {
    return dirty;
  }

  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  public ArrayList<BasicBlock> getDominanceFrontier() {
    return dominanceFrontier;
  }

  public INode<BasicBlock, Function> node_;
  public IList<Instruction, BasicBlock> list_; //在well form的bb里面,最后一个listNode是terminator
  protected ArrayList<BasicBlock> predecessor_;//前驱
  protected ArrayList<BasicBlock> successor_;//后继

  private boolean dirty; // 在一些对基本块的遍历中，表示已经遍历过

  // domination info
  // FIXME maybe change `ArrayList` to `HashSet` is better.
  protected BasicBlock idomer;  // 直接支配节点
  protected ArrayList<BasicBlock> idoms; // 直接支配的节点集
  protected ArrayList<BasicBlock> domers; // 支配者节点集
  protected ArrayList<BasicBlock> dominanceFrontier;
  protected Integer domLevel;

  public Integer getDomLevel() {
    return domLevel;
  }

  public void setDomLevel(Integer domLevel) {
    this.domLevel = domLevel;
  }

  public int getLoopDepth() {
    return this.node_.getParent().getVal().getLoopInfo().getLoopDepthForBB(this);
  }

  private boolean isParallelLoopHeader; // 是否是可并行循环的循环头基本块

  public boolean isParallelLoopHeader() {
    return isParallelLoopHeader;
  }

  public void setParallelLoopHeader(boolean parallelLoopHeader) {
    isParallelLoopHeader = parallelLoopHeader;
  }
}
