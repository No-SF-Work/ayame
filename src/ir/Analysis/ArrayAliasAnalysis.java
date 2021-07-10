package ir.Analysis;

import ir.MyFactoryBuilder;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.MemPhi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import util.IList.INode;

public class ArrayAliasAnalysis {

  private static MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  private static class ArrayDefUses {

    private Value array;
    private ArrayList<LoadInst> loads;
    private ArrayList<Instruction> defs;
  }

  private static class RenameData {

    BasicBlock bb;
    BasicBlock pred;
    ArrayList<Value> values;

    public RenameData(BasicBlock bb, BasicBlock pred, ArrayList<Value> values) {
      this.bb = bb;
      this.pred = pred;
      this.values = values;
    }
  }

  // Use array as memory unit
  public static Value getArrayValue(Value pointer) {
    while (((Instruction) pointer).tag == TAG_.GEP) {
      pointer = ((Instruction) pointer).getOperands().get(0);
    }
    // pointer should be an AllocaInst
    return pointer;
  }

  // TODO: 设计 alias 策略，设计 load 指令的 mem-token 策略，判断是否需要构造 store 依赖 load 的情况
  public static void run(Function function) {
    DomInfo.computeDominanceInfo(function);
    DomInfo.computeDominanceFrontier(function);

    ArrayList<ArrayDefUses> arrays = new ArrayList<>();
    HashMap<Value, Integer> arraysLookup = new HashMap<>();
    ArrayList<ArrayList<BasicBlock>> defBlocks = new ArrayList();

    // initialize
    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.tag == TAG_.Load) {
          LoadInst loadInst = (LoadInst) inst;
          Value array = getArrayValue(loadInst);
          if (arraysLookup.get(array) == null) {
            ArrayDefUses newArray = new ArrayDefUses();
            arrays.add(newArray);
            arraysLookup.put(array, arrays.size() - 1);
          }
          arrays.get(arraysLookup.get(array)).loads.add(loadInst);
        }
      }
    }

    for (ArrayDefUses arrayDefUse : arrays) {
      Value array = arrayDefUse.array;
      for (INode<BasicBlock, Function> bbNode : function.getList_()) {
        BasicBlock bb = bbNode.getVal();
        for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
          Instruction inst = instNode.getVal();
          // 这里对 Load/Store/Call 进行分组，粒度决定了后面分析的精度和速度
          if (inst.tag == TAG_.Store) {
            // TODO: alias analysis and add the bb to defBlocks
          } else if (inst.tag == TAG_.Call) {
            // TODO: alias analysis
          }
        }
      }
    }

    // insert mem-phi-instructions
    Queue<BasicBlock> W = new LinkedList<>();
    HashMap<MemPhi, Integer> phiToArrayMap = new HashMap<>();
    for (ArrayDefUses arrayDefUse : arrays) {
      Value array = arrayDefUse.array;
      int index = arraysLookup.get(array);

      for (INode<BasicBlock, Function> bbNode : function.getList_()) {
        bbNode.getVal().setDirty(false);
      }

      W.addAll(defBlocks.get(index));

      while (!W.isEmpty()) {
        BasicBlock bb = W.remove();
        for (BasicBlock y : bb.getDominanceFrontier()) {
          if (!y.isDirty()) {
            y.setDirty(true);
            MemPhi memPhiInst = new MemPhi(TAG_.MemPhi, factory.getI32Ty(), 0, array, y);
            phiToArrayMap.put(memPhiInst, index);
            if (!defBlocks.get(index).contains(y)) {
              W.add(y);
            }
          }
        }
      }
    }

    // variable renaming (set mem-token)
    ArrayList<Value> values = new ArrayList<>();
    for (int i = 0; i < arrays.size(); i++) {
      values.set(i, null);
    }
    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      bbNode.getVal().setDirty(false);
    }

    Stack<RenameData> renameDataStack = new Stack<>();
    renameDataStack.push(new RenameData(function.getList_().getEntry().getVal(), null, values));
    while (!renameDataStack.isEmpty()) {
      RenameData data = renameDataStack.pop();
      ArrayList<Value> currValues = new ArrayList<>(data.values);
      for (INode<Instruction, BasicBlock> instNode : data.bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.tag != TAG_.MemPhi) {
          break;
        }

        MemPhi memPhiInst = (MemPhi) inst;
        int predIndex = data.bb.getPredecessor_().indexOf(data.pred);
        memPhiInst.setIncomingVals(predIndex, data.values.get(phiToArrayMap.get(memPhiInst)));
      }

      if (data.bb.isDirty()) {
        continue;
      }
      data.bb.setDirty(true);
      for (INode<Instruction, BasicBlock> instNode : data.bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.tag == TAG_.Load) {
          // TODO: set mem-token as corresponding value
        } else if (inst.tag == TAG_.Store || inst.tag == TAG_.Call) {
          for (ArrayDefUses arrayDefUse : arrays) {
            if (arrayDefUse.defs.contains(inst)) {
              // TODO: update values
            }
          }
          // TODO: update values
        }
      }

      for (BasicBlock bb : data.bb.getSuccessor_()) {
        renameDataStack.push(new RenameData(bb, data.bb, currValues));
      }
    }

    // THU also builds `load` to `store` dependency, but I don't know if it is useful.
  }
}
