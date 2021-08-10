package pass.ir;

import driver.Config;
import ir.Analysis.ArrayAliasAnalysis;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.Use;
import ir.values.*;
import ir.values.Constants.ConstantArray;
import ir.values.Constants.ConstantInt;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.*;
import ir.values.instructions.SimplifyInstruction;
import ir.values.instructions.TerminatorInst.BrInst;
import ir.values.instructions.TerminatorInst.CallInst;
import ir.values.instructions.TerminatorInst.RetInst;
import pass.Pass.IRPass;
import util.IList;
import util.IList.INode;
import util.Mylogger;
import util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;
import java.util.logging.Logger;

// TODO: 高级一点：对未修改的全局变量和数组的 Load 直接取值

/**
 * GVN: 尽可能地消除冗余的变量，同时会做常量合并、代数化简 GCM：把指令调度到支配深度尽可能深的地方
 */
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
    // TODO: usage of gvngcm
    for (INode<Function, MyModule> funcNode : m.__functions) {
      if (!funcNode.getVal().isBuiltin_()) {
        runGVNGCM(funcNode.getVal());
      }
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
      if (key instanceof GEPInst && !gepInst.equals(key)) {
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
      // FIXME: side_effect WA here!
      if (Config.getInstance().isO2 && key instanceof LoadInst && !loadInst.equals(key)) {
        LoadInst keyInst = (LoadInst) key;
        var allSame =
            lookupOrAdd(loadInst.getPointer()) == lookupOrAdd(keyInst.getPointer());
        allSame = allSame && loadInst.getUseStore() == keyInst.getUseStore();
        if (allSame) {
          return valueNumber;
        }
      }
      if (key instanceof StoreInst) {
        StoreInst keyInst = (StoreInst) key;
        var allSame =
            lookupOrAdd(loadInst.getPointer()) == lookupOrAdd(keyInst.getPointer());
        allSame = allSame && (loadInst.getUseStore() == keyInst);
        if (allSame) {
          return keyInst.getVal();
        }
      }
    }
    return loadInst;
  }

  public Value findValueNumber(CallInst callInst) {
    if (!callInst.isPureCall()) {
      return callInst;
    }
    var sz = valueTable.size();
    for (var i = 0; i < sz; i++) {
      var key = valueTable.get(i).getFirst();
      var valueNumber = valueTable.get(i).getSecond();
      if (key instanceof CallInst) {
        CallInst keyInst = (CallInst) key;
        if (callInst.getFunc() != keyInst.getFunc()) {
          continue;
        }
        var argSize = callInst.getNumOP();
        var allSame = true;
        for (var argIndex = 1; argIndex < argSize; argIndex++) {
          allSame = allSame &&
              (lookupOrAdd(callInst.getOperands().get(argIndex)) == lookupOrAdd(
                  keyInst.getOperands().get(argIndex)));
        }
        if (allSame) {
          return valueNumber;
        }
      }
    }
    return callInst;
  }

  public Value findValueNumber(Instruction inst) {
    if (inst.isBinary()) {
      return findValueNumber((BinaryInst) inst);
    }
    return switch (inst.tag) {
      case GEP -> findValueNumber((GEPInst) inst);
      case Load -> findValueNumber((LoadInst) inst);
      case Call -> findValueNumber((CallInst) inst);
      default -> null;
    };

  }

  public Value lookupOrAdd(Value val) {
    for (var pair : valueTable) {
      if (pair.getFirst() == val) {
        return pair.getSecond();
      }
      // 保证每个常数只会出现一次
      if (val instanceof ConstantInt && pair.getFirst() instanceof ConstantInt) {
        if (((ConstantInt) val).getVal() == ((ConstantInt) pair.getFirst()).getVal()) {
          return pair.getSecond();
        }
      }
    }
    valueTable.add(new Pair<>(val, val));
    int pos = valueTable.size() - 1;
    if (val.isInstruction()) {
      Instruction inst = (Instruction) val;
      if (inst.isBinary() || inst.tag == TAG_.GEP || inst.tag == TAG_.Load
          || inst.tag == TAG_.Call) {
        valueTable.get(pos).setSecond(findValueNumber(inst));
      }
    }
    return valueTable.get(pos).getSecond();
  }

  public void replace(Instruction inst, Value val) {
    if (inst == val) {
      return;
    }
    valueTable.removeIf(pair -> pair.getFirst() == inst);
    inst.COReplaceAllUseWith(val);
    inst.CORemoveAllOperand();
    inst.node.removeSelf();
  }

  // Algorithm: Global Code Motion Global Value Numbering, Cliff Click
  // TODO: 研究更好的算法 "A Sparse Algorithm for Predicated Global Value Numbering" describes a better algorithm
  public void runGVNGCM(Function func) {
    var bropt = new BranchOptimization();
    int cnt = 0;
    do {
      ArrayAliasAnalysis.run(func);
      log.info(("GVMGCM: GVN for " + func.getName()));
      runGVN(func);

      // clear MemorySSA, dead code elimination, compute MemorySSA
      ArrayAliasAnalysis.clear(func);
      DeadCodeEmit dce = new DeadCodeEmit();
      dce.runDCE(func);
      ArrayAliasAnalysis.run(func);

      log.info(("GVMGCM: GCM for " + func.getName()));
      runGCM(func);
      ArrayAliasAnalysis.clear(func);
      cnt++;
    } while (bropt.runBranchOptimization(func));
//    func.getLoopInfo().computeAdditionalLoopInfo();
//    System.out.println("Run GVNGCM for func " + func.getName() + " " + cnt + " times.");
  }

  // TODO: use better algebraic simplification and unreachable code elimination
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

    for (var instNode = bb.getList().getEntry(); instNode != null; ) {
      var tmp = instNode.getNext();
      runGVNOnInstruction(instNode.getVal());
      instNode = tmp;
    }
    assert bb.getList().getLast().getVal() instanceof BrInst || bb.getList().getLast()
        .getVal() instanceof RetInst;
  }

  public void runGVNOnInstruction(Instruction inst) {
    if (inst.getUsesList().size() == 0 && inst.tag != TAG_.Store && inst.tag != TAG_.Call) {
      return;
    }
    Value v = SimplifyInstruction.simplifyInstruction(inst);
    // 后续通过跑多次 GVN 来找 BinaryInst 的不动点
    // log.info("GVN optimizing instruction: " + inst.toString());
    if (!(v instanceof Instruction)) {
      replace(inst, v);
      return;
    }

    Instruction simpInst = (Instruction) v;

    if (inst.isBinary()) {
      // 循环展开时发现可能有多个 br 共用一个 icmp 的情况，而循环展开时会更改 icmp，所以不替换 icmp
      if (inst.isRelBinary()) {
        return;
      }
      Value val = lookupOrAdd(simpInst);
      if (inst != val) {
        replace(inst, val);
      }
    } else if (inst.tag == TAG_.GEP) {
      Value val = lookupOrAdd(simpInst);
      if (inst != val) {
        replace(inst, val);
      }
    } else if (inst.tag == TAG_.Load) {
      Value pointer = ((LoadInst) inst).getPointer();
      Value array = ArrayAliasAnalysis.getArrayValue(pointer);

      boolean getConst = false;
      if (pointer instanceof GEPInst && ArrayAliasAnalysis
          .isGlobal(array)) {
        GlobalVariable globalArray = (GlobalVariable) array;
        if (globalArray.isConst) {
          boolean constIndex = true;

          if (globalArray.fixedInit == null) {
            // mark global const 产生的常量数组
            ConstantInt c = ConstantInt.newOne(factory.getI32Ty(), 0);
            replace(inst, c);
            return;
          } else if (globalArray.fixedInit instanceof ConstantArray
              && ((GEPInst) pointer).getNumOP() > 2) {
            ConstantArray constantArray = (ConstantArray) globalArray.fixedInit;
            Stack<Integer> indexList = new Stack<>();
            Value tmpPtr = pointer;
            while (tmpPtr instanceof GEPInst) {
              // 不考虑基址+偏移的GEP
              if (((GEPInst) tmpPtr).getNumOP() <= 2) {
                constIndex = false;
                break;
              }
              Value index = ((Instruction) tmpPtr).getOperands().get(2);
              if (!(index instanceof ConstantInt)) {
                constIndex = false;
                break;
              }
              indexList.push(((ConstantInt) index).getVal());
              tmpPtr = ((Instruction) tmpPtr).getOperands().get(0);
            }
            if (constIndex) {
              Constant c = constantArray;
              while (!indexList.isEmpty()) {
                int index = indexList.pop();
                c = ((ConstantArray) c).getConst_arr_().get(index);
              }
              assert c instanceof ConstantInt;
              replace(inst, c);
              getConst = true;
            }
          }
        }
      }

      if (!getConst) {
        Value val = lookupOrAdd(simpInst);
        if (inst != val) {
          replace(inst, val);
        }
      }

    } else if (inst.tag == TAG_.Phi) {
      // 所有 incomingVals 相同
      Phi phiInst = (Phi) simpInst;
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
      // 避免替换掉 store pointer to pointer(pointer)
      var val = inst.getOperands().get(0);
      if (!val.getType().isPointerTy()) {
        valueTable.add(new Pair<>(inst, inst));
      }
    } else if (inst.tag == TAG_.Call && ((CallInst) inst).isPureCall()) {
      Value val = lookupOrAdd(simpInst);
      if (inst != val) {
        replace(inst, val);
      }
    }
  }

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
        inst.node.insertAtSecondToEnd(entryList);
        assert entryList.getLast().getVal() instanceof BrInst || entryList.getLast()
            .getVal() instanceof RetInst;
      }

      if (inst.isBinary() || inst.tag == TAG_.GEP || inst.tag == TAG_.Load) {
        // schedule early operands and ensure that inst is dominated by both operands
        for (Value op : inst.getOperands()) {
          if (op instanceof Instruction) {
            Instruction opInst = (Instruction) op;
            scheduleEarly(opInst, func);
            if (opInst.getBB().getDomLevel() > inst.getBB().getDomLevel()) {
              inst.node.removeSelf();
              inst.node.insertAtSecondToEnd(opInst.getBB().getList());
              assert opInst.getBB().getList().getLast().getVal() instanceof BrInst || opInst.getBB()
                  .getList().getLast().getVal() instanceof RetInst;
            }
          }
        }
      }

      // Load 的 useStore
