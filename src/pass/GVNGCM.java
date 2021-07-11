package pass;

import ir.Analysis.ArrayAliasAnalysis;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.Use;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.User;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.SimplifyInstruction;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;
import javax.management.ValueExp;
import pass.Pass.IRPass;
import util.IList;
import util.IList.INode;
import util.Mylogger;
import util.Pair;

// Not completed: findValueNumber, processInstruction on Load and pure call
public class GVNGCM implements IRPass {

  private Logger log = Mylogger.getLogger(IRPass.class);
  private static MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  private ArrayList<Pair<Value, Value>> valueTable = new ArrayList<>();
  private HashSet<Instruction> instructionsVis = new HashSet<>();

  @Override
  public String getName() {
    return "gvngcm";
  }

  public void run(MyModule m) {
    log.info("Running pass : GVNGCM");

    // TODO: usage of gvngcm
    for (INode<Function, MyModule> funcNode : m.__functions) {
      runGVNGCM(funcNode.getVal());
    }
  }

  public Value findValueNumber(BinaryInst binaryInst) {
    Value lhs = lookupOrAdd(binaryInst.getOperands().get(0));
    Value rhs = lookupOrAdd(binaryInst.getOperands().get(1));
    var sz = valueTable.size();
    for (var i = 0; i < sz; i++) {
      Value key = valueTable.get(i).getFirst();
      Value valueNumber = valueTable.get(i).getSecond();
      if (key.isInstruction() && ((Instruction) key).isBinary() && !binaryInst.equals(key)) {
        BinaryInst keyInst = (BinaryInst) key;
        Value lhs2 = lookupOrAdd(keyInst.getOperands().get(0));
        Value rhs2 = lookupOrAdd(keyInst.getOperands().get(1));
        boolean sameOp = binaryInst.tag == keyInst.tag;
        boolean sameOperand =
            (lhs.equals(lhs2) && rhs.equals(rhs2)) || (lhs.equals(rhs2) && rhs.equals(lhs2)
                && binaryInst.isCommutative());
        boolean sameRev =
            lhs.equals(rhs2) && rhs.equals(lhs2) && BinaryInst.isRev(binaryInst.tag, keyInst.tag);
        if ((sameOp && sameOperand) || sameRev) {
          return valueNumber;
        }
      }
    }
    return binaryInst;
  }

  public Value findValueNumber(GEPInst gepInst) {
    var sz = valueTable.size();
    for (var i = 0; i < sz; i++) {
      var key = valueTable.get(i).getFirst();
      var valueNumber = valueTable.get(i).getSecond();
      if (key.isInstruction() && ((Instruction) key).tag == TAG_.GEP && gepInst.equals(key)) {
        GEPInst keyInst = (GEPInst) key;
        boolean allSame = gepInst.getNumOP() == keyInst.getNumOP();
        if (allSame) {
          for (var j = 0; j < gepInst.getNumOP(); j++) {
            if (lookupOrAdd(gepInst.getOperands().get(j)) != lookupOrAdd(
                keyInst.getOperands().get(j))) {
              allSame = false;
              break;
            }
          }
        }
        if (allSame) {
          return valueNumber;
        }
      }
    }
    return gepInst;
  }

  public Value findValueNumber(LoadInst loadInst) {
    var sz = valueTable.size();
    for (var i = 0; i < sz; i++) {
      var key = valueTable.get(i).getFirst();
      var valueNumber = valueTable.get(i).getSecond();
      if (key.isInstruction() && ((Instruction) key).tag == TAG_.Load && loadInst.equals(key)) {
        LoadInst keyInst = (LoadInst) key;
        var allSame =
            lookupOrAdd(loadInst.getOperands().get(0)) == lookupOrAdd(keyInst.getOperands().get(0));
        allSame = allSame && loadInst.useStore == keyInst.useStore;
        if (allSame) {
          return valueNumber;
        }
      } else if (key.isInstruction() && ((Instruction) key).tag == TAG_.Store) {
        StoreInst keyInst = (StoreInst) key;
        var allSame =
            lookupOrAdd(loadInst.getOperands().get(0)) == lookupOrAdd(keyInst.getOperands().get(1));
        allSame = allSame && (loadInst.useStore.getValue() == keyInst);
        if (allSame) {
          return keyInst.getOperands().get(0);
        }
      }
    }
    return loadInst;
  }

  public Value findValueNumber(CallInst callInst) {
    return null;
  }

