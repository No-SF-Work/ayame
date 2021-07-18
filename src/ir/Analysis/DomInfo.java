package ir.Analysis;

import ir.values.BasicBlock;
import ir.values.Function;
import util.IList;

import java.util.ArrayList;
import java.util.BitSet;

public class DomInfo {

  /**
   * Compute domers, idomer and idoms for all the basic blocks of a function.
   *
   * @param function
   * @author Codevka
   */
  public static void computeDominanceInfo(Function function) {
    // assume that `entry` is the entry basic block as well as the head of linked list
    IList.INode<BasicBlock, Function> head = function.getList_().getEntry();
    BasicBlock entry = head.getVal();
    int numNode = function.getList_().getNumNode();
    ArrayList<BitSet> domers = new ArrayList<>(numNode);
    ArrayList<BasicBlock> bbList = new ArrayList<>();

    int index = 0;
    // clear existing dominance information and initialize
    for (IList.INode<BasicBlock, Function> iterator : function.getList_()) {
      BasicBlock bb = iterator.getVal();
      bb.getDomers().clear();
      bb.getIdoms().clear();
      bbList.add(bb);
      domers.add(new BitSet());

      if (bb == entry) {
        // use index instead of 0 to avoid case where entry is not the head of linked list
        // entry is dominated by entry
        domers.get(index).set(index);
      } else {
        // initialize other nodes with being dominated by all nodes
        domers.get(index).set(0, numNode);
      }
      index++;
    }

    // calculate domer
    // Algorithm: Engineering A Compiler P479.
    boolean changed = true;
    while (changed) {
      changed = false;

      index = 0;

      for (IList.INode<BasicBlock, Function> iterator : function.getList_()) {
        BasicBlock bb = iterator.getVal();
        // no need to consider entry node
        if (bb != entry) {
          BitSet temp = new BitSet();
          temp.set(0, numNode);

          // temp <- {index} \cup (\Bigcap_{j \in preds(index)} domer(j) )
          for (BasicBlock pre_bb : bb.getPredecessor_()) {
            int preIndex = bbList.indexOf(pre_bb);
            temp.and(domers.get(preIndex));
          }
          temp.set(index);

          if (!temp.equals(domers.get(index))) {
            // replace domers[index] with temp
            domers.get(index).clear();
            domers.get(index).or(temp);
            changed = true;
          }
        }
        index++;
      }
    }

    for (int i = 0; i < numNode; i++) {
      BasicBlock bb = bbList.get(i);
      BitSet domerInfo = domers.get(i);
      for (int domerIndex = domerInfo.nextSetBit(0); domerIndex >= 0;
          domerIndex = domerInfo.nextSetBit(domerIndex + 1)) {
        BasicBlock domerbb = bbList.get(domerIndex);
        bb.getDomers().add(domerbb);
      }
    }

    // calculate doms and idom
    for (int i = 0; i < numNode; i++) {
      BasicBlock bb = bbList.get(i);

      for (BasicBlock maybeIdomerbb : bb.getDomers()) {
        if (maybeIdomerbb != bb) {
          boolean isIdom = true;
          for (BasicBlock domerbb : bb.getDomers()) {
            if (domerbb != bb && domerbb != maybeIdomerbb && domerbb.getDomers()
                .contains(maybeIdomerbb)) {
              isIdom = false;
              break;
            }
          }

          if (isIdom) {
            bb.setIdomer(maybeIdomerbb);
            maybeIdomerbb.getIdoms().add(bb);
            break;
          }
        }
      }
    }

    // calculate dom level
    computeDominanceLevel(entry, 0);
  }

  /**
   * Compute the dominance frontier of all the basic blocks of a function.
   *
   * @param function
   * @author : Codevka
   * <p>
   * Algorithm: The SSA Book P32. for (a, b) \in CFG edges do x <- a while x does not strictly
   * dominate b do DF(x) <- DF(x) + b x <- idom(x)
   */
  public static void computeDominanceFrontier(Function function) {
    for (var bbNode : function.getList_()) {
      bbNode.getVal().getDominanceFrontier().clear();
    }

    for (IList.INode<BasicBlock, Function> iterator : function.getList_()) {
      BasicBlock a = iterator.getVal();
      for (BasicBlock b : a.getSuccessor_()) {
        BasicBlock x = a;
        while (x == b || !b.getDomers().contains(x)) {
          if (!x.getDominanceFrontier().contains(b)) {
            // maybe better to design the data structure of dominance frontier as a set
            x.getDominanceFrontier().add(b);
          }
          x = x.getIdomer();
        }
      }
    }
  }

  public static void computeDominanceLevel(BasicBlock bb, Integer domLevel) {
    bb.setDomLevel(domLevel);
    for (BasicBlock succ : bb.getIdoms()) {
      computeDominanceLevel(succ, domLevel + 1);
    }
  }
}
