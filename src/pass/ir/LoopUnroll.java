package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst.CallInst;
import pass.Pass.IRPass;
import util.Mylogger;
import util.LoopUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class LoopUnroll implements IRPass {

  private static final int maxBBinLoop = 5;
  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;
  private Function currFunction;

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
      LoopUtils.addLoopToQueue(topLoop, loopQueue);
    }
    while (!loopQueue.isEmpty()) {
      var loop = loopQueue.remove();
      runOnLoop(loop);
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

    if (loop.getLoopHeader().isParallelLoopHeader()) {
      return;
    }

    // stepInst 是 add (phi, constant) 才展开（sub 被转换成了 add 负常数）
    LoopUtils.rearrangeBBOrder(loop);

//    if (loop.getTripCount() != null) {
//      return;
//    } else {
//      doubleUnroll(loop);
//    }
    doubleUnroll(loop);
  }

  public void doubleUnroll(Loop loop) {
    log.info("Run double unroll");

    // 可以循环展开的情况：是 simpleForLoop，满足 canDoubleUnroll 的情况（可以拿到 indVar，stepInst 是 Add/Sub），循环体基本块不超过 5 个
    if (loop.getBlocks().size() > maxBBinLoop) {
      return;
    }

    // 目前只对 simpleForLoop 做 doubleUnroll
    if (!loop.isSimpleForLoop() || !canDoubleUnroll(loop)) {
      return;
    }

    // 目前只考虑 latchBlock 和 exitingBlock 只有一个且相同的情况
    if (loop.getExitingBlocks().size() != 1 || loop.getLatchBlocks().size() != 1) {
      return;
    }

    // step too big
    if (loop.getStep() instanceof ConstantInt) {
      var s = ((ConstantInt) loop.getStep()).getVal();
      if (Math.abs(s) > 100000000) {
        return;
      }
    }

    BasicBlock header = loop.getLoopHeader();

    // check gep (base, load)
    for (var instNode : header.getList()) {
      if (instNode.getVal() instanceof MemInst.GEPInst) {
        var gepInst = instNode.getVal();
        for (int i = 1; i < gepInst.getNumOP(); i++) {
          if (gepInst.getOperands().get(i) instanceof MemInst.LoadInst) {
            return;
          }
        }
      }
    }

    var latchBlock = loop.getSingleLatchBlock();
    var latchBr = latchBlock.getList().getLast().getVal();
    var latchCmpInst = loop.getLatchCmpInst();
    var latchPredIndex = header.getPredecessor_().indexOf(latchBlock);
    var stepInst = loop.getStepInst();
    ArrayList<BasicBlock> loopBlocks = new ArrayList<>(loop.getBlocks());
    BasicBlock exit = null;
    if (loopBlocks.contains((BasicBlock) (latchBr.getOperands().get(1)))) {
      exit = (BasicBlock) (latchBr.getOperands().get(2));
    } else {
      exit = (BasicBlock) (latchBr.getOperands().get(1));
    }

    HashMap<Value, Value> lastValueMap = new HashMap<>();
    ArrayList<Phi> originPhis = new ArrayList<>();

    // 准备工作：获取原循环头的 phi
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

    // 清除 latchBlock 的前驱后继信息
    var latchBrHeaderIndex = latchBr.getOperands().indexOf(header);
    latchBr.CORemoveAllOperand();
    latchBlock.getSuccessor_().remove(header);
    latchBlock.getSuccessor_().remove(exit);
    latchBr.node.removeSelf();

    BasicBlock secondHeader = null, secondLatch = null;

    // 构建增量循环体 start
    ArrayList<BasicBlock> newBlocks = new ArrayList<>();

    // 构建基本块，复制指令，删去 phi，更新 map，基本块加入循环
    for (var bb : loopBlocks) {
      HashMap<Value, Value> valueMap = new HashMap<>();
      BasicBlock newBB = LoopUtils.getLoopBasicBlockCopy(bb, valueMap);

      // 循环头中的 phi 直接用 incomingVal 替换
      if (bb == header) {
        for (var phi : originPhis) {
          var newPhi = (Phi) valueMap.get(phi);
          var latchIncomingVal = newPhi.getIncomingVals().get(latchPredIndex);
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
        secondHeader = newBB;
      }
      if (bb == latchBlock) {
        secondLatch = newBB;
      }
      newBlocks.add(newBB);
    }
    assert secondHeader != null;
    assert secondLatch != null;

    // 更新新基本块中的指令操作数，顺便维护新基本块之间的前驱后继关系
    for (var newBB : newBlocks) {
      newBB.getList().forEach(instNode -> {
        LoopUtils.remapInstruction(instNode.getVal(), lastValueMap);
      });
      if (newBB != secondHeader) {
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

    // 在新 latchBlock 的末尾插入新的 icmp
    // while (i < n) { i = i + 1; } => while (i + 1 < n) { i = i + 1; i = i + 1; }
    BinaryInst secondIndVarCondInst = (BinaryInst) lastValueMap.get(loop.getIndVarCondInst());
    BinaryInst secondStepInst = (BinaryInst) lastValueMap.get(stepInst);
    int stepIndex = secondStepInst.getOperands().indexOf(loop.getStep());
    assert stepIndex != -1;
    Value lhs, rhs;
    lhs = stepIndex == 0 ? loop.getStep() : secondIndVarCondInst;
    rhs = stepIndex == 0 ? secondIndVarCondInst : loop.getStep();
    var secondStepIndVarCondInst = factory
        .buildBinary(secondIndVarCondInst.tag, lhs, rhs, secondLatch);

    int indVarEndIndex = latchCmpInst.getOperands().indexOf(loop.getIndVarEnd());
    assert indVarEndIndex != -1;
    lhs = indVarEndIndex == 0 ? loop.getIndVarEnd() : secondStepIndVarCondInst;
    rhs = indVarEndIndex == 0 ? secondStepIndVarCondInst : loop.getIndVarEnd();
    var newIcmpInst = factory.buildBinary(latchCmpInst.tag, lhs, rhs, secondLatch);
    // 构建增量循环体 end

    // 保存 lastValueMap
    HashMap<Value, Value> iterValueMap = new HashMap<>(lastValueMap);

    // 保存 exit 中 phi 对应来自 latchBlock 的前驱
    // 后面设置 restBBLast 的 incomingVals 时使用这里缓存的原 incomingVals 作为查找 lastValueMap 的索引
    var exitLatchPredIndex = exit.getPredecessor_().indexOf(latchBlock);
    ArrayList<Value> cachedExitIncoming = new ArrayList<>();
    for (var instNode : exit.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }
      cachedExitIncoming.add(inst.getOperands().get(exitLatchPredIndex));
    }
    // updatePhiNewIncoming(exit, exitLatchPredIndex, loop, lastValueMap);

    // preHeader 维护跳转：跳转条件中迭代器加 1；对后继的维护在后面做
    // canonical loop 的 header 只有两个前驱
    var preHeader = header.getPredecessor_().get(1 - latchPredIndex);

    // 修改 preHeader 进入循环的判断条件
    // while (i < n) => while (i + 1 < n)
    // preHeader 中的 icmp 的 i 设为 i + 1
    assert preHeader.getList().getLast().getVal().getOperands().size() == 3;
    var preBrInst = preHeader.getList().getLast().getVal();
    var preIcmpInst = (Instruction) (preBrInst.getOperands().get(0));
    int preIndVarEndIndex = 0;
    var indVarEnd = loop.getIndVarEnd();
    for (var op : preIcmpInst.getOperands()) {
      if (op == loop.getIndVarEnd() || (op instanceof ConstantInt
          && indVarEnd instanceof ConstantInt
          && ((ConstantInt) op).getVal() == ((ConstantInt) indVarEnd).getVal())) {
        break;
      }
      preIndVarEndIndex++;
    }
    var preStepIndex = 1 - preIndVarEndIndex;
    var preStepInst = preIcmpInst.getOperands().get(preStepIndex);
    lhs = stepIndex == 0 ? loop.getStep() : preStepInst;
    rhs = stepIndex == 0 ? preStepInst : loop.getStep();
    var preStepIterOnce = factory.buildBinaryBefore(preIcmpInst, stepInst.tag, lhs, rhs);
    lhs = preIndVarEndIndex == 0 ? loop.getIndVarEnd() : preStepIterOnce;
    rhs = preIndVarEndIndex == 0 ? preStepIterOnce : loop.getIndVarEnd();
    var newPreIcmpInst = factory.buildBinaryBefore(preBrInst, preIcmpInst.tag, lhs, rhs);
    preBrInst.COReplaceOperand(preIcmpInst, newPreIcmpInst);
//    preIcmpInst.CoReplaceOperandByIndex(preStepIndex, preStepIterOnce);

    // 多出一次的迭代的收尾工作
    // original code : secondLatch -> exit
    // unroll code : secondLatch/preHeader -> exitIfBB, exitIfBB -> restBBHeader/exit, restBBLast -> exit
    // WARNING: this makes exitIfBB have a pred(preHeader) out of the loop

    // 构建 exitIfBB
    // 循环结束或在 preHeader 不进入循环时跳转到 exitIfBB，判断是否执行剩余一次迭代的计算，跳转到剩余一次迭代的基本块或原 exit
    var exitIfBB = factory.buildBasicBlock("", currFunction);
    currLoopInfo.addBBToLoop(exitIfBB, loop.getParentLoop());
    // exitIfBB 中的指令：多个 Phi 承接来自循环或 preHeader 的 incomingVals，一条 Phi 表示迭代器，一个 icmp 指令，一个 Br，跳转到 exit 或 restBBHeader
    for (var instNode : header.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      } else if (inst == loop.getIndVar()) {
        continue;
      }

      var copy = LoopUtils.copyInstruction(inst);
      copy.node.insertAtEnd(exitIfBB.getList());
      LoopUtils.remapInstruction(copy, lastValueMap);
      lastValueMap.put(iterValueMap.get(inst), copy);
    }
    var copyPhi = LoopUtils.copyInstruction(loop.getIndVar());
    var copyIcmp = LoopUtils.copyInstruction(latchCmpInst);
    copyPhi.node.insertAtEnd(exitIfBB.getList());
    LoopUtils.remapInstruction(copyPhi, lastValueMap);

    var copyIndVarCondInst = LoopUtils.copyInstruction(loop.getIndVar());
    LoopUtils.remapInstruction(copyIndVarCondInst, lastValueMap);
    copyIndVarCondInst
        .CoReplaceOperandByIndex(latchPredIndex, lastValueMap.get(loop.getIndVarCondInst()));
    copyIndVarCondInst.node.insertAtEnd(exitIfBB.getList());
    copyIcmp.COReplaceOperand(loop.getIndVarCondInst(), copyIndVarCondInst);
    copyIcmp.node.insertAtEnd(exitIfBB.getList());

    lastValueMap.put(stepInst, copyPhi);
    lastValueMap.put(latchCmpInst, copyIcmp);

    exit.getPredecessor_().set(exit.getPredecessor_().indexOf(latchBlock), exitIfBB);
    var exitExitIfBBIndex = exit.getPredecessor_().indexOf(exitIfBB);
    updatePhiNewIncoming(exit, exitExitIfBBIndex, loop, lastValueMap);

    // 复制多余的一次迭代
    ArrayList<BasicBlock> restBBs = new ArrayList<>();
    BasicBlock restBBHeader = null, restBBLast = null;
    for (var bb : loopBlocks) {
      HashMap<Value, Value> valueMap = new HashMap<>();
      BasicBlock newBB = LoopUtils.getLoopBasicBlockCopy(bb, valueMap);
      if (bb == header) {
        for (var phi : originPhis) {
          var newPhi = (Phi) valueMap.get(phi);
          var latchIncomingVal = newPhi.getIncomingVals().get(latchPredIndex);
          if (latchIncomingVal instanceof Instruction &&
              (loopBlocks.contains(((Instruction) latchIncomingVal).getBB()))) {
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
      // TODO 多个出口时在这里更新 LCSSA 生成的 phi
      if (bb == header) {
        restBBHeader = newBB;
      }
      if (bb == latchBlock) {
        restBBLast = newBB;
      }
      currLoopInfo.addBBToLoop(newBB, loop.getParentLoop());
      restBBs.add(newBB);
    }
    assert restBBHeader != null && restBBLast != null;
    for (var newBB : restBBs) {
      newBB.getList().forEach(instNode -> {
        LoopUtils.remapInstruction(instNode.getVal(), lastValueMap);
      });
      if (newBB != restBBHeader) {
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

    // 在复制完成多余的一次迭代后，header 才能更新 phi
    updatePhiNewIncoming(header, latchPredIndex, loop, iterValueMap);

    // restBBs 连接到循环中
    factory.buildBr(copyIcmp, restBBHeader, exit, exitIfBB);
    factory.buildBr(exit, restBBLast);
    restBBHeader.getPredecessor_().add(exitIfBB);
    restBBLast.getSuccessor_().add(exit);

    // exitIfBB 维护 successor
    exitIfBB.getSuccessor_().add(restBBHeader);
    exitIfBB.getSuccessor_().add(exit);

    // 假设 preHeader 只有 header 和 exit 两个后继，修改 exit 为 exitIfBB
    // exit 的前驱中 latchBlock 的位置替换成 exitIfBB
    preHeader.getList().getLast().getVal().COReplaceOperand(exit, exitIfBB);
    preHeader.getSuccessor_().set(preHeader.getSuccessor_().indexOf(exit), exitIfBB);
    secondLatch.getSuccessor_().add(exitIfBB);
    int preHeaderIndex = header.getPredecessor_().indexOf(preHeader);
    switch (preHeaderIndex) {
      case 0 -> {
        exitIfBB.getPredecessor_().add(preHeader);
        exitIfBB.getPredecessor_().add(secondLatch);
      }
      case 1 -> {
        exitIfBB.getPredecessor_().add(secondLatch);
        exitIfBB.getPredecessor_().add(preHeader);
      }
    }

    // exit 的 predecessor 中，latchblock 的位置换成 exitIfBB（前面构造 exitIfBB 做了），加入 restBBLast 前驱，删去 preHeader
    // 维护 phi：exitIfBB 来源的 incomingVals 替换 latchBlock (前面构造 exitIfBB 时做的，防止 lastValueMap 被更新) ，restBBLast 来源的 incomingVals 通过 lastValueMap 查询 cachedExitIncoming，preHeader 来源的 incomingVals 删去
    var exitPreHeaderIndex = exit.getPredecessor_().indexOf(preHeader);
    copyIndVarCondInst.CoReplaceOperandByIndex(exitPreHeaderIndex, preStepInst);
    exit.getPredecessor_().set(exitPreHeaderIndex, restBBLast);

    int cacheIndex = 0;
    int[] exitPreHeaderIndexArr = new int[]{exitPreHeaderIndex};
    for (var instNode : exit.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }

      var phi = (Phi) inst;
      var incomingVal = cachedExitIncoming.get(cacheIndex);
//      phi.CORemoveNOperand(exitPreHeaderIndexArr);
      if (incomingVal instanceof Instruction &&
          (loopBlocks.contains(((Instruction) incomingVal).getBB()))) {
        incomingVal = lastValueMap.get(incomingVal);
      }
//      phi.COaddOperand(incomingVal);
      phi.CoReplaceOperandByIndex(exitPreHeaderIndex, incomingVal);
      cacheIndex++;
    }

    // link latchBlock and secondHeader
    factory.buildBr(secondHeader, latchBlock);
    header.getPredecessor_().remove(latchBlock);
    linkBasicBlock(latchBlock, secondHeader);

    // link secondLatch and header
    BasicBlock trueBB = latchBrHeaderIndex == 1 ? header : exitIfBB;
    BasicBlock falseBB = trueBB == header ? exitIfBB : header;
    factory.buildBr(newIcmpInst, trueBB, falseBB, secondLatch);
    linkBasicBlock(secondLatch, header);
  }

  private void linkBasicBlock(BasicBlock pred, BasicBlock succ) {
    if (!succ.getPredecessor_().contains(pred)) {
      succ.getPredecessor_().add(pred);
    }
    if (!pred.getSuccessor_().contains(succ)) {
      pred.getSuccessor_().add(succ);
    }
  }

  private void updatePhiNewIncoming(BasicBlock bb, int predIndex, Loop loop,
      HashMap<Value, Value> lastValueMap) {
    for (var instNode : bb.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }

      var phi = (Phi) inst;
      var incomingVal = phi.getIncomingVals().get(predIndex);
      if (incomingVal instanceof Instruction && (loop.getBlocks()
          .contains(((Instruction) incomingVal).getBB()))) {
        incomingVal = lastValueMap.get(incomingVal);
      }
      phi.CoReplaceOperandByIndex(predIndex, incomingVal);
    }
  }

  // 根据 LoopInfo 分析情况调整
  private boolean canDoubleUnroll(Loop loop) {
    return loop.getIndVar() != null &&
        (loop.getStepInst().tag == TAG_.Sub || loop.getStepInst().tag == TAG_.Add) &&
        (loop.getLatchCmpInst().tag.ordinal() >= TAG_.Lt.ordinal() &&
            loop.getLatchCmpInst().tag.ordinal() <= TAG_.Gt.ordinal());
  }
}
