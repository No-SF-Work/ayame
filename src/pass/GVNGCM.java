package pass;

import ir.Analysis.ArrayAliasAnalysis;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.Use;
import ir.types.IntegerType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.User;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.Phi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.IList;
import util.IList.INode;
import util.Mylogger;

public class GVNGCM implements IRPass {

  private Logger log = Mylogger.getLogger(IRPass.class);
  private static MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  private HashMap<Value, Value> valueTable = new HashMap<>();
  private HashSet<Instruction> instructionsVis = new HashSet<>();

  @Override
  public String getName() {
    return "gvngcm";
  }

  public void run(MyModule m) {
    log.info("Running pass : GVNGCM");

    // I don't know if there is a better way than using Tsinghua's
    // bbopt->gvngcm->bbopt steps:
    // while (code_changed) {
    // bb_opt();
    // gvngcm();
    // }
    // TODO: usage of gvngcm
    for (INode<Function, MyModule> funcNode : m.__functions) {
      runGVNGCM(funcNode.getVal());
    }
  }

  public Value createExpr(Instruction inst) {
    if (inst.isBinary()) {
      BinaryInst binaryInst = (BinaryInst) inst;
      Value lhs = lookupOrAdd(binaryInst.getOperands().get(0));
      Value rhs = lookupOrAdd(binaryInst.getOperands().get(1));
      // for ()
    } else {
      switch (inst.tag) {
        case GEP: {

        }

        case Load:
          break;
        case Call:
          break;
      }
    }
    return null;
  }

  public Value lookupOrAdd(Value val) {
    if (valueTable.containsKey(val)) {
      return valueTable.get(val);
    }
    valueTable.put(val, val);
    if (val.isInstruction()) {
      Instruction inst = (Instruction) val;
      if (inst.isBinary() || inst.tag == TAG_.GEP || inst.tag == TAG_.Load
          || inst.tag == TAG_.Call) {
        valueTable.put(val, createExpr(inst));
      }
    }
    return valueTable.get(val);
  }

  public void elimRedunWith(Instruction inst, Value val) {
    valueTable.remove(inst);
    inst.COReplaceAllUseWith(val);
    inst.node.removeSelf();
  }

  // return an Integer if val is a constant int
  public static Integer getConstValue(Value val) {
    if (val.getType() == factory.getI32Ty()) {
      // if val is a constant int
      return ((ConstantInt) val).getVal();
    }
    return null;
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
    if (inst.isBinary()) {
      // TODO: llvm/lib/Analysis/InstructionSimplify.cpp
      BinaryInst binaryInst = (BinaryInst) inst;
      Value lhs = binaryInst.getOperands().get(0);
      Value rhs = binaryInst.getOperands().get(1);
      Integer lhsVal = getConstValue(lhs);
      Integer rhsVal = getConstValue(rhs);
      if (lhsVal != null && rhsVal != null) {
        // constant folding
        if (binaryInst.isArithmeticBinary()) {
          elimRedunWith(binaryInst,
              ConstantInt.newOne(factory.getI32Ty(), binaryInst.evalSelf()));
        } else if (binaryInst.isLogicalBinary()) {
          elimRedunWith(binaryInst,
              ConstantInt.newOne(factory.getI1Ty(), binaryInst.evalSelf()));
        } else {
          log.info(
              "[Error: GVNGCM] lhsVal and rhsVal is constant but binaryInst is not arithmetic or logical");
        }
        continue;
      } else if (lhsVal != null) {
        // TODO: swap lhs and rhs
      }
      // TODO: fold lhs

    } else if (inst.tag == TAG_.GEP) {
      // 直接找等价
    } else if (inst.tag == TAG_.Load) {
      // const int a[const][const]: 直接取值替换
      // 普通 Load: 找到等价的 Load，或者对应地址 Store 的值
    } else if (inst.tag == TAG_.Phi) {
      // 两种情况：1. 所有 incomingVals 相同  2. 两条 phi 指令的所有 incomingVals 对应相同
      // LLVM NewGVN 的额外优化：可以识别 phi(a + b, c + d) = phi(a, c) + phi(b, d)
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

  // TODO: complete CallInst
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
        Value value = loadInst.directContent.getValue();
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
      }
      // TODO: CallInst and args
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
          BasicBlock userbb = new BasicBlock();
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
