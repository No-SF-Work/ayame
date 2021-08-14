package pass.ir;

import ir.Analysis.ArrayAliasAnalysis;
import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyModule;
import ir.types.ArrayType;
import ir.values.Function;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
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
    // TODO 判断 gepInst 指向的是否是全局数组
    var bias = onlyPointer.getOperands().get(1);

    // 指向 i32，退出
    var pointToType = onlyPointer.getElementType_();
    if (pointToType.isIntegerTy()) {
      return;
    }

    var arrType = pointToType;
    int dimNum = 0;
    while (arrType.isArrayTy()) {
      arrType = ((ArrayType) arrType).getELeType();
      dimNum++;
    }
    if (dimNum == 0) {
      return;
    }
    // 指向一维数组
    if (dimNum == 1 && bias != indVar) {
      return;
    }
    // 指向多维数组
    else if (dimNum != 1) {
      // %bias = add %indvar, %mul
      // %mul -> out of loop OR not an instruction
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
          if (ArrayAliasAnalysis.getArrayValue(gepInst) == ArrayAliasAnalysis.getArrayValue(onlyPointer)) {
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
