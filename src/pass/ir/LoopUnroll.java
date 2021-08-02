package pass.ir;

import ir.Loop;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst.CallInst;
import pass.Pass.IRPass;

import java.util.ArrayList;
import java.util.HashMap;

public class LoopUnroll implements IRPass {

  private static final int threshold = 2000;

  @Override
  public String getName() {
    return "loopUnroll";
  }

  @Override
  public void run(MyModule m) {
    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        runOnFunction(func);
      }
    }
  }

  public void runOnFunction(Function func) {

  }

  public void runOnLoop(Loop loop) {
    var header = loop.getLoopHeader();
    var latchBlock = loop.getLatchBlock();
    var stepInst = loop.getStepInst();

    // stepInst 是 add (phi, constant) 才展开（sub 被转换成了 add 负常数）
    if (!loop.isCanonical()) {
      return;
    }

    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst instanceof CallInst) {
          return;
        }
      }
    }

    if (loop.getTripCount() != null) {
      constantUnroll(loop);
    } else {
      doubleUnroll(loop);
    }
  }

  public void constantUnroll(Loop loop) {
    int instNum = 0;
    var header = loop.getLoopHeader();
    var tripCount = loop.getTripCount();
    var loopBlocks = loop.getBlocks();

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
      return;
    } else if (instNum * tripCount > 2000) {
      doubleUnroll(loop);
      return;
    }

    HashMap<Value, Value> lastValueMap = new HashMap<>();
    ArrayList<Phi> originPhis = new ArrayList<>();
    var latchBlock = loop.getLatchBlock();
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
    latchBlock.getList().getLast().removeSelf();

    ArrayList<BasicBlock> headers = new ArrayList<>();
    ArrayList<BasicBlock> latches = new ArrayList<>();
    headers.add(header);
    latches.add(latchBlock);

    for (int iterNum = 1; iterNum < tripCount; iterNum++) {
      ArrayList<BasicBlock> newBlocks = new ArrayList<>();

      for (var bb : loopBlocks) {
        HashMap<Value, Value> valueMap = new HashMap<>();
        // TODO: ValueCloner CloneBB(bb, valueMap, suffixBuffer)

        if (bb == header) {
          for (var phi : originPhis) {
            var newPhi = (Phi) valueMap.get(phi);
            Value latchIncomingVal = newPhi.getIncomingVals().get(latchPredIndex);
            if (latchIncomingVal instanceof Instruction &&
                (loopBlocks.contains(((Instruction) latchIncomingVal).getBB())) && iterNum > 1) {
              valueMap.replace(phi, latchIncomingVal);
              // TODO new block erase newPhi
            }
          }
        }
      }
    }


  }

  public void doubleUnroll(Loop loop) {

  }
}
