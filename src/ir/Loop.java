package ir;

import ir.values.BasicBlock;
import java.util.ArrayList;

public class Loop {
  private Loop parentLoop;
  private ArrayList<Loop> subLoops;
  private ArrayList<BasicBlock> blocks;

  public Loop(Loop parentLoop) {
    this.parentLoop = parentLoop;
    this.subLoops = new ArrayList<>();
    this.blocks = new ArrayList<>();
  }

  public Loop(BasicBlock header) {
    this.parentLoop = null;
    this.subLoops = new ArrayList<>();
    this.blocks = new ArrayList<>();
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
}
