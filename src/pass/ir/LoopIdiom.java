package pass.ir;

import driver.Config;
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
import ir.values.instructions.MemInst.Phi;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.LoopUtils;
import util.Mylogger;

public class LoopIdiom implements IRPass {

  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;

  @Override
  public String getName() {
    return "loopIdiom";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : LoopIdiom");

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

  public void runOnLoop(Loop loop) {
    runSimplifyCalc(loop);
    // TODO loop -> memset/memcpy
  }

  public void runSimplifyCalc(Loop loop) {
    if (!loop.getSubLoops().isEmpty() || !loop.isSimpleForLoop() || loop.getBlocks().size() != 1) {
      return;
    }

    var header = loop.getLoopHeader();
    // 或许太严格了
    if (header.getList().getNumNode() != 6) {
      return;
    }

    var stepInst = loop.getStepInst();
    var indVar = loop.getIndVar();
    var latchCmpInst = loop.getLatchCmpInst();
    var latchBrInst = loop.getSingleLatchBlock().getList().getLast().getVal();
    Phi sumInst = null;
    BinaryInst calcInst = null;
    if (indVar == null || stepInst == null) {
      return;
    }

    for (var instNode : header.getList()) {
      var inst = instNode.getVal();
      if (inst != stepInst && inst != indVar && inst != latchCmpInst) {
        if (inst instanceof BinaryInst) {
          calcInst = (BinaryInst) inst;
        } else if (inst instanceof Phi) {
          sumInst = (Phi) inst;
        }
      }
    }

    if (sumInst == null || calcInst == null) {
      return;
    }

    // sumInst = [ baseSum , %preHeader ], [ calcInst , %latch ]
    // calcInst = OP sumInst num
    // => %1 = OP2 tripCount num
    //    %2 = OP baseSum %1
    int latchPredIndex = header.getPredecessor_().indexOf(header);
    var baseSum = sumInst.getIncomingVals().get(1 - latchPredIndex);
    if (calcInst != sumInst.getIncomingVals().get(latchPredIndex)) {
      return;
    }
    int sumInstOpIndex = calcInst.getOperands().indexOf(sumInst);
    if (sumInstOpIndex == -1) {
      return;
    }
    var num = calcInst.getOperands().get(1 - sumInstOpIndex);
    if (num instanceof Instruction &&
        (((Instruction) num).getBB().getLoopDepth() == loop.getLoopDepth())) {
      return;
    }

    BasicBlock exit = null;
    if (loop.getBlocks().contains((BasicBlock) (latchBrInst.getOperands().get(1)))) {
      exit = (BasicBlock) (latchBrInst.getOperands().get(2));
    } else {
      exit = (BasicBlock) (latchBrInst.getOperands().get(1));
    }

    // start transform
    // 计算迭代次数 tripCount
    // FIXME: tripCount 为非正数时需要跳过循环
    var init = loop.getIndVarInit();
    var end = loop.getIndVarEnd();
    var step = loop.getStep();
    var indVarCondInst = loop.getIndVarCondInst();
    Value compareBias = null;
    for (var op : indVarCondInst.getOperands()) {
      if (!(op instanceof Phi)) {
        compareBias = op;
      }
    }

    Instruction tripCountInst = null;
    // 向上取整：b / a => (b + a - 1) / a
    // Lt, Gt: |end - init| / |step|
    // Le, Ge: (|end - init| + 1) / |step|
    var subInst1 = factory.buildBinary(TAG_.Sub, end, init, header);
    var subInst2 = factory.buildBinary(TAG_.Sub, compareBias, step, header); // 大部分情况下是 0
    var indVarRange = factory.buildBinary(TAG_.Sub, subInst1, subInst2, header);
    switch (latchCmpInst.tag) {
      case Lt, Gt -> {
        tripCountInst = buildCeilDiv(header, indVarRange, step);
      }
      case Le -> {
        var addInst = factory.buildBinary(TAG_.Add, indVarRange,
            ConstantInt.newOne(factory.getI32Ty(), 1), header);
        tripCountInst = buildCeilDiv(header, addInst, step);
      }
      case Ge -> {
        var subInst3 = factory.buildBinary(TAG_.Sub, indVarRange,
            ConstantInt.newOne(factory.getI32Ty(), 1), header);
        tripCountInst = buildCeilDiv(header, subInst3, step);
      }
    }

    // 构造新的 calcInst 和 sumInst
    Instruction noLoopSumInst = null, noLoopCalcInst = null;
    switch (calcInst.tag) {
      case Add, Sub -> {
        noLoopCalcInst = factory.buildBinary(TAG_.Mul, tripCountInst, num, header);
        noLoopSumInst = factory.buildBinary(calcInst.tag, baseSum, noLoopCalcInst, header);
      }
      case Mul, Div -> {
        if (!(num instanceof ConstantInt)) {
          return;
        }
        int cnum = ((ConstantInt) num).getVal();
        if (!(cnum > 0 && (cnum & (cnum - 1)) == 0)) {
          return;
        }
        // FIXME lowbit 失效
        ConstantInt shiftTime = ConstantInt
            .newOne(factory.getI32Ty(), (int) (Math.log(cnum) / Math.log(2)));
        noLoopCalcInst = factory.buildBinary(TAG_.Mul, tripCountInst, shiftTime, header);
        if (calcInst.tag == TAG_.Mul) {
          noLoopSumInst = factory.buildBinary(TAG_.Shl, baseSum, noLoopCalcInst, header);
        } else {
          noLoopSumInst = factory.buildBinary(TAG_.Shr, baseSum, noLoopCalcInst, header);
        }
      }
    }

    // exit 的 phi 中替换对应来自循环的 calcInst 为 noLoopSumInst
    int headerPredIndex = exit.getPredecessor_().indexOf(header);
    for (var instNode : exit.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof Phi)) {
        break;
      }
      if (inst.getOperands().get(headerPredIndex) == calcInst) {
        inst.CoReplaceOperandByIndex(headerPredIndex, noLoopSumInst);
      }
    }

    // TODO 如果循环结束后还有用到 i 的情况，构造 i 的 sum

    // 将循环改造成单个基本块，删掉原循环指令，跳转到 exit
    header.getSuccessor_().remove(header);
    header.getPredecessor_().remove(header);
    indVar.node.removeSelf();
    indVar.CORemoveAllOperand();
    sumInst.node.removeSelf();
    sumInst.CORemoveAllOperand();
    calcInst.node.removeSelf();
    calcInst.CORemoveAllOperand();
    stepInst.node.removeSelf();
    stepInst.CORemoveAllOperand();
    latchCmpInst.node.removeSelf();
    latchCmpInst.CORemoveAllOperand();
    latchBrInst.node.removeSelf();
    latchBrInst.CORemoveAllOperand();

    factory.buildBr(exit, header);

    // 删除循环
    currLoopInfo.removeLoop(loop);

    Config.getInstance().runStableRegAlloc = true;
    Config.getInstance().isAggressiveDiv = true;
  }

  public BinaryInst buildCeilDiv(BasicBlock bb, Value lhs, Value rhs) {
    // FIXME 负数的向上取整
    var addInst = factory.buildBinary(TAG_.Add, lhs, rhs, bb);
    var subInst = factory
        .buildBinary(TAG_.Sub, addInst, ConstantInt.newOne(factory.getI32Ty(), 1), bb);
    var divInst = factory.buildBinary(TAG_.Div, subInst, rhs, bb);
    return divInst;
  }
}
