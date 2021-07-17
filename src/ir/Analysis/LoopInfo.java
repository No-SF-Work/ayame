package ir.Analysis;

import ir.Analysis.DomInfo;
import ir.Loop;
import ir.values.BasicBlock;
import ir.values.Function;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;
import util.IList.INode;

public class LoopInfo {

  // map between basic block and the most inner loop
  private HashMap<BasicBlock, Loop> bbLoopMap;
  private ArrayList<Loop> topLevelLoops;

  public LoopInfo() {
    this.bbLoopMap = new HashMap<>();
    this.topLevelLoops = new ArrayList<>();
  }

  public ArrayList<Loop> getTopLevelLoops() {
    return topLevelLoops;
  }

  public Loop getLoopForBB(BasicBlock bb) {
    return bbLoopMap.get(bb);
  }

  public Integer getLoopDepthForBB(BasicBlock bb) {
    if (bbLoopMap.get(bb) == null) {
      return 0;
    }
    return bbLoopMap.get(bb).getLoopDepth();
  }

  public Boolean isLoopHeader(BasicBlock bb) {
    // Notice: use `==` instead of `equals` just for speed.
    return bbLoopMap.get(bb).getHeader() == bb;
  }

  // Algorithm: Testing flow graph reducibility, Tarjan
  // https://blog.csdn.net/yeshahayes/article/details/97233940
  // LLVM: LoopInfoImpl.h
  public void computeLoopInfo(Function function) {
    DomInfo.computeDominanceInfo(function);

    BasicBlock entry = function.getList_().getEntry().getVal();
    Stack<BasicBlock> postOrderStack = new Stack<>();
    Stack<BasicBlock> backEdgeTo = new Stack<>();
    ArrayList<BasicBlock> postOrder = new ArrayList<>();

    postOrderStack.push(entry);
    BasicBlock curr;
    while (!postOrderStack.isEmpty()) {
      curr = postOrderStack.pop();
      postOrder.add(curr);
      for (BasicBlock child : curr.getIdoms()) {
        postOrderStack.push(child);
      }
    }
    Collections.reverse(postOrder);

    for (BasicBlock header : postOrder) {
      for (BasicBlock pred : header.getPredecessor_()) {
        if (pred.getDomers().contains(header)) {
          backEdgeTo.push(pred);
        }
      }

      if (!backEdgeTo.isEmpty()) {
        Loop loop = new Loop(header);
        while (!backEdgeTo.isEmpty()) {
          BasicBlock pred = backEdgeTo.pop();
          Loop subloop = getLoopForBB(pred);
          if (subloop == null) {
            bbLoopMap.put(pred, loop);
            if (pred == loop.getHeader()) {
              continue;
            }

            for (BasicBlock predPred : pred.getPredecessor_()) {
              backEdgeTo.push(predPred);
            }
          } else {
            while (subloop.getParentLoop() != null) {
              subloop = subloop.getParentLoop();
            }
            if (subloop == loop) {
              continue;
            }

            subloop.setParentLoop(loop);
            for (BasicBlock subHeaderPred : subloop.getHeader().getPredecessor_()) {
              if (!bbLoopMap.get(subHeaderPred).equals(subloop)) {
                backEdgeTo.push(subHeaderPred);
              }
            }
          }
        }
      }
    }

    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      bbNode.getVal().setDirty(false);
    }
    populateLoopsDFS(entry);
  }

  public void populateLoopsDFS(BasicBlock bb) {
    if (bb.isDirty()) {
      return;
    }
    bb.setDirty(true);

    for (BasicBlock succBB : bb.getSuccessor_()) {
      populateLoopsDFS(succBB);
    }

    Loop subLoop = getLoopForBB(bb);
    if (subLoop != null && bb == subLoop.getHeader()) {
      if (subLoop.getParentLoop() != null) {
        subLoop.getParentLoop().getSubLoops().add(subLoop);
      } else {
        topLevelLoops.add(subLoop);
      }

      // 反转 subLoop.getBlocks[1, size - 1]
      Collections.reverse(subLoop.getBlocks());
      subLoop.getBlocks().add(0, bb);
      subLoop.getBlocks().remove(subLoop.getBlocks().size() - 1);

      Collections.reverse(subLoop.getSubLoops());
      subLoop = subLoop.getParentLoop();
    }

    while (subLoop != null) {
      subLoop.getBlocks().add(bb);
      subLoop = subLoop.getParentLoop();
    }
  }

}
