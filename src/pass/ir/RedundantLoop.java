package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;
import util.UnrollUtils;

public class RedundantLoop implements IRPass {

  private static Logger log = Mylogger.getLogger(IRPass.class);
  private LoopInfo currLoopInfo;

  @Override
  public String getName() {
    return "redundantLoop";
  }

  @Override
  public void run(MyModule m) {
    log.info("running redundantLoop");

    for (var funcNode : m.__functions) {
      if (!funcNode.getVal().isBuiltin_()) {
        runOnFunction(funcNode.getVal());
      }
    }
  }

  public void runOnFunction(Function func) {
    var loopInfoFullAnalysis = new LoopInfoFullAnalysis();
    loopInfoFullAnalysis.runOnFunction(func);

    currLoopInfo = func.getLoopInfo();

    Queue<Loop> loopQueue = new LinkedList<>();
    var loopInfo = func.getLoopInfo();

    // run on loop manager
    for (var topLoop : loopInfo.getTopLevelLoops()) {
      UnrollUtils.addLoopToQueue(topLoop, loopQueue);
    }
    while (!loopQueue.isEmpty()) {
      var loop = loopQueue.remove();
      removeUselessLoop(loop);
    }
  }

  public void removeUselessLoop(Loop loop) {
    // 循环结构限制：一个 preHeader，一个 latchBlock，一个 exit，可能有多个 exitingBlock
    var preHeader = loop.getPreHeader();
    var latchBlock = loop.getSingleLatchBlock();
    if (preHeader == null || latchBlock == null || loop.getExitBlocks().size() > 1) {
      return;
    }

    var latchBr = latchBlock.getList().getLast().getVal();
    if (latchBr.getOperands().size() == 1) {
      return;
    }
    BasicBlock exit = null;
    if (loop.getBlocks().contains((BasicBlock) (latchBr.getOperands().get(1)))) {
      exit = (BasicBlock) (latchBr.getOperands().get(2));
    } else {
      exit = (BasicBlock) (latchBr.getOperands().get(1));
    }
    if (exit == null) {
      return;
    }

    HashSet<Instruction> loopInsts = new HashSet<>();
    // 循环内指令限制：不能有 Store/Ret/side effect Call
    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst.tag == TAG_.Store || inst.tag == TAG_.Ret
            || (inst.tag == TAG_.Call && ((CallInst) inst).getFunc().isHasSideEffect())) {
          return;
        }
        loopInsts.add(inst);
      }
    }

    // 出口限制：循环内指令不被出口处 LCSSA Phi 使用
    for (var instNode : exit.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }

      for (var op : inst.getOperands()) {
        if (loopInsts.contains(op)) {
          return;
        }
      }
    }

    // 开始消除无用循环
    preHeader.getSuccessor_().remove(loop.getLoopHeader());
    var preBrInst = preHeader.getList().getLast().getVal();
    assert preBrInst.getOperands().size() == 3;
    int headerOpIndex = preBrInst.getOperands().indexOf(loop.getHeader());
    preBrInst.CORemoveNOperand(new int[]{0, headerOpIndex});

    HashSet<BasicBlock> predSet = new HashSet<>();
    for (var pred : exit.getPredecessor_()) {
      if (loop.getBlocks().contains(pred)) {
        predSet.add(pred);
      }
    }
    for (var pred : predSet) {
      exit.getPredecessor_().remove(pred);
    }

    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        inst.CORemoveAllOperand();
      }
      bb.node_.removeSelf();
    }

    currLoopInfo.removeLoop(loop);
  }
}