//      if (inst.tag == TAG_.Load) {
//        LoadInst loadInst = (LoadInst) inst;
//        Value value = loadInst.getUseStore();
//        if (value instanceof Instruction) {
//          Instruction valueInst = (Instruction) value;
//          scheduleEarly(valueInst, func);
//          if (valueInst.getBB().getDomLevel() > inst.getBB().getDomLevel()) {
//            inst.node.removeSelf();
//            inst.node.insertAtEnd(valueInst.getBB().getList());
//          }
//        }
//      }

      if (inst.tag == TAG_.Call && ((CallInst) inst).isPureCall()) {
        for (var i = 1; i < inst.getNumOP(); i++) {
          Value value = inst.getOperands().get(i);
          if (value instanceof Instruction) {
            Instruction valueInst = (Instruction) value;
            scheduleEarly(valueInst, func);
            if (valueInst.getBB().getDomLevel() > inst.getBB().getDomLevel()) {
              inst.node.removeSelf();
              inst.node.insertAtSecondToEnd(valueInst.getBB().getList());
            }
          }
        }
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
          scheduleLate(userInst, func);
          BasicBlock userbb = userInst.getBB();
          ;
          if (userInst.tag == TAG_.Phi) {
            int idx = 0;
            for (Value value : ((Phi) userInst).getIncomingVals()) {
              if (value.getUsesList().contains(use)) {
                userbb = userInst.getBB().getPredecessor_().get(idx);
                lcabb = (lcabb == null) ? userbb : lca(lcabb, userbb);
              }
              idx++;
            }
          }
          // FIXME maybe problem here
          else if (userInst.tag == TAG_.MemPhi) {
            int idx = 0;
            for (Value value : ((MemPhi) userInst).getIncomingVals()) {
              if (value.getUsesList().contains(use)) {
                userbb = userInst.getBB().getPredecessor_().get(idx);
                lcabb = (lcabb == null) ? userbb : lca(lcabb, userbb);
              }
              idx++;
            }
          }

          lcabb = (lcabb == null) ? userbb : lca(lcabb, userbb);
        }
      }
      // 在 schedule early 和 schedule late 找到的上下界中找到 loop depth 最小的基本块
      // 上界：指令当前位置，下界：lcabb
      BasicBlock bestbb = lcabb;
      Integer bestbbLoopDepth = func.getLoopInfo().getLoopDepthForBB(bestbb);
      while (lcabb != inst.getBB()) {
        lcabb = lcabb.getIdomer();
        assert lcabb != null;
        int currLoopDepth = func.getLoopInfo().getLoopDepthForBB(lcabb);
        if (currLoopDepth < bestbbLoopDepth) {
          bestbb = lcabb;
          bestbbLoopDepth = currLoopDepth;
        }
      }
      inst.node.removeSelf();
      inst.node.insertAtSecondToEnd(bestbb.getList());
      assert bestbb.getList().getLast().getVal() instanceof BrInst || bestbb.getList().getLast()
          .getVal() instanceof RetInst;

      // bestbb 是 lcabb 时，可能 use inst 的指令在 inst 前面，需要把 inst 往前稍稍
      for (INode<Instruction, BasicBlock> instNode : bestbb.getList()) {
        Instruction tmpInst = instNode.getVal();
        if (tmpInst.tag != TAG_.Phi && tmpInst.tag != TAG_.MemPhi) {
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
    return inst.isBinary() || inst.tag == TAG_.Load || inst.tag == TAG_.GEP || (
        inst.tag == TAG_.Call && ((CallInst) inst).isPureCall());
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