  public Value findValueNumber(Instruction inst) {
    if (inst.isBinary()) {
      return findValueNumber((BinaryInst) inst);
    }
    switch (inst.tag) {
      case GEP:
        return findValueNumber((GEPInst) inst);
      case Load:
        return findValueNumber((LoadInst) inst);
      case Call:
        return findValueNumber((CallInst) inst);
    }

    return null;
  }

  public Value lookupOrAdd(Value val) {
    for (var pair : valueTable) {
      if (pair.getFirst() == val) {
        return pair.getSecond();
      }
    }
    valueTable.add(new Pair<>(val, val));
    if (val.isInstruction()) {
      Instruction inst = (Instruction) val;
      if (inst.isBinary() || inst.tag == TAG_.GEP || inst.tag == TAG_.Load
          || inst.tag == TAG_.Call) {
        valueTable.add(new Pair<>(val, findValueNumber(inst)));
      }
    }
    return valueTable.get(valueTable.size() - 1).getSecond();
  }

  public void replace(Instruction inst, Value val) {
    valueTable.removeIf(pair -> pair.getFirst() == inst);
    inst.COReplaceAllUseWith(val);
    inst.node.removeSelf();
  }

  // Algorithm: Global Code Motion Global Value Numbering, Cliff Click
  // TODO: 研究更好的算法 "A Sparse Algorithm for Predicated Global Value Numbering" describes a better algorithm
  public void runGVNGCM(Function func) {
    ArrayAliasAnalysis.run(func);

    runGVN(func);

    // clear MemorySSA, dead code elimination, compute MemorySSA

    runGCM(func);
  }

  // TODO: use better algebraic simplification and unreachable code elimination
  // TODO GVN 还没有考虑 CallInst
  public void runGVN(Function func) {
    BasicBlock entry = func.getList_().getEntry().getVal();
    Stack<BasicBlock> postOrderStack = new Stack<>();
    ArrayList<BasicBlock> reversePostOrder = new ArrayList<>();
    valueTable.clear();

    // calculate reverse postorder
    for (INode<BasicBlock, Function> bbNode : func.getList_()) {
      bbNode.getVal().setDirty(false);
    }

    postOrderStack.push(entry);
    BasicBlock curr;
    while (!postOrderStack.isEmpty()) {
      curr = postOrderStack.pop();
      reversePostOrder.add(curr);
      for (BasicBlock child : curr.getSuccessor_()) {
        if (!child.isDirty()) {
          postOrderStack.push(child);
          child.setDirty(true);
        }
      }
    }

    for (BasicBlock bb : reversePostOrder) {
      runGVNOnBasicBlock(bb);
    }
  }

