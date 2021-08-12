package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;
import util.UnrollUtils;

public class ConstantLoopUnroll implements IRPass {

  private static final int threshold = 800;
  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;

  @Override
  public String getName() {
    return "constantLoopUnroll";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : ConstantLoopUnroll");

    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        runOnFunction(func);
      }
    }
  }

  public void runOnFunction(Function func) {
    var branchOpt = new BranchOptimization();
    var gvngcm = new GVNGCM();
    var loopInfoFullAnalysis = new LoopInfoFullAnalysis();
    var lcssa = new LCSSA();

    boolean change = true;

    while (change) {
      change = false;

      loopInfoFullAnalysis.runOnFunction(func);
      lcssa.runOnFunction(func);
      this.currLoopInfo = func.getLoopInfo();
      Queue<Loop> loopQueue = new LinkedList<>();

      // run on loop manager
      for (var topLoop : this.currLoopInfo.getTopLevelLoops()) {
        UnrollUtils.addLoopToQueue(topLoop, loopQueue);
      }
      while (!loopQueue.isEmpty()) {
        var loop = loopQueue.remove();
        if (runOnLoop(loop)) {
          change = true;
          branchOpt.runBranchOptimization(func);
          gvngcm.runGVNGCM(func);
          break;
        }
      }
    }
  }

  public boolean runOnLoop(Loop loop) {
    if (loop.getTripCount() == null) {
      return false;
    }

    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst instanceof CallInst) {
          return false;
        }
      }
    }

    UnrollUtils.rearrangeBBOrder(loop);
    return constantUnroll(loop);
  }

  public boolean constantUnroll(Loop loop) {
    log.info("Run constant unroll");

    // 目前只对 simpleForLoop 做 constantUnroll
    if (!loop.isSimpleForLoop()) {
      return false;
    }

    int instNum = 0;
    var latchBr = loop.getSingleLatchBlock().getList().getLast().getVal();
    var tripCount = loop.getTripCount();

    ArrayList<BasicBlock> loopBlocks = new ArrayList<>(loop.getBlocks());

    BasicBlock header = loop.getLoopHeader();
    BasicBlock exit = null; // 循环结束跳到的基本块
    if (loopBlocks.contains((BasicBlock) (latchBr.getOperands().get(1)))) {
      exit = (BasicBlock) (latchBr.getOperands().get(2));
    } else {
      exit = (BasicBlock) (latchBr.getOperands().get(1));
    }

    for (var bb : loopBlocks) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (!(inst instanceof Phi && bb != header)) {
          // ignore header Phi
          instNum++;
        }
      }
    }
    if (tripCount == 1e9 + 7 || tripCount == 0) {
      // dead loop or unreachable loop
      return false;
    } else if (instNum * tripCount > threshold) {
      // doubleUnroll(loop);
      return false;
    }

    // start constant unroll
    HashMap<Value, Value> lastValueMap = new HashMap<>();
    ArrayList<Phi> originPhis = new ArrayList<>();
    var latchBlock = loop.getSingleLatchBlock();
    var latchPredIndex = header.getPredecessor_().indexOf(latchBlock);
    for (var instNode : header.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }

      Phi phi = (Phi) inst;
      originPhis.add(phi);
      var latchIncomingVal = phi.getIncomingVals().get(latchPredIndex);
      if (latchIncomingVal instanceof Instruction &&
          (loopBlocks.contains(((Instruction) latchIncomingVal).getBB()))) {
        lastValueMap.put(latchIncomingVal, latchIncomingVal);
      }
    }

    // remove latch br inst
    latchBr.CORemoveAllOperand();
    latchBlock.getSuccessor_().remove(header);
    latchBlock.getSuccessor_().remove(exit);
    latchBr.node.removeSelf();

    ArrayList<BasicBlock> headers = new ArrayList<>();
    ArrayList<BasicBlock> latches = new ArrayList<>();
    headers.add(header);
    latches.add(latchBlock);

    for (int iterNum = 1; iterNum < tripCount; iterNum++) {
      ArrayList<BasicBlock> newBlocks = new ArrayList<>();

      for (var bb : loopBlocks) {
        HashMap<Value, Value> valueMap = new HashMap<>();
        BasicBlock newBB = UnrollUtils.getLoopBasicBlockCopy(bb, valueMap);

        if (bb == header) {
          for (var phi : originPhis) {
            var newPhi = (Phi) valueMap.get(phi);
            var latchIncomingVal = newPhi.getIncomingVals().get(latchPredIndex);
            if (latchIncomingVal instanceof Instruction &&
                (loopBlocks.contains(((Instruction) latchIncomingVal).getBB())) && iterNum > 1) {
              latchIncomingVal = lastValueMap.get(latchIncomingVal);
            }
            valueMap.put(phi, latchIncomingVal);
            newPhi.CORemoveAllOperand();
            newPhi.node.removeSelf();
          }
        }

        lastValueMap.put(bb, newBB);
        for (var key : valueMap.keySet()) {
          lastValueMap.put(key, valueMap.get(key));
        }
        currLoopInfo.addBBToLoop(newBB, loop);

        // TODO 多个出口时在这里更新 LCSSA 生成的 phi

        if (bb == header) {
          headers.add(newBB);
        }
        if (bb == latchBlock) {
          latches.add(newBB);
        }
        newBlocks.add(newBB);
      }

      var newHeader = lastValueMap.get(header);
      for (var newBB : newBlocks) {
        newBB.getList().forEach(instNode -> {
          UnrollUtils.remapInstruction(instNode.getVal(), lastValueMap);
        });
        if (newBB != newHeader) {
          for (var key : lastValueMap.keySet()) {
            if (lastValueMap.get(key) == newBB) {
              BasicBlock oldBB = (BasicBlock) key;
              for (var pred : oldBB.getPredecessor_()) {
                assert loopBlocks.contains(pred);
                BasicBlock newPred = (BasicBlock) lastValueMap.get(pred);
                newPred.getSuccessor_().add(newBB);
                newBB.getPredecessor_().add(newPred);
              }
            }
          }
        }
      }
    }

    // latch block 跳到的 exit block 更新 LCSSA phi
    if (tripCount > 1) {
      for (var exitBB : loop.getExitBlocks()) {
        if (exitBB.getPredecessor_().contains(latchBlock)) {
          var latchIndex = exitBB.getPredecessor_().indexOf(latchBlock);
          for (var instNode : exitBB.getList()) {
            var inst = instNode.getVal();
            if (!(inst instanceof Phi)) {
              break;
            }

            var phi = (Phi) inst;
            var incomingVal = phi.getIncomingVals().get(latchIndex);
            if (incomingVal instanceof Instruction && (loop.getBlocks()
                .contains(((Instruction) incomingVal).getBB()))) {
              incomingVal = lastValueMap.get(incomingVal);
            }
            phi.CoReplaceOperandByIndex(latchIndex, incomingVal);
          }
        }
      }
    }

    // set original values of head phi
    var preheaderPredIndex = 1 - latchPredIndex;
    for (var phi : originPhis) {
      phi.COReplaceAllUseWith(phi.getIncomingVals().get(preheaderPredIndex));
      phi.CORemoveAllOperand();
      phi.node.removeSelf();
    }

    // link the new blocks
    for (int i = 1; i < latches.size(); i++) {
      var succ = headers.get(i);
      var pred = latches.get(i - 1);
      factory.buildBr(succ, pred);

      var latchIndex = succ.getPredecessor_().indexOf(latchBlock);
      if (latchIndex != -1) {
        succ.getPredecessor_().set(latchIndex, pred);
      } else {
        succ.getPredecessor_().add(pred);
      }
      var headIndex = pred.getSuccessor_().indexOf(succ);
      if (headIndex != -1) {
        pred.getSuccessor_().set(headIndex, succ);
      } else {
        pred.getSuccessor_().add(succ);
      }
    }
    var last = latches.get(latches.size() - 1);
    factory.buildBr(exit, last);
    var latchIndex = exit.getPredecessor_().indexOf(latchBlock);
    if (latchIndex != -1) {
      exit.getPredecessor_().set(latchIndex, last);
    } else {
      exit.getPredecessor_().add(last);
    }
    var headIndex = last.getSuccessor_().indexOf(exit);
    if (headIndex != -1) {
      last.getSuccessor_().set(headIndex, exit);
    } else {
      last.getSuccessor_().add(exit);
    }

    header.getPredecessor_().remove(latchBlock);
    currLoopInfo.removeLoop(loop);

    return true;
  }
}
