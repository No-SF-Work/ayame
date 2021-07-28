package ir.Analysis;

import ir.MyFactoryBuilder;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.values.*;
import ir.values.Constants.ConstantArray;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.*;
import ir.values.instructions.TerminatorInst.CallInst;
import util.IList.INode;

import java.util.*;

public class ArrayAliasAnalysis {

  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  public static ArrayList<ArrayDefUses> arrays = new ArrayList<>();

  public static class ArrayDefUses {

    public Value array;
    public ArrayList<LoadInst> loads;
    public ArrayList<Instruction> defs;

    public ArrayDefUses() {
      this.loads = new ArrayList<>();
      this.defs = new ArrayList<>();
    }

    public ArrayDefUses(Value array) {
      this.array = array;
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
    while (pointer instanceof GEPInst || pointer instanceof LoadInst) {
      if (pointer instanceof GEPInst) {
        pointer = ((Instruction) pointer).getOperands().get(0);
      } else {
        pointer = ((LoadInst) pointer).getOperands().get(0);
      }
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
    if (isGlobal(arr1) && isParam(arr2) && ((GlobalVariable) arr1).init instanceof ConstantArray) {
      return aliasGlobalParam(arr1, arr2);
    }
    if (isParam(arr1) && isGlobal(arr2) && ((GlobalVariable) arr2).init instanceof ConstantArray) {
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

  public static void runLoadDependStore(Function function) {
    HashMap<Value, Integer> arraysLookup = new HashMap<>();
    ArrayList<ArrayList<BasicBlock>> defBlocks = new ArrayList<>();

    // initialize
    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        if (inst.tag == TAG_.Load) {
          LoadInst loadInst = (LoadInst) inst;
          if (loadInst.getPointer() instanceof AllocaInst) {
            AllocaInst allocaInst = (AllocaInst) loadInst.getPointer();
            if (allocaInst.getAllocatedType().equals(factory.getI32Ty())) {
              continue;
            }
          }
          Value array = getArrayValue(loadInst.getOperands().get(0));
          if (arraysLookup.get(array) == null) {
            ArrayDefUses newArray = new ArrayDefUses(array);
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
              storeInst.hasAlias = true;
              arrayDefUse.defs.add(storeInst);
              defBlocks.get(index).add(bb);
            }
          } else if (inst.tag == TAG_.Call) {
            Function func = (Function) inst.getOperands().get(0);
            CallInst callInst = (CallInst) inst;
            if (func.isHasSideEffect() && callAlias(array, callInst)) {
              callInst.hasAlias = true;
              arrayDefUse.defs.add(callInst);
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
            MemPhi memPhiInst = new MemPhi(TAG_.MemPhi, factory.getVoidTy(),
                y.getPredecessor_().size() + 1, array, y);
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
      values.add(new UndefValue());
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
          Integer index = arraysLookup.get(getArrayValue(loadInst.getPointer()));
          if (index == null) {
            continue;
          }
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
  }

  // avoid gcm breaks the dependence
  public static void runStoreDependLoad(Function function) {
    ArrayList<LoadInst> loads = new ArrayList<>();
    HashMap<LoadInst, Integer> loadsLookup = new HashMap<>();
//    ArrayList<ArrayList<BasicBlock>> defBlocks = new ArrayList<>();

    // insert mem-phi-instructions
    Queue<BasicBlock> W = new LinkedList<>();
    HashMap<MemPhi, Integer> phiToLoadMap = new HashMap<>();
    for (ArrayDefUses array : arrays) {
      for (LoadInst loadInst : array.loads) {
        loads.add(loadInst);
        int index = loads.size() - 1;
        loadsLookup.put(loadInst, index);

        for (INode<BasicBlock, Function> bbNode : function.getList_()) {
          bbNode.getVal().setDirty(false);
        }

        W.add(loadInst.getBB());

        while (!W.isEmpty()) {
          BasicBlock bb = W.remove();
          for (BasicBlock y : bb.getDominanceFrontier()) {
            if (!y.isDirty()) {
              y.setDirty(true);
              MemPhi memPhiInst = new MemPhi(TAG_.MemPhi, factory.getVoidTy(),
                  y.getPredecessor_().size() + 1, new UndefValue(), y);
              phiToLoadMap.put(memPhiInst, index);
              W.add(y);
            }
          }
        }
      }
    }

    // variable renaming (construct LoadDepInst)
    ArrayList<Value> values = new ArrayList<>();
    for (int i = 0; i < loads.size(); i++) {
      values.add(new UndefValue());
    }
    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      bbNode.getVal().setDirty(false);
    }

    Stack<RenameData> renameDataStack = new Stack<>();
    renameDataStack.push(new RenameData(function.getList_().getEntry().getVal(), null, values));
    while (!renameDataStack.isEmpty()) {
      RenameData data = renameDataStack.pop();
      ArrayList<Value> currValues = new ArrayList<>(data.values);

      // mem-phi update incoming values
      for (var instNode : data.bb.getList()) {
        var inst = instNode.getVal();
        if (inst.tag != TAG_.MemPhi) {
          break;
        }

        MemPhi memPhiInst = (MemPhi) inst;
        Integer index = phiToLoadMap.get(memPhiInst);
        if (index != null) {
          int predIndex = data.bb.getPredecessor_().indexOf(data.pred);
          memPhiInst.setIncomingVals(predIndex, data.values.get(index));
        }
      }

      if (data.bb.isDirty()) {
        continue;
      }
      data.bb.setDirty(true);

      // construct LoadDepInst
      for (var instNode = data.bb.getList().getEntry(); instNode != null; ) {
        var tmp = instNode.getNext();
        var inst = instNode.getVal();
        switch (inst.tag) {
          case MemPhi -> {
            MemPhi memPhiInst = (MemPhi) inst;
            Integer index = phiToLoadMap.get(memPhiInst);
            if (index != null) {
              currValues.set(index, memPhiInst);
            }
          }
          case Load -> {
            LoadInst loadInst = (LoadInst) inst;
            currValues.set(loadsLookup.get(loadInst), loadInst);
          }
          case Store, Call -> {
            if ((inst.tag == TAG_.Store && ((StoreInst) inst).hasAlias) || (inst.tag == TAG_.Call
                && ((CallInst) inst).hasAlias)) {
              for (var memInst : currValues) {
                if (!memInst.getName().equals("UndefValue")) {
                  LoadDepInst loadDepInst = new LoadDepInst(inst, TAG_.LoadDep, factory.getVoidTy(),
                      1);
                  loadDepInst.setLoadDep(memInst);
                }
              }
            }
          }
        }
        instNode = tmp;
      }

      for (BasicBlock bb : data.bb.getSuccessor_()) {
        renameDataStack.push(new RenameData(bb, data.bb, currValues));
      }
    }

    while (true) {
      boolean clear = true;
      for (var bbNode : function.getList_()) {
        var bb = bbNode.getVal();
        for (var instNode = bb.getList().getEntry(); instNode != null; ) {
          var tmp = instNode.getNext();
          var inst = instNode.getVal();

          if (!(inst instanceof MemPhi)) {
            break;
          }
          MemPhi memPhi = (MemPhi) inst;
          if (memPhi.getUsesList().isEmpty() || memPhi.getUsesList().get(0) == null) {
            memPhi.node.removeSelf();
            memPhi.CORemoveAllOperand();
            clear = false;
          }

          instNode = tmp;
        }
      }
      if (clear) {
        break;
      }
    }
  }

  public static void run(Function function) {
    DomInfo.computeDominanceInfo(function);
    DomInfo.computeDominanceFrontier(function);

    arrays = new ArrayList<>();
    runLoadDependStore(function);
    runStoreDependLoad(function);
  }

  public static void clear(Function function) {
    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (INode<Instruction, BasicBlock> instNode : bb.getList()) {
        Instruction inst = instNode.getVal();
        switch (inst.tag) {
          case MemPhi, LoadDep -> {
            inst.CORemoveAllOperand();
            inst.COReplaceAllUseWith(null);
          }
          case Load -> {
            LoadInst loadInst = (LoadInst) inst;
            if (loadInst.getNumOP() == 2) {
              loadInst.removeUseStore();
            }
          }
          case Store -> {
            StoreInst storeInst = (StoreInst) inst;
            storeInst.hasAlias = false;
          }
          case Call -> {
            CallInst callInst = (CallInst) inst;
            callInst.hasAlias = false;
          }
        }
      }
    }

    for (INode<BasicBlock, Function> bbNode : function.getList_()) {
      BasicBlock bb = bbNode.getVal();
      for (var instNode = bb.getList().getEntry(); instNode != null; ) {
        var tmp = instNode.getNext();
        Instruction inst = instNode.getVal();
        if (inst instanceof MemPhi || inst instanceof LoadDepInst) {
          instNode.removeSelf();
        }
        instNode = tmp;
      }
    }
  }
}
