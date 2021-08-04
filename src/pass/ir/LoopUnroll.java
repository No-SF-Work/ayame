package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst;
import ir.values.instructions.TerminatorInst.CallInst;
import pass.Pass.IRPass;
import util.Mylogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class LoopUnroll implements IRPass {

  private static final int threshold = 2000;
  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;

  // return a new instruction but uses the old inst operands
  public static Instruction copyInstruction(Instruction inst) {
    Instruction copy = null;
    var ops = inst.getOperands();
    if (inst instanceof BinaryInst) {
      copy = factory.getBinary(inst.tag, ops.get(0), ops.get(1));
    } else if (inst instanceof MemInst) {
      copy = switch (inst.tag) {
        case Alloca -> factory.getAlloca(((AllocaInst) inst).getAllocatedType());
        case Load -> factory.getLoad(inst.getType(), ops.get(0));
        case Store -> factory.getStore(ops.get(0), ops.get(1));
        case GEP -> factory.getGEP(ops.get(0),
            new ArrayList<>() {{
              for (int i = 1; i < ops.size(); i++) {
                add(ops.get(i));
              }
            }});
        case Zext -> factory.getZext(ops.get(0));
        case Phi -> new Phi(inst.tag, inst.getType(), inst.getNumOP(), inst.getOperands());
        default -> throw new RuntimeException();
      };
    } else if (inst instanceof TerminatorInst) {
      assert inst.tag != TAG_.Call;
      switch (inst.tag) {
        case Br -> {
          if (ops.size() == 3) {
            copy = factory.getBr(ops.get(0), (BasicBlock) ops.get(1), (BasicBlock) ops.get(2));
          }
          if (ops.size() == 1) {
            copy = factory.getBr((BasicBlock) ops.get(0));
          }
        }

        case Ret -> {
          if (ops.size() == 1) {
            copy = factory.getRet(ops.get(0));
          } else {
            copy = factory.getRet();
          }
        }
      }
    }

    if (copy == null) {
      throw new RuntimeException();
    }
    return copy;
  }

  public static BasicBlock getLoopBasicBlockCopy(BasicBlock source, HashMap<Value, Value> vMap) {
    var func = source.getParent();
    var target = factory.buildBasicBlock("", func);
    source.getList().forEach(instNode -> {
      var inst = instNode.getVal();
      var copyInst = copyInstruction(inst);
      vMap.put(inst, copyInst);
      copyInst.node.insertAtEnd(target.getList());
    });

    return target;
  }

  public static void remapInstruction(Instruction inst, HashMap<Value, Value> vMap) {
    for (int i = 0; i < inst.getOperands().size(); i++) {
      var op = inst.getOperands().get(i);
      if (vMap.containsKey(op)) {
        var newOp = vMap.get(op);
        inst.CoReplaceOperandByIndex(i, newOp);
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
      for (var bb : loopBlocks) {
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
      for (var bb : loopBlocks) {
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

    // run on loop manager
    for (var topLoop : this.currLoopInfo.getTopLevelLoops()) {
      addLoopToQueue(topLoop, loopQueue);
    }
    while (!loopQueue.isEmpty()) {
      var loop = loopQueue.remove();
      runOnLoop(loop);
    }
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
    var latchBr = loop.getLatchBlock().getList().getLast()
        .getVal(); // FIXME: maybe exit block is better
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
            var latchIncomingVal = newPhi.getIncomingVals().get(latchPredIndex);
            if (latchIncomingVal instanceof Instruction &&
                (loopBlocks.contains(((Instruction) latchIncomingVal).getBB())) && iterNum > 1) {
              latchIncomingVal = lastValueMap.get(latchIncomingVal);
            }
            valueMap.put(phi, latchIncomingVal);
            newPhi.node.removeSelf();
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
      phi.CORemoveAllOperand();
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
