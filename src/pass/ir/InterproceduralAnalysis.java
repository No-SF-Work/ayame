package pass.ir;

import ir.Analysis.ArrayAliasAnalysis;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.PointerType;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.Value;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.TerminatorInst.CallInst;
import pass.Pass.IRPass;

public class InterproceduralAnalysis implements IRPass {

  MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  @Override
  public String getName() {
    return "interproceduralAnalysis";
  }

  @Override
  public void run(MyModule m) {
    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      func.getCallerList().clear();
      func.getCalleeList().clear();
//      func.setHasSideEffect(true); // for debug
      func.setHasSideEffect(func.isBuiltin_()); // builtin 的全部都有副作用
      func.setUsedGlobalVariable(func.isBuiltin_());
    }

    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      for (var bbNode : func.getList_()) {
        var bb = bbNode.getVal();
        for (var instNode : bb.getList()) {
          var instruction = instNode.getVal();
          switch (instruction.tag) {
            case Call -> {
              Function calleeFunc = ((CallInst) instruction).getFunc();
              func.getCalleeList().add(calleeFunc);
              calleeFunc.getCallerList().add(func);
            }
            case Load -> {
              var loadInst = (LoadInst) instruction;
              var addr = loadInst.getPointer();
              if (addr instanceof MemInst.AllocaInst && ((MemInst.AllocaInst) addr)
                  .getAllocatedType().equals(factory.getI32Ty())) {
                continue;
              }
              Value pointer = ArrayAliasAnalysis.getArrayValue(addr);
              if (ArrayAliasAnalysis.isGlobal(pointer)) {
                func.setUsedGlobalVariable(true);
                GlobalVariable gv = (GlobalVariable) pointer;
                if (((PointerType) gv.getType()).getContained().isIntegerTy()) {
                  func.getLoadGVSet().add(gv);
                }
              }
            }
            case Store -> {
              var storeInst = (StoreInst) instruction;
              var addr = storeInst.getPointer();
              if (addr instanceof MemInst.AllocaInst && ((MemInst.AllocaInst) addr)
                  .getAllocatedType().equals(factory.getI32Ty())) {
                continue;
              }
              Value pointer = ArrayAliasAnalysis.getArrayValue(addr);
              if (ArrayAliasAnalysis.isGlobal(pointer) || ArrayAliasAnalysis.isParam(pointer)) {
                func.setHasSideEffect(true);
                if (ArrayAliasAnalysis.isGlobal(pointer)) {
                  GlobalVariable gv = (GlobalVariable) pointer;
                  if (((PointerType) gv.getType()).getContained().isIntegerTy()) {
                    func.getStoreGVSet().add(gv);
                  }
                }
              }
            }
          }
        }
      }
    }

    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      if (func.isHasSideEffect()) {
        dfsSideEffect(func);
      }
      if (func.isUsedGlobalVariable()) {
        dfsUsedGlobalVariable(func);
      }
    }
  }

  private void dfsSideEffect(Function func) {
    for (var callerFunc : func.getCallerList()) {
      callerFunc.getStoreGVSet().addAll(func.getStoreGVSet());
      if (!callerFunc.isHasSideEffect()) {
        callerFunc.setHasSideEffect(true);
        dfsSideEffect(callerFunc);
      }
    }
  }

  private void dfsUsedGlobalVariable(Function func) {
    for (var callerFunc : func.getCallerList()) {
      callerFunc.getLoadGVSet().addAll(func.getLoadGVSet());
      if (!callerFunc.isUsedGlobalVariable()) {
        callerFunc.setUsedGlobalVariable(true);
        dfsUsedGlobalVariable(callerFunc);
      }
    }
  }
}
