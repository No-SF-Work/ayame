package pass.ir;

import ir.Analysis.ArrayAliasAnalysis;
import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyModule;
import ir.types.PointerType;
import ir.types.Type;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.Function.Arg;
import ir.values.GlobalVariable;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.LoopUtils;
import util.Mylogger;

// 识别到可并行循环后，在循环头基本块处标记
public class MarkParallel implements IRPass {

  private static Logger log = Mylogger.getLogger(IRPass.class);
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

    // 如果有 call，不并行
    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst instanceof CallInst) {
          return;
        }
      }
    }

    var header = loop.getLoopHeader();
    var latchBlock = loop.getSingleLatchBlock();
    if (latchBlock == null) {
      return;
    }
    var headerSuccIndex = latchBlock.getSuccessor_().indexOf(header);
    var exit = latchBlock.getSuccessor_().get(1 - headerSuccIndex);
    var indVar = loop.getIndVar();

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
    if (onlyPointer.getAimTo() instanceof GlobalVariable) {
      GlobalVariable gv = (GlobalVariable) onlyPointer.getAimTo();
      if (gv.init instanceof ConstantInt) {
        return;
      }
    }

    Type pointToType;

    if (ArrayAliasAnalysis.getArrayValue(onlyPointer) instanceof GlobalVariable) {
      // 普通全局数组
      GlobalVariable gv = (GlobalVariable) ArrayAliasAnalysis.getArrayValue(onlyPointer);
      pointToType = ((PointerType) (gv.getType())).getContained();
    } else {
      // 传参进函数的全局数组
      AllocaInst alloca = (AllocaInst) ArrayAliasAnalysis.getArrayValue(onlyPointer);
      if (alloca.getAllocatedType().isPointerTy()) {
        PointerType allocatedType = (PointerType) alloca.getAllocatedType();
        pointToType = allocatedType.getContained();

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
    }
    // 指向多维数组
    else if (pointToType.isArrayTy()) {
      // %bias = add %indvar, %mul
      // %mul -> out of loop OR not an instruction
      var bias = onlyPointer.getOperands().get(2);
      if (!(bias instanceof BinaryInst) || ((BinaryInst) bias).tag != TAG_.Add) {
        return;
      }
      var biasInst = (BinaryInst) bias;
      var indVarOpIndex = biasInst.getOperands().indexOf(indVar);
      if (indVarOpIndex == -1) {
        return;
      }

      var mulInst = biasInst.getOperands().get(1 - indVarOpIndex);
      if (mulInst instanceof Instruction && ((Instruction) mulInst).getBB().getLoopDepth() == loop
          .getLoopDepth()) {
        return;
      }
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

    header.setParallelLoopHeader(true);
  }
}
