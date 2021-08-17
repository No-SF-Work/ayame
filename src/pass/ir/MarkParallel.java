package pass.ir;

import ir.Analysis.ArrayAliasAnalysis;
import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.Function.Arg;
import ir.values.GlobalVariable;
import ir.values.Value;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.LoopUtils;
import util.Mylogger;

// 识别到可并行循环后，在循环头基本块处标记
public class MarkParallel implements IRPass {

  private static Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private static int parallelFactor = 4;
  private LoopInfo currLoopInfo;
  private Function currFunc;

  @Override
  public String getName() {
    return "markParallel";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : MarkParallel");

    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        runOnFunction(func);
      }
    }
  }

  public void runOnFunction(Function func) {
    Queue<Loop> loopQueue = new LinkedList<>();
    this.currLoopInfo = func.getLoopInfo();
    this.currFunc = func;

    // run on loop manager
    for (var topLoop : this.currLoopInfo.getTopLevelLoops()) {
      LoopUtils.addLoopToQueue(topLoop, loopQueue);
    }
    while (!loopQueue.isEmpty()) {
      var loop = loopQueue.remove();
      runOnLoop(loop);
    }
  }

  // 循环特征：simpleForLoop，只修改全局数组，且修改的下标是 indVar 本身
  public void runOnLoop(Loop loop) {
    if (!loop.isSimpleForLoop()) {
      return;
    }

    // 如果有副作用 call，不并行
    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst instanceof CallInst) {
          return;
        }
      }
    }

    var preHeader = loop.getPreHeader();
    var header = loop.getLoopHeader();
    var latchBlock = loop.getSingleLatchBlock();
    if (latchBlock == null) {
      return;
    }
    var headerSuccIndex = latchBlock.getSuccessor_().indexOf(header);
    var exit = latchBlock.getSuccessor_().get(1 - headerSuccIndex);
    var indVar = loop.getIndVar();
    var indVarInit = loop.getIndVarInit();
    var indVarEnd = loop.getIndVarEnd();
    var latchCmpInst = loop.getLatchCmpInst();
    var indVarCondInst = loop.getIndVarCondInst();
    var stepInst = loop.getStepInst();
    if (indVar == null || indVarInit == null || indVarEnd == null || indVarCondInst == null
        || stepInst == null || indVarCondInst != stepInst || latchCmpInst == null) {
      return;
    }

    // 如果 exit 有 phi，不并行
    if (exit.getList().getEntry().getVal() instanceof Phi) {
      return;
    }

    // 只修改全局数组，且下标是迭代器
    GEPInst onlyPointer = null;
    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst instanceof StoreInst) {
          var pointer = ((StoreInst) inst).getPointer();
          if (!(pointer instanceof GEPInst)) {
            return;
          }

          var gepInst = (GEPInst) pointer;

          if (onlyPointer == null) {
            onlyPointer = gepInst;
          } else if (onlyPointer != gepInst) {
            return;
          }
        }
      }
    }

    if (onlyPointer == null) {
      return;
    }

    // 判断唯一的 store 地址是不是满足迭代器要求
    // 指向 global i32，退出
    if (onlyPointer.getAimTo() == null) {
      return;
    }
    if (onlyPointer.getAimTo() instanceof GlobalVariable) {
      GlobalVariable gv = (GlobalVariable) onlyPointer.getAimTo();
      if (((PointerType) gv.getType()).getContained() instanceof IntegerType) {
        return;
      }
    }

    Type pointToType;

    if (ArrayAliasAnalysis.getArrayValue(onlyPointer) instanceof GlobalVariable) {
      // 普通全局数组
      return;
//      GlobalVariable gv = (GlobalVariable) ArrayAliasAnalysis.getArrayValue(onlyPointer);
//      pointToType = ((PointerType) (gv.getType())).getContained();//ARRTY
//      if (pointToType instanceof ArrayType) {
//        pointToType = ((ArrayType) pointToType).getELeType();
//      }
    } else {
      // 传参进函数的全局数组
      AllocaInst alloca = (AllocaInst) ArrayAliasAnalysis.getArrayValue(onlyPointer);
      if (alloca.getAllocatedType().isPointerTy()) {
        PointerType allocatedType = (PointerType) alloca.getAllocatedType();
        if (allocatedType == null) {
          return;
        }
        pointToType = allocatedType.getContained();//ARRTY

        for (var instNode : currFunc.getList_().getEntry().getVal().getList()) {
          var inst = instNode.getVal();
          if (inst instanceof StoreInst && inst.getOperands().get(1) == alloca) {
            if (!(inst.getOperands().get(0) instanceof Arg)) {
              return;
            }
            if (!((Arg) (inst.getOperands().get(0))).isMustBeGlobal()) {
              return;
            }
          }
        }
      } else {
        return;
      }
    }

    // 指向一维数组
    if (pointToType.isIntegerTy() && onlyPointer.getOperands().get(1) != indVar) {
      return;
    } else if (pointToType.isArrayTy()) {
      return;
    }

    // 判断循环中其他此 store 地址数组的使用中，下标是不是和这个 store 相同
    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst instanceof LoadInst) {
          var pointer = ((LoadInst) inst).getPointer();
          if (!(pointer instanceof GEPInst)) {
            continue;
          }
          var gepInst = (GEPInst) pointer;
          if (ArrayAliasAnalysis.getArrayValue(gepInst) == ArrayAliasAnalysis
              .getArrayValue(onlyPointer)) {
            if (gepInst != onlyPointer) {
              return;
            }
          }
        }
      }
    }

    // mark parallel
    header.setParallelLoopHeader(true);

    // insert parallel start/end function
    BasicBlock parallelStartBB = factory.buildBasicBlock("", currFunc);
    BasicBlock parallelEndBB = factory.buildBasicBlock("", currFunc);
    currLoopInfo.addBBToLoop(parallelStartBB, loop.getParentLoop());
    currLoopInfo.addBBToLoop(parallelEndBB, loop.getParentLoop());

    Function parallelStartFunc = null;
    Function parallelEndFunc = null;
    for (var func : currFunc.getNode().getParent().getVal().__functions) {
      if (func.getVal().getName().equals("parallel_start")) {
        parallelStartFunc = func.getVal();
      }
      if (func.getVal().getName().equals("parallel_end")) {
        parallelEndFunc = func.getVal();
      }
    }
    assert parallelStartFunc != null && parallelEndFunc != null;

    var callParallelStart = factory
        .buildFuncCall(parallelStartFunc, new ArrayList<>(), parallelStartBB);
    ArrayList<Value> args = new ArrayList<>();
    args.add(callParallelStart);
    var multInst = factory
        .buildBinaryAfter(TAG_.Mul, callParallelStart, indVarEnd, callParallelStart);
    var divInst = factory.buildBinaryAfter(TAG_.Div, multInst,
        ConstantInt.newOne(factory.getI32Ty(), parallelFactor), multInst);
    var initIndex = indVar.getOperands().indexOf(indVarInit);
    indVar.CoReplaceOperandByIndex(initIndex, divInst);
    var addInst2 = factory
        .buildBinaryBefore(latchCmpInst, TAG_.Add, ConstantInt.newOne(factory.getI32Ty(), 1),
            callParallelStart);
    var multInst2 = factory.buildBinaryAfter(TAG_.Mul, addInst2, indVarEnd, addInst2);
    var divInst2 = factory.buildBinaryAfter(TAG_.Div, multInst2,
        ConstantInt.newOne(factory.getI32Ty(), parallelFactor), multInst2);
    latchCmpInst.COReplaceOperand(indVarEnd, divInst2);

    factory.buildFuncCall(parallelEndFunc, args, parallelEndBB);

    factory.buildBr(header, parallelStartBB);
    factory.buildBr(exit, parallelEndBB);
    preHeader.getList().getLast().getVal().COReplaceOperand(header, parallelStartBB);
    latchBlock.getList().getLast().getVal().COReplaceOperand(exit, parallelEndBB);

    parallelStartBB.getPredecessor_().add(preHeader);
    parallelStartBB.getSuccessor_().add(header);
    parallelEndBB.getPredecessor_().add(latchBlock);
    parallelEndBB.getSuccessor_().add(exit);
    preHeader.getSuccessor_().set(preHeader.getSuccessor_().indexOf(header), parallelStartBB);
    header.getPredecessor_().set(header.getPredecessor_().indexOf(preHeader), parallelStartBB);
    latchBlock.getSuccessor_().set(latchBlock.getSuccessor_().indexOf(exit), parallelEndBB);
    exit.getPredecessor_().set(exit.getPredecessor_().indexOf(latchBlock), parallelEndBB);
  }
}
