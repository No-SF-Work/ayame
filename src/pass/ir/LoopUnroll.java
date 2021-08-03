package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Value;
import ir.values.ValueCloner;
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

public class LoopUnroll implements IRPass {

  private static final int threshold = 2000;
  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;

  public static BasicBlock getLoopBasicBlockCopy(BasicBlock source, HashMap<Value, Value> vMap) {
    var func = source.getParent();
    var target = factory.buildBasicBlock("", func);
    var cloner = new ValueCloner() {
      @Override
      public Value findValue(Value value) {
        if (value instanceof Constant) {
          return value;
        } else {
          return this.valueMap.get(value);
        }
      }
    };

    source.getList().forEach(instNode -> {
      var inst = instNode.getVal();
      for (Value operand : inst.getOperands()) {
        if (cloner.findValue(operand) == null) {
          cloner.put(operand, operand);
        }
      }
      var cloneInst = cloner.getInstCopy(inst);
      cloner.put(inst, cloneInst);
      vMap.put(inst, cloneInst);
      cloneInst.node.insertAtEnd(target.getList());
    });

    return target;
  }

  public static void remapInstruction(Instruction inst, HashMap<Value, Value> vMap) {
    for (int i = 0; i < inst.getOperands().size(); i++) {
      var op = inst.getOperands().get(i);
      if (vMap.containsKey(op)) {
        var newOp = vMap.get(op);
        inst.CoSetOperand(i, newOp);
      }
    }
  }

  public static void addLoopToQueue(Loop loop, Queue<Loop> queue) {
    queue.add(loop);
    for (var subLoop : loop.getSubLoops()) {
      if (subLoop != null) {
        queue.add(subLoop);
      }
    }
  }

  public static void removeLoop(Loop loop, LoopInfo loopInfo) {
    ArrayList<BasicBlock> loopBlocks = new ArrayList<>();
    loopBlocks.addAll(loop.getBlocks());
    if (loop.getParentLoop() != null) {
      var parentLoop = loop.getParentLoop();
      for (var bb: loopBlocks) {
        if (loopInfo.getLoopForBB(bb) == loop) {
          loopInfo.getBbLoopMap().put(bb, parentLoop);
        }
      }

      parentLoop.removeSubLoop(loop);

      while (loop.getSubLoops().size() != 0) {
        var subLoop = loop.getSubLoops().get(0);
        loop.removeSubLoop(subLoop);
        parentLoop.addSubLoop(subLoop);
      }
    } else {
      for (var bb: loopBlocks) {
        if (loopInfo.getLoopForBB(bb) == loop) {
          // bb 在最外层循环里了
          loopInfo.removeBBFromAllLoops(bb);
        }
      }

      loopInfo.removeTopLevelLoop(loop);
      while (loop.getSubLoops().size() != 0) {
        var subLoop = loop.getSubLoops().get(0);
        loop.removeSubLoop(subLoop);
        loopInfo.addTopLevelLoop(subLoop);
      }
    }
  }

  @Override
  public String getName() {
    return "loopUnroll";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : LoopUnroll");

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
    for (var topLoop: this.currLoopInfo.getTopLevelLoops()) {
      addLoopToQueue(topLoop, loopQueue);
    }
    while (!loopQueue.isEmpty()) {
      var loop = loopQueue.remove();
      runOnLoop(loop);
    }

    // TODO run on loop manager
  }

  public void runOnLoop(Loop loop) {
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
    log.info("Run constant unroll");

    int instNum = 0;
    var latchBr = loop.getLatchBlock().getList().getLast().getVal(); // FIXME: maybe exit block is better
    var tripCount = loop.getTripCount();

    ArrayList<BasicBlock> loopBlocks = new ArrayList<>();
    loopBlocks.addAll(loop.getBlocks()); // we will update loop.getBlocks() later

    BasicBlock header = loop.getLoopHeader();
    BasicBlock exit = null; // 循环结束跳到的基本块
    if (loopBlocks.contains(latchBr.getOperands().get(1))) {
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
      return;
    } else if (instNum * tripCount > threshold) {
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
    latchBr.node.removeSelf();

    ArrayList<BasicBlock> headers = new ArrayList<>();
    ArrayList<BasicBlock> latches = new ArrayList<>();
    headers.add(header);
    latches.add(latchBlock);

    for (int iterNum = 1; iterNum < tripCount; iterNum++) {
      ArrayList<BasicBlock> newBlocks = new ArrayList<>();

      for (var bb : loopBlocks) {
        HashMap<Value, Value> valueMap = new HashMap<>();
        BasicBlock newBB = getLoopBasicBlockCopy(bb, valueMap);

        if (bb == header) {
          for (var phi : originPhis) {
            var newPhi = (Phi) valueMap.get(phi);
            Value latchIncomingVal = newPhi.getIncomingVals().get(latchPredIndex);
            if (latchIncomingVal instanceof Instruction &&
                (loopBlocks.contains(((Instruction) latchIncomingVal).getBB())) && iterNum > 1) {
              valueMap.replace(phi, latchIncomingVal);
              newPhi.node.removeSelf();
            }
          }
        }

        lastValueMap.put(bb, newBB);
        for (var key : valueMap.keySet()) {
          lastValueMap.put(key, valueMap.get(key));
        }
        currLoopInfo.addBBToLoop(newBB, loop);

        // 暂时不考虑多个 exitingBlocks 时的循环外 phi 更新

        if (bb == header) {
          headers.add(newBB);
        }
        if (bb == latchBlock) {
          latches.add(newBB);
        }
        newBlocks.add(newBB);
      }

      for (var newBB : newBlocks) {
        newBB.getList().forEach(instNode -> {
          remapInstruction(instNode.getVal(), lastValueMap);
        });
      }
    }

    // latch block 跳到的基本块更新 phi
    if (tripCount > 1) {

    }

    // set original values of head phi
    var preheaderPredIndex = 1 - latchPredIndex;
    for (var phi : originPhis) {
      phi.COReplaceAllUseWith(phi.getIncomingVals().get(preheaderPredIndex));
      phi.node.removeSelf();
    }

    // link the new blocks
    for (int i = 1; i < latches.size(); i++) {
      factory.buildBr(headers.get(i), latches.get(i - 1));
    }
    factory.buildBr(exit, latches.get(latches.size() - 1));

    removeLoop(loop, currLoopInfo);
  }

  public void doubleUnroll(Loop loop) {
    log.info("Run double unroll");
  }
}
