package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.UndefValue;
import ir.values.Value;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.TerminatorInst.BrInst;
import ir.values.instructions.TerminatorInst.CallInst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.LoopUtils;
import util.Mylogger;

public class LoopMergeLastBreak implements IRPass {

  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;

  @Override
  public String getName() {
    return "loopMergeLastBreak";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : LoopMergeLastBreak");

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

  // TODO 需要递归判断无用返回值
  public void runOnLoop(Loop loop) {
    if (!structureJudge(loop)) {
      return;
    }

    var preHeader = loop.getPreHeader();
    var header = loop.getLoopHeader();
    var latchBlock = loop.getSingleLatchBlock();
    for (var instNode : latchBlock.getList()) {
      var inst = instNode.getVal();
      if (inst instanceof StoreInst || inst instanceof CallInst) {
        return;
      }
      for (var use : inst.getUsesList()) {
        var user = use.getUser();
        if (user instanceof Instruction && ((Instruction) user).getBB() != latchBlock) {
          return;
        }
      }
    }

    BasicBlock exit = latchBlock.getSuccessor_()
        .get(1 - latchBlock.getSuccessor_().indexOf(header));

    // 普通的循环分析拿不到 indVar，这里根据假设的特征特判
    // FIXME maybe bug here
    var latchCmpInst = loop.getLatchCmpInst();
    Instruction stepInst = null;
    Phi indVar = null;
    Value indVarInit = null;
    Value indVarEnd = null;
    Value step = null;
    for (var op : latchCmpInst.getOperands()) {
      if (op instanceof Instruction && ((Instruction) op).getBB().getLoopDepth() == latchCmpInst
          .getBB().getLoopDepth()) {
        stepInst = (Instruction) op;
      } else {
        indVarEnd = op;
      }
    }
    assert stepInst != null;
    for (var op : stepInst.getOperands()) {
      if (op instanceof Phi && ((Phi) op).getIncomingVals().contains(stepInst)) {
        indVar = (Phi) op;
      } else {
        step = op;
      }
    }
    assert indVar != null;
    for (var op : indVar.getIncomingVals()) {
      if (op != stepInst) {
        indVarInit = op;
      }
    }

    // 假设计算都在 header 中，header 的 br 对应 break，latch 的判断对应 while
    // header 的计算尽可能移到 latch 中，在 header 和原 preheader 之间新建一个 preheader，并作为循环的新 preheader
    // preHeader 和 secondPreHeader 之间插入一个基本块进行运算，指令数较小时可被后端优化成条件执行
    // start transform
    // build secondPreHeader
    HashMap<Value, Value> lastValueMap = new HashMap<>();
    ArrayList<Phi> originPhis = new ArrayList<>();
    int preHeaderIndex = header.getPredecessor_().indexOf(preHeader);
    for (var instNode : header.getList()) {
      var inst = instNode.getVal();
      if (inst instanceof Phi) {
        Phi phi = (Phi) inst;
        originPhis.add(phi);
        lastValueMap.put(phi, phi.getIncomingVals().get(preHeaderIndex));
      }
    }
    // 构建 calcBB
    HashMap<Value, Value> valueMap = new HashMap<>();
    BasicBlock calcBB = LoopUtils.getLoopBasicBlockCopy(header, valueMap);
    currLoopInfo.addBBToLoop(calcBB, loop.getParentLoop());
    for (var phi : originPhis) {
      valueMap.put(phi, lastValueMap.get(phi));
    }
    for (var instNode = calcBB.getList().getEntry(); instNode != null; ) {
      var inst = instNode.getVal();
      var tmp = instNode.getNext();
      if (inst instanceof Phi || inst instanceof BrInst) {
        inst.CORemoveAllOperand();
        instNode.removeSelf();
      } else {
        LoopUtils.remapInstruction(inst, valueMap);
      }
      instNode = tmp;
    }

    // preHeader 跳转到 calcBB
    var preBrInst = preHeader.getList().getLast().getVal();
    if (preBrInst.getOperands().size() == 1) {
      return;
    }
    preBrInst.CoReplaceOperandByIndex(1, calcBB);

    // 构建 secondPreHeader，Phi 通过 remap 替换，Br 另外构建新的，跳转到 header 和 exit，Br 对应的 icmp 是 preheader 的 icmp
    BasicBlock secondPreHeader = factory.buildBasicBlock("", header.getParent());
    currLoopInfo.addBBToLoop(secondPreHeader, loop.getParentLoop());

    // calcBB 的 br
    factory.buildBr(secondPreHeader, calcBB);
    // preHeader 跳转到 secondPreHeader
    preBrInst.CoReplaceOperandByIndex(2, secondPreHeader);


    // secondPreHeader 的 phi：复制 header 的 phi。来自 preHeader 不变，来自 calcBB remap
    // 维护完成后修改 lastValueMap 的 phi
    int predIndex = header.getPredecessor_().indexOf(preHeader);
    int latchPredIndex = header.getPredecessor_().indexOf(latchBlock);
    for (var phi : originPhis) {
      ArrayList<Value> incoming = new ArrayList<>();
      incoming.add(new UndefValue());
      incoming.add(new UndefValue());
      var calcIncoming = valueMap.get(phi.getIncomingVals().get(latchPredIndex));
      incoming.set(predIndex, phi.getIncomingVals().get(predIndex));
      incoming.set(latchPredIndex, calcIncoming);
      var newPhi = new Phi(TAG_.Phi, factory.getI32Ty(), 2, incoming, secondPreHeader);
      valueMap.put(phi.getIncomingVals().get(latchPredIndex), newPhi);
    }

    // secondPreHeader 的 icmp 和 br
    var preCmpInst = (BinaryInst) preBrInst.getOperands().get(0);
    // secondPreHeader 中的 icmp 是 preheader 的复制，不过迭代器位置上是 stepInst 在 secondPreHeader 中的对应 value（只适用于最简单情况）（可以换成
    // latchCmpInst 的 remap）
    // FIXME maybe bug here
    var preIndVarEndIndex = preCmpInst.getOperands().indexOf(indVarEnd);
    Value lhs = null, rhs = null;
    lhs = preIndVarEndIndex == 0 ? indVarEnd : valueMap.get(stepInst);
    rhs = preIndVarEndIndex == 1 ? indVarEnd : valueMap.get(stepInst);
    BinaryInst secondCmpInst = factory.buildBinary(preCmpInst.tag, lhs, rhs, secondPreHeader);

    BasicBlock trueBB = preBrInst.getOperands().get(1) == calcBB ? header : exit;
    BasicBlock falseBB = preBrInst.getOperands().get(1) == calcBB ? exit : header;
    factory.buildBr(secondCmpInst, trueBB, falseBB, secondPreHeader);
    preBrInst.CoReplaceOperandByIndex(2, secondPreHeader);
    // 手动构造的代码生成出的 ir 结构：secondPreHeader/header/latch 跳到的 exit 只有一个 phi，preheader 和 exit 跳到一个基本块，里面是真正的
    // exit，出现性能问题再改成这样

    // 维护前驱后继关系
    preHeader.getSuccessor_().remove(header);
    preHeader.getSuccessor_().remove(exit);
    preHeader.getSuccessor_().add(secondPreHeader);
    preHeader.getSuccessor_().add(calcBB);
    calcBB.getPredecessor_().add(preHeader);
    calcBB.getSuccessor_().add(secondPreHeader);
    secondPreHeader.getPredecessor_().add(predIndex == 0 ? preHeader : calcBB);
    secondPreHeader.getPredecessor_().add(predIndex == 1 ? preHeader : calcBB);
    secondPreHeader.getSuccessor_().add(header);
    secondPreHeader.getSuccessor_().add(exit);
    header.getPredecessor_().set(preHeaderIndex, secondPreHeader);
    int preHeaderIndexOfExit = exit.getPredecessor_().indexOf(preHeader);
    exit.getPredecessor_().set(preHeaderIndexOfExit, secondPreHeader);

    // 维护 phi（header 和 exit）
    // header: preHeader 来源替换成 secondPreHeader 来源，即 valueMap.get(incomingVal.get(latchIndex))
    for (var instNode : header.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }
      var incomingVal = valueMap.get(((Phi) inst).getIncomingVals().get(latchPredIndex));
      inst.CoReplaceOperandByIndex(preHeaderIndex,
          incomingVal); // preHeaderIndex 上对应的是 secondPreHeader
    }

    // exit: secondPreHeader 来源的 incomingVal 是 valueMap.get(incomingVal.get(latchIndex))
    int latchPredIndexOfExit = exit.getPredecessor_().indexOf(latchBlock);
    int headerIndexOfExit = exit.getPredecessor_().indexOf(header);
    for (var instNode : exit.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }
      var incomingVal = ((Phi) inst).getIncomingVals().get(latchPredIndexOfExit);
      incomingVal = valueMap.get(incomingVal);
      inst.CoReplaceOperandByIndex(preHeaderIndexOfExit, incomingVal);
    }

