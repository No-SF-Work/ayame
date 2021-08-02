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

  private HashSet<BasicBlock> exitingBlocks; // blocks that jump out from the loop

  // 这两个只在 Canonical 的循环中才计算，loop header 有两个 pred，只有一个 exiting block，只有一个 latch block
  private BasicBlock latchBlock; // 跳回循环头的基本块
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
  }


  public Loop(BasicBlock header) {
    this.parentLoop = null;
    this.subLoops = new ArrayList<>();
    this.blocks = new ArrayList<>();
    this.exitingBlocks = new HashSet<>();
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


  public BasicBlock getLoopHeader() {
    return loopHeader;
  }

  public MemInst.Phi getIndVar() {
    return indVar;
  }

  public BasicBlock getLatchBlock() {
    return latchBlock;
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

  public void setLatchBlock(BasicBlock latchBlock) {
    this.latchBlock = latchBlock;
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


  // 判断循环是否结束的 icmp 指令
  public Instruction getLatchCmpInst() {
    if (latchBlock == null) {
      return null;
    }
    var brInst = latchBlock.getList().getLast().getVal();
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

  public boolean isCanonical() {
    return loopHeader.getPredecessor_().size() == 2 && exitingBlocks.size() == 1;
  }
}
