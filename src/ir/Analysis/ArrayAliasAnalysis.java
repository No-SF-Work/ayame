package ir.Analysis;

import ir.MyFactoryBuilder;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Constants.ConstantArray;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.*;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import util.IList.INode;

public class ArrayAliasAnalysis {

  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  private static class ArrayDefUses {

    public Value array;
    public ArrayList<LoadInst> loads;
    public ArrayList<Instruction> defs;

    public ArrayDefUses() {
      this.loads = new ArrayList<>();
      this.defs = new ArrayList<>();
    }
  }

  private static class RenameData {

    public BasicBlock bb;
    public BasicBlock pred;
    public ArrayList<Value> values;

    public RenameData(BasicBlock bb, BasicBlock pred, ArrayList<Value> values) {
      this.bb = bb;
      this.pred = pred;
      this.values = values;
    }
  }

  // Use array as memory unit
  public static Value getArrayValue(Value pointer) {
    while (pointer instanceof GEPInst) {
      pointer = ((Instruction) pointer).getOperands().get(0);
    }
    // pointer should be an AllocaInst or GlobalVariable
    if (pointer instanceof AllocaInst || pointer instanceof GlobalVariable) {
      return pointer;
    } else {
      return null;
    }
  }

  public static boolean isGlobal(Value array) {
    return array instanceof GlobalVariable;
  }

  public static boolean isParam(Value array) {
    // allocaType 为 i32ptr，表示是一个参数数组
    if (array instanceof AllocaInst) {
      AllocaInst allocaInst = (AllocaInst) array;
      return allocaInst.getAllocatedType().isPointerTy();
    }
    return false;
  }

  public static boolean isLocal(Value array) {
    return !isGlobal(array) && !isParam(array);
  }

  // TODO: 我裂了
  public static boolean aliasGlobalParam(Value globalArray, Value paramArray) {
    if (!isGlobal(globalArray) || !isParam(paramArray)) {
      return false;
    }
    int dimNum1, dimNum2;
    ArrayList<Integer> dims1 = new ArrayList<>();
    ArrayList<Integer> dims2 = new ArrayList<>();

    ConstantArray globalArr = (ConstantArray) ((GlobalVariable) globalArray).init;
    dims1.addAll(globalArr.getDims());
    dimNum1 = dims1.size();
    for (var i = dimNum1 - 2; i >= 0; i--) {
      dims1.set(i, dims1.get(i) * dims1.get(i + 1));
    }

    AllocaInst allocaInst = (AllocaInst) paramArray;
    PointerType ptrTy = (PointerType) allocaInst.getAllocatedType();
    if (ptrTy.getContained().isI32()) {
      return true;
    } else {
      ArrayType arrayType = (ArrayType) ptrTy.getContained();
      dims2.add(0);
      dims2.addAll(arrayType.getDims());
      dimNum2 = dims2.size();
    }
    for (var i = dimNum2 - 2; i >= 0; i--) {
      dims2.set(i, dims2.get(i) * dims2.get(i + 1));
    }

    // dims从右向左累乘
    boolean allSame = true;
    var minDim = Math.min(dimNum1, dimNum2);
    for (var i = 0; i < minDim; i++) {
      // dims2[0] 始终为 0
      if (i == 0 && minDim == dimNum2) {
        continue;
      }
      allSame = dims1.get(i + dimNum1 - minDim) == dims2.get(i + dimNum2 - minDim);
    }
    return allSame;
  }

  // TODO: 可能有问题，还不太懂
  public static boolean alias(Value arr1, Value arr2) {
    // 都是param: 名字相等
    // param - glob: dim_alias
    // global - global: AllocaInst 相同
    // local - local: AllocaInst 相同
    if ((isGlobal(arr1) && isGlobal(arr2)) || (isParam(arr1) && isParam(arr2)) || (isLocal(arr1)
        && isLocal(arr2))) {
      return arr1 == arr2;
    }
    if (isGlobal(arr1) && isParam(arr2)) {
      return aliasGlobalParam(arr1, arr2);
    }
    if (isParam(arr1) && isGlobal(arr2)) {
      return aliasGlobalParam(arr2, arr1);
    }
    return false;
  }