  public void runGVNOnBasicBlock(BasicBlock bb) {
    // TODO 可以加入消除重复 phi 指令

    for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
      Instruction inst = instNode.getVal();
      runGVNOnInstruction(inst);
    }
  }

  public void runGVNOnInstruction(Instruction inst) {
    Value v = SimplifyInstruction.simplifyInstruction(inst);
    if (!(v instanceof Instruction)) {
      replace(inst, v);
      return;
    }

    inst = (Instruction) v;

    if (inst.isBinary()) {
      replace(inst, lookupOrAdd(v));
    } else if (inst.tag == TAG_.GEP) {
      // 直接找等价
      replace(inst, lookupOrAdd(v));
    } else if (inst.tag == TAG_.Load) {
      replace(inst, lookupOrAdd(inst));
    } else if (inst.tag == TAG_.Phi) {
      // 所有 incomingVals 相同
      Phi phiInst = (Phi) inst;
      boolean sameIncoming = true;
      Value val = lookupOrAdd(phiInst.getIncomingVals().get(0));
      for (int i = 1; i < phiInst.getIncomingVals().size() && sameIncoming; i++) {
        if (!val.equals(lookupOrAdd(phiInst.getIncomingVals().get(i)))) {
          sameIncoming = false;
        }
      }
      if (sameIncoming) {
        replace(phiInst, val);
      }
      // TODO LLVM NewGVN 的额外优化：可以识别 phi(a + b, c + d) = phi(a, c) + phi(b, d)
    } else if (inst.tag == TAG_.Store) {
      valueTable.add(new Pair<>(inst, inst));
    }
  }

  // TODO: GCM 还没有考虑 CallInst 的情况
  public void runGCM(Function func) {
    func.getLoopInfo().computeLoopInfo(func);
    ArrayList<Instruction> instructions = new ArrayList<>();
    for (INode<BasicBlock, Function> bbNode : func.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        instructions.add(instNode.getVal());
      }
    }

    instructionsVis.clear();
    for (Instruction inst : instructions) {
      scheduleEarly(inst, func);
    }
    instructionsVis.clear();
    for (Instruction inst : instructions) {
      scheduleLate(inst, func);
    }
  }

  public void scheduleEarly(Instruction inst, Function func) {
    IList<Instruction, BasicBlock> entryList = func.getList_().getEntry().getVal().getList();
    if (!instructionsVis.contains(inst)) {
      instructionsVis.add(inst);

      if (canSchedule(inst)) {
        // move instruction to the end of entry bb
        inst.node.removeSelf();
        inst.node.insertAtEnd(entryList);
      }

      if (inst.isBinary() || inst.tag == TAG_.GEP || inst.tag == TAG_.Load) {
        // schedule early operands and ensure that inst is dominated by both operands
        for (Value op : inst.getOperands()) {
          if (op instanceof Instruction) {
            Instruction opInst = (Instruction) op;
            scheduleEarly(opInst, func);
            if (opInst.getBB().getDomLevel() > inst.getBB().getDomLevel()) {
              inst.node.removeSelf();
              inst.node.insertAtEnd(opInst.getBB().getList());
            }
          }
        }
      }

      if (inst.tag == TAG_.Load) {
        LoadInst loadInst = (LoadInst) inst;
        Value value = loadInst.useStore.getValue();
        if (value instanceof Instruction) {
          Instruction valueInst = (Instruction) value;
          scheduleEarly(valueInst, func);
          if (valueInst.getBB().getDomLevel() > inst.getBB().getDomLevel()) {
            inst.node.removeSelf();
            inst.node.insertAtEnd(valueInst.getBB().getList());
          }
        }
      }

      if (inst.tag == TAG_.Call) {
        // TODO
      }
    }
  }

  public void scheduleLate(Instruction inst, Function func) {
    if (canSchedule(inst) && !instructionsVis.contains(inst)) {
      instructionsVis.add(inst);

      BasicBlock lcabb = null;
      for (Use use : inst.getUsesList()) {
        User user = use.getUser();
        if (user instanceof Instruction) {
          Instruction userInst = (Instruction) user;
          scheduleLate(inst, func);
          BasicBlock userbb = null;
          if (userInst.tag == TAG_.Phi) {
            int idx = 0;
            for (Value value : ((Phi) userInst).getIncomingVals()) {
              if (value.getUsesList().contains(use)) {
                userbb = userInst.getBB().getPredecessor_().get(idx);
              }
              idx++;
            }
          } else {
            userbb = userInst.getBB();
          }
          lcabb = (lcabb == null) ? userbb : lca(lcabb, userbb);
        }
      }

      // 在 schedule early 和 schedule late 找到的上下界中找到 loop depth 最小的基本块
      // 上界：指令当前位置，下界：lcabb
      BasicBlock bestbb = lcabb;
      Integer bestbbLoopDepth = func.getLoopInfo().getLoopDepthForBB(bestbb);
      while (lcabb != inst.getBB()) {
        int currLoopDepth = func.getLoopInfo().getLoopDepthForBB(lcabb);
        if (currLoopDepth < bestbbLoopDepth) {
          bestbb = lcabb;
          bestbbLoopDepth = currLoopDepth;
        }
        lcabb = lcabb.getIdomer();
      }
      inst.node.removeSelf();
      inst.node.insertAtEnd(bestbb.getList());

      // bestbb 是 lcabb 时，可能 use inst 的指令在 inst 前面，需要把 inst 往前稍稍
      for (INode<Instruction, BasicBlock> instNode : bestbb.getList()) {
        Instruction tmpInst = instNode.getVal();
        if (tmpInst.tag != TAG_.Phi) {
          // 从 operands 里拿到 inst，和从 inst.getUsesList 里找是否有个 Use 的 user 是 tmpInst，应该没有区别吧
          if (tmpInst.getOperands().contains(inst)) {
            inst.node.removeSelf();
            inst.node.insertBefore(tmpInst.node);
            // 找到第一个就已经插在了最前面了，直接退出
            break;
          }
        }
      }
    }
  }

  public boolean canSchedule(Instruction inst) {
    return inst.isBinary() || inst.tag == TAG_.Load || inst.tag == TAG_.GEP;
    // TODO: add CallInst which is pure and has no GEP in args
  }

  public BasicBlock lca(BasicBlock a, BasicBlock b) {
    while (a.getDomLevel() < b.getDomLevel()) {
      b = b.getIdomer();
    }
    while (b.getDomLevel() < a.getDomLevel()) {
      a = a.getIdomer();
    }
    while (!(a.equals(b))) {
      a = a.getIdomer();
      b = b.getIdomer();
    }
    return a;
  }
}