    // header 中的计算移到 latch
    // 某些情况下，header 的 icmp 需要数组取址，而数组取址需要 header 中的运算，把运算移到 latch 的同时取址操作数改成对应的 phi 才正确
    // 上面的只是一般情况，可能还有更复杂的
    // 只移动 Add, Sub, Mul, Div，剩下的指令通过 valueMap 重新映射（可能映射错误）
    lastValueMap.clear();
    for (var instNode = header.getList().getEntry(); instNode != null; ) {
      var inst = instNode.getVal();
      var tmp = instNode.getNext();
      if (isFourArithOp(inst)) {
        for (var phi : originPhis) {
          if (phi.getIncomingVals().contains(inst)) {
            instNode.removeSelf();

            boolean hasUser = false;
            for (var latchInstNode : latchBlock.getList()) {
              var latchInst = latchInstNode.getVal();
              if (latchInst.getOperands().contains(inst)) {
                instNode.insertBefore(latchInst.node);
                hasUser = true;
                break;
              }
            }
            if (!hasUser) {
              instNode.insertAtSecondToEnd(latchBlock.getList());
            }
            lastValueMap.put(inst, phi);
            break;
          }
        }
      }
      instNode = tmp;
    }

    header.getList().forEach(instNode -> {
      if (!(instNode.getVal() instanceof Phi)) {
        LoopUtils.remapInstruction(instNode.getVal(), lastValueMap);
      }
    });

    // header 到 exit 的 phi 也需要更新
    int headerPredIndexOfExit = exit.getPredecessor_().indexOf(header);
    for (var instNode : exit.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }
      var headerIncoming = lastValueMap
          .get(((Phi) inst).getIncomingVals().get(headerPredIndexOfExit));
      inst.CoReplaceOperandByIndex(headerPredIndexOfExit, headerIncoming);
    }
  }

  public boolean structureJudge(Loop loop) {
    return loop.getBlocks().size() == 2
        && loop.getLoopHeader().getPredecessor_().size() == 2
        && loop.getExitingBlocks().size() == 2
        && loop.getLoopHeader() != loop.getSingleLatchBlock();
  }

  public boolean isFourArithOp(Instruction inst) {
    return inst.tag == TAG_.Add || inst.tag == TAG_.Sub || inst.tag == TAG_.Mul
        || inst.tag == TAG_.Div;
  }
}
