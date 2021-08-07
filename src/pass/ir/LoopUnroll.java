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
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

public class LoopUnroll implements IRPass {

  private static final int threshold = 800;
  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;
  private Function currFunction;

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
//      assert inst.tag != TAG_.Call;
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
        case Call -> {
          copy = factory.getFuncCall((Function) ops.get(0), new ArrayList<>() {{
            for (int i = 1; i < ops.size(); i++) {
              add(ops.get(i));
            }
          }});
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
      if (vMap.containsKey(op) && !op.isFunction()) {
        var newOp = vMap.get(op);
        inst.CoReplaceOperandByIndex(i, newOp);
      }
    }
  }

  public static void addLoopToQueue(Loop loop, Queue<Loop> queue) {
    for (var subLoop : loop.getSubLoops()) {
      if (subLoop != null) {
        addLoopToQueue(subLoop, queue);
      }
    }
    queue.add(loop);
  }

  public static void rearrangeBBOrder(Loop loop) {
    for (var bb : loop.getBlocks()) {
      bb.setDirty(false);
    }
    var header = loop.getLoopHeader();
    ArrayList<BasicBlock> tmp = new ArrayList<>();
    rearrangeDFS(header, loop, tmp);
    loop.getBlocks().clear();
    loop.getBlocks().addAll(tmp);
  }

  public static void rearrangeDFS(BasicBlock curr, Loop loop, ArrayList<BasicBlock> tmp) {
    if (curr.isDirty()) {
      return;
    }
    curr.setDirty(true);
    tmp.add(curr);
    for (var bb : curr.getSuccessor_()) {
      if (loop.getBlocks().contains(bb)) {
        rearrangeDFS(bb, loop, tmp);
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
    this.currFunction = func;

    // run on loop manager
    for (var topLoop : this.currLoopInfo.getTopLevelLoops()) {
      addLoopToQueue(topLoop, loopQueue);
    }
    while (!loopQueue.isEmpty()) {
      var loop = loopQueue.remove();
      runOnLoop(loop);
//      loopQueue.remove();
    }
  }

  public void runOnLoop(Loop loop) {
    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        if (inst instanceof CallInst) {
          return;
        }
      }
    }

    // stepInst 是 add (phi, constant) 才展开（sub 被转换成了 add 负常数）
    rearrangeBBOrder(loop);

    if (loop.getTripCount() != null) {
      constantUnroll(loop);
    } else {
//      doubleUnroll(loop);
    }
  }

  public void constantUnroll(Loop loop) {
    log.info("Run constant unroll");

    // 目前只对 simpleForLoop 做 constantUnroll
    if (!loop.isSimpleForLoop()) {
      doubleUnroll(loop);
    }

    int instNum = 0;
    var latchBr = loop.getSingleLatchBlock().getList().getLast().getVal();
    var tripCount = loop.getTripCount();

    ArrayList<BasicBlock> loopBlocks = new ArrayList<>(
        loop.getBlocks()); // we will update loop.getBlocks() later

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
      return;
    } else if (instNum * tripCount > threshold) {
//      doubleUnroll(loop);
      return;
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
          remapInstruction(instNode.getVal(), lastValueMap);
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
      var lastBB = (BasicBlock) lastValueMap.get(latchBlock);

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
  }

  public void doubleUnroll(Loop loop) {
    log.info("Run double unroll");

    // 目前只对 simpleForLoop 做 doubleUnroll
    if (!loop.isSimpleForLoop()) {
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

    // latchBr 跳转到的循环外基本块
    var latchBlock = loop.getSingleLatchBlock();
    var latchBr = latchBlock.getList().getLast().getVal();
    ArrayList<BasicBlock> loopBlocks = new ArrayList<>(loop.getBlocks());
    BasicBlock header = loop.getLoopHeader();
    BasicBlock exit = null;
    if (loopBlocks.contains((BasicBlock) (latchBr.getOperands().get(1)))) {
      exit = (BasicBlock) (latchBr.getOperands().get(2));
    } else {
      exit = (BasicBlock) (latchBr.getOperands().get(1));
    }

    // start double unroll

  }
}
