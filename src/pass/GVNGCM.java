package pass;

import ir.Analysis.ArrayAliasAnalysis;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.IntegerType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.LoadInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.IList.INode;
import util.Mylogger;

public class GVNGCM implements IRPass {

  private Logger log = Mylogger.getLogger(IRPass.class);
  private static MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  HashMap<Value, Value> valueMap = new HashMap<>();

  @Override
  public String getName() {
    return "gvngcm";
  }

  public void run(MyModule m) {
    log.info("Running pass : GVNGCM");

    // I don't know if there is a better way than using Tsinghua's bbopt->gvngcm->bbopt steps:
    // while (code_changed) {
    //    bb_opt();
    //    gvngcm();
    // }
    for (INode<Function, MyModule> funcNode : m.__functions) {
      runGVNGCM(funcNode.getVal());
    }
  }

  public Value getValueName(Value val) {
    if (valueMap.containsKey(val)) {
      return valueMap.get(val);
    }
    valueMap.put(val, val);
    // TODO find equal
    return valueMap.get(val);
  }

  public void elimRedunWith(Instruction inst, Value val) {
    valueMap.remove(inst);
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
  public void runGVNGCM(Function func) {
    ArrayAliasAnalysis.run(func);

    BasicBlock entry = func.getList_().getEntry().getVal();
    Stack<BasicBlock> postOrderStack = new Stack<>();
    ArrayList<BasicBlock> reversePostOrder = new ArrayList<>();
    valueMap.clear();

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
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.isBinary()) {
          BinaryInst binaryInst = (BinaryInst) inst;
          Value lhs = binaryInst.getOperands().get(0);
          Value rhs = binaryInst.getOperands().get(1);
          Integer lhsVal = getConstValue(lhs);
          Integer rhsVal = getConstValue(rhs);
          if (lhsVal != null && rhsVal != null) {
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
        }
      }
    }


  }
}