  // TODO: 可能有问题，还不太懂
  public static boolean callAlias(Value arr, CallInst callinst) {
    // 条件宽泛到了不是 local array 就可能 alias，不管是 Global 还是 Param，都是外来的，都有可能被 call 改变
    if (isGlobal(arr) || isParam(arr)) {
      return true;
    }
    for (Value arg : callinst.getOperands()) {
      if (arg instanceof GEPInst) {
        GEPInst gepInst = (GEPInst) arg;
        if (alias(arr, getArrayValue(gepInst))) {
          return true;
        }
      }
    }
    return false;
  }

  public static void run(Function function) {
    DomInfo.computeDominanceInfo(function);
    DomInfo.computeDominanceFrontier(function);

    ArrayList<ArrayDefUses> arrays = new ArrayList<>();
    HashMap<Value, Integer> arraysLookup = new HashMap<>();
    ArrayList<ArrayList<BasicBlock>> defBlocks = new ArrayList<>();

    // initialize
    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.tag == TAG_.Load) {
          LoadInst loadInst = (LoadInst) inst;
          if (loadInst.getOperands().get(0) instanceof AllocaInst) {
            continue;
          }
          Value array = getArrayValue(loadInst.getOperands().get(0));
          if (arraysLookup.get(array) == null) {
            ArrayDefUses newArray = new ArrayDefUses();
            arrays.add(newArray);
            arraysLookup.put(array, arrays.size() - 1);
            defBlocks.add(new ArrayList<>());
          }
          arrays.get(arraysLookup.get(array)).loads.add(loadInst);
        }
      }
    }

    for (ArrayDefUses arrayDefUse : arrays) {
      Value array = arrayDefUse.array;
      int index = arraysLookup.get(array);
      for (INode<BasicBlock, Function> bbNode : function.getList_()) {
        BasicBlock bb = bbNode.getVal();
        for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
          Instruction inst = instNode.getVal();
          // 这里对 Load/Store/Call 进行分组，粒度决定了后面分析的精度和速度
          if (inst.tag == TAG_.Store) {
            StoreInst storeInst = (StoreInst) inst;
            if (alias(array, getArrayValue(storeInst.getOperands().get(1)))) {
              arrayDefUse.defs.add(inst);
              defBlocks.get(index).add(bb);
            }
          } else if (inst.tag == TAG_.Call) {
            Function func = (Function) inst.getOperands().get(0);
            if (func.isHasSideEffect() && callAlias(array, (CallInst) inst)) {
              arrayDefUse.defs.add(inst);
              defBlocks.get(index).add(bb);
            }
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
        if (inst.tag == TAG_.MemPhi) {
          MemPhi memPhiInst = (MemPhi) inst;
          int index = phiToArrayMap.get(memPhiInst);
          currValues.set(index, memPhiInst);
        } else if (inst.tag == TAG_.Load) {
          // set useStore as corresponding value
          LoadInst loadInst = (LoadInst) inst;
          int index = arraysLookup.get(loadInst.getOperands().get(0));
          loadInst.setUseStore(currValues.get(index));
        } else if (inst.tag == TAG_.Store || inst.tag == TAG_.Call) {
          Integer index = null;
          for (ArrayDefUses arrayDefUse : arrays) {
            if (arrayDefUse.defs.contains(inst)) {
              index = arraysLookup.get(arrayDefUse.array);
            }
          }
          if (index != null) {
            currValues.set(index, inst);
          }
        }
      }

      for (BasicBlock bb : data.bb.getSuccessor_()) {
        renameDataStack.push(new RenameData(bb, data.bb, currValues));
      }
    }

    // THU also builds `load` to `store` dependency, but I don't know if it is useful.
  }

  public static void clear(Function function) {
    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst instanceof MemPhi) {
          for (var i = 0; i < inst.getNumOP(); i++) {
            inst.CoSetOperand(i, null);
          }
        } else if (inst instanceof LoadInst) {
          LoadInst loadInst = (LoadInst) inst;
          loadInst.removeUseStore();
        }
      }
    }

    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (!(inst instanceof MemPhi)) {
          break;
        }
        instNode.removeSelf();
      }
    }
  }
}
