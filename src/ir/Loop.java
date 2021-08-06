package ir;

import ir.values.BasicBlock;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst.BrInst;
import java.util.ArrayList;
import java.util.HashSet;

public class Loop {

  private Loop parentLoop;
  private BasicBlock loopHeader;
  private ArrayList<Loop> subLoops;
  private ArrayList<BasicBlock> blocks;

  private HashSet<BasicBlock> exitingBlocks; // 跳转到循环外的基本块
  private HashSet<BasicBlock> exitBlocks; // 循环跳转到的循环外基本块
  private ArrayList<BasicBlock> latchBlocks; // 跳回循环头的基本块

  // 这两个只在 SimpleFor 的循环中才计算，loop header 有两个 pred，只有一个 exiting block，只有一个 latch block
  private MemInst.Phi indVar; // 索引 phi
  private Value indVarInit; // 索引初值
  private Value indVarEnd; // 索引边界（可不可以等于边界，自己判断）
  private Instruction stepInst; // 索引迭代指令
  private Value step; // 迭代长度
  private Integer tripCount; // 迭代次数（只考虑 init/end/step 都是常量的情况）

  public Loop(Loop parentLoop) {
    this.parentLoop = parentLoop;
    this.subLoops = new ArrayList<>();
    this.blocks = new ArrayList<>();
    this.exitingBlocks = new HashSet<>();
    this.exitBlocks = new HashSet<>();
    this.latchBlocks = new ArrayList<>();
  }


  public Loop(BasicBlock header) {
    this.parentLoop = null;
    this.subLoops = new ArrayList<>();
    this.blocks = new ArrayList<>();
    this.exitingBlocks = new HashSet<>();
    this.exitBlocks = new HashSet<>();
    this.latchBlocks = new ArrayList<>();
    this.loopHeader = header;
    this.blocks.add(header);
  }

  public Loop getParentLoop() {
    return parentLoop;
  }

  public void setParentLoop(Loop parentLoop) {
    this.parentLoop = parentLoop;
  }

  public ArrayList<Loop> getSubLoops() {
    return subLoops;
  }

  public ArrayList<BasicBlock> getBlocks() {
    return blocks;
  }

  public HashSet<BasicBlock> getExitingBlocks() {
    return exitingBlocks;
  }

  public HashSet<BasicBlock> getExitBlocks() {
    return exitBlocks;
  }

  public BasicBlock getLoopHeader() {
    return loopHeader;
  }

  public MemInst.Phi getIndVar() {
    return indVar;
  }

  public ArrayList<BasicBlock> getLatchBlocks() {
    return latchBlocks;
  }

  public Instruction getStepInst() {
    return stepInst;
  }

  public Value getIndVarInit() {
    return indVarInit;
  }

  public Value getIndVarEnd() {
    return indVarEnd;
  }

  public Value getStep() {
    return step;
  }

  public Integer getTripCount() {
    return tripCount;
  }

  public void setIndVar(Phi indVar) {
    this.indVar = indVar;
  }

  public void setStepInst(Instruction stepInst) {
    this.stepInst = stepInst;
  }

  public void setIndVarInit(Value indVarInit) {
    this.indVarInit = indVarInit;
  }

  public void setIndVarEnd(Value indVarEnd) {
    this.indVarEnd = indVarEnd;
  }

  public void setStep(Value step) {
    this.step = step;
  }

  public void setTripCount(Integer tripCount) {
    this.tripCount = tripCount;
  }

  public BasicBlock getSingleLatchBlock() {
    if (latchBlocks.size() != 1) {
      return null;
    }
    return latchBlocks.get(0);
  }


  // 判断循环是否结束的 icmp 指令
  public Instruction getLatchCmpInst() {
    if (getSingleLatchBlock() == null) {
      return null;
    }
    var brInst = getSingleLatchBlock().getList().getLast().getVal();
    assert brInst instanceof BrInst;

    // dead loop or constant condition
    if (!(brInst.getOperands().get(0) instanceof Instruction)) {
      return null;
    }

    return (Instruction) brInst.getOperands().get(0);
  }

  // 循环边界
  public Value getFinalIndVar() {
    var latchCmpInst = getLatchCmpInst();
    if (latchCmpInst == null) {
      return null;
    }

    Value lhs = latchCmpInst.getOperands().get(0);
    Value rhs = latchCmpInst.getOperands().get(1);

    if (lhs == indVar || lhs == stepInst) {
      return rhs;
    }
    if (rhs == indVar || rhs == stepInst) {
      return lhs;
    }

    return null;
  }


  public Integer getLoopDepth() {
    int depth = 0;
    for (Loop curLoop = this; curLoop != null; curLoop = curLoop.parentLoop) {
      depth++;
    }
    return depth;
  }

  public BasicBlock getHeader() {
    return blocks.get(0);
  }

  // 1 pre header, 1 latch block, n exit blocks with all pred in loop
  public boolean isCanonical() {
    boolean exitPredInLoop = true;
    for (var exitBB : exitBlocks) {
      for (var pred : exitBB.getPredecessor_()) {
        if (!this.getBlocks().contains(pred)) {
          exitPredInLoop = false;
        }
      }
    }
    return latchBlocks.size() == 1 && loopHeader.getPredecessor_().size() == 2
        && exitPredInLoop;
  }

  // 1 pre header, 1 latch block, 1 exit block
  public boolean isSimpleForLoop() {
    return latchBlocks.size() == 1 && loopHeader.getPredecessor_().size() == 2
        && exitBlocks.size() == 1;
  }

  public void addBlock(BasicBlock bb) {
    var loop = this;
    while (loop != null) {
      loop.getBlocks().add(bb);
      loop = loop.getParentLoop();
    }
  }

  public void removeBlock(BasicBlock bb) {
    this.blocks.remove(bb);
  }


  public void addSubLoop(Loop subLoop) {
    this.subLoops.add(subLoop);
    subLoop.setParentLoop(this);
  }

  public void removeSubLoop(Loop subLoop) {
    this.subLoops.remove(subLoop);
    subLoop.setParentLoop(null);
  }
}
