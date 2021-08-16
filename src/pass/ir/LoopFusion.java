package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst.BrInst;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;
import util.Pair;

// 只考虑最里层循环
public class LoopFusion implements IRPass {

  private static Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;
  private Function currFunction;

  @Override
  public String getName() {
    return "loopFusion";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : LoopFusion");

    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        runOnFunction(func);
      }
    }
  }

  public void runOnFunction(Function func) {
    this.currLoopInfo = func.getLoopInfo();
    this.currFunction = func;

    ArrayList<Pair<Loop, Loop>> fusionPairList = new ArrayList<>();
    HashSet<Loop> fusionSet = new HashSet<>();

    for (var predLoop : currLoopInfo.getAllLoops()) {
      if (predLoop.getSubLoops().isEmpty() && fusionConditionForSingleLoop(predLoop) && !fusionSet
          .contains(predLoop)) {
        for (var succLoop : currLoopInfo.getAllLoops()) {
          if (succLoop.getSubLoops().isEmpty() && fusionConditionForSingleLoop(succLoop)
              && !fusionSet.contains(succLoop)) {
            if (fusionConditionForTwoLoops(predLoop, succLoop)) {
              Pair<Loop, Loop> pair = new Pair<>(predLoop, succLoop);
              fusionPairList.add(pair);
              fusionSet.add(predLoop);
              fusionSet.add(succLoop);
            }
          }
        }
      }
    }

    for (var pair : fusionPairList) {
      runOnLoops(pair.getFirst(), pair.getSecond());
    }
  }

  public void runOnLoops(Loop predLoop, Loop succLoop) {
    // TODO: pred exit/succ preheader 中的指令是否能移到 pred preheader
    var predPreHeader = predLoop.getPreHeader();
    var predHeader = predLoop.getLoopHeader();
    var succHeader = succLoop.getLoopHeader();
    var commonBB = succLoop.getPreHeader();
    var succExit = succLoop.getExitBlocks().iterator().next();
    HashSet<Instruction> opIrrelevantInstSet = new HashSet<>();
    HashSet<Instruction> userIrrelevantInstSet = new HashSet<>();
    HashSet<Instruction> irrelevantInstSet = new HashSet<>();
    HashSet<Instruction> relevantInstSet = new HashSet<>();
    for (var instNode : commonBB.getList()) {
      var inst = instNode.getVal();
      if (!(inst instanceof BrInst)) {
        boolean isIrrelevant = true;
        for (var op : inst.getOperands()) {
          if (!(op instanceof Instruction)) {
            continue;
          }
          Instruction opInst = (Instruction) op;
          if (!opIrrelevantInstSet.contains(op) && !predHeader.getDomers()
              .contains(opInst.getBB())) {
            isIrrelevant = false;
          }
        }
        if (isIrrelevant) {
          opIrrelevantInstSet.add(inst);
        }
      }
    }
    for (var instNode = commonBB.getList().getLast(); instNode != null;
        instNode = instNode.getPrev()) {
      var inst = instNode.getVal();
      if (!(inst instanceof BrInst)) {
        boolean isIrrelevant = true;
        for (var use : inst.getUsesList()) {
          if (!(use.getUser() instanceof Instruction)) {
            continue;
          }
          Instruction userInst = (Instruction) use.getUser();
          if (!userIrrelevantInstSet.contains(userInst) && !userInst.getBB().getDomers()
              .contains(predPreHeader)) {
            isIrrelevant = false;
          }
          if (isIrrelevant) {
            userIrrelevantInstSet.add(inst);
          }
        }
      }
    }
    irrelevantInstSet.addAll(opIrrelevantInstSet);
    irrelevantInstSet.retainAll(userIrrelevantInstSet);
    commonBB.getList().forEach(instNode -> {
      if (!(irrelevantInstSet.contains(instNode.getVal()))) {
        relevantInstSet.add(instNode.getVal());
      }
    });

    var brInst = relevantInstSet.iterator().next();
    if (!(brInst instanceof BrInst)) {
      return;
    }

    // irrelevantInst 移动到 predPreHeader
    for (var inst : irrelevantInstSet) {
      inst.node.removeSelf();
      inst.node.insertAtSecondToEnd(predPreHeader.getList());
    }

    System.out.println("fusion");

    // predHeader 直接跳到 succHeader
    // succHeader 对 indVar 的 use 换成对 predHeader indVar 的 use
    // predHeader 和 succHeader 的 indVarCondInst 和 latchCmpInst 同构
    // TODO: 检查 Br trueBB falseBB 的对应是否相同
    var predBrInst = predHeader.getList().getLast().getVal();
    predBrInst.CORemoveAllOperand();
    predBrInst.COaddOperand(succHeader);
    succHeader.getList().forEach(instNode -> {
      var inst   = instNode.getVal();
      inst.COReplaceOperand(succLoop.getIndVar(), predLoop.getIndVar());
    });
    // succHeader 的 phi 移到 predHeader 前面
    for (var instNode = succHeader.getList().getEntry(); instNode != null; ) {
      var inst = instNode.getVal();
      var tmp = instNode.getNext();
      if (!(inst instanceof Phi)) {
        break;
      }
      instNode.removeSelf();
      instNode.insertAtEntry(predHeader.getList());
      instNode = tmp;
    }

    int predHeaderPredIndexOfPredLatch = predHeader.getPredecessor_().indexOf(predHeader);
    int succHeaderPredIndexOfSuccLatch = succHeader.getPredecessor_().indexOf(succHeader);
    int succHeaderPredIndexOfSuccPreHeader = succHeader.getPredecessor_().indexOf(commonBB);
    int succExitPredIndexOfSuccPreHeader = succExit.getPredecessor_().indexOf(commonBB);
    int predPreHeaderSuccIndexOfPredExit = predPreHeader.getSuccessor_().indexOf(commonBB);
    int predLatchSuccIndexOfPredHeader = predHeader.getSuccessor_().indexOf(predHeader);
    int predLatchSuccIndexOfPredExit = predHeader.getSuccessor_().indexOf(commonBB);
    int succLatchSuccIndexOfSuccHeader = succHeader.getSuccessor_().indexOf(succHeader);

    predHeader.getPredecessor_().set(predHeaderPredIndexOfPredLatch, succHeader);
    succHeader.getPredecessor_().clear();
    succHeader.getPredecessor_().add(predHeader);
    succExit.getPredecessor_().set(succExitPredIndexOfSuccPreHeader, predPreHeader);

    predPreHeader.getSuccessor_().set(predPreHeaderSuccIndexOfPredExit, succExit);
    predHeader.getSuccessor_().clear();
    predHeader.getSuccessor_().add(succHeader);
    succHeader.getSuccessor_().set(succLatchSuccIndexOfSuccHeader, predHeader);

    BrInst predPreBrInst = (BrInst) predPreHeader.getList().getLast().getVal();
    BrInst succBrInst = (BrInst) succHeader.getList().getLast().getVal();
    predPreBrInst.COReplaceOperand(commonBB, succExit);
    succBrInst.COReplaceOperand(succHeader, predHeader);
  }

  public boolean fusionConditionForSingleLoop(Loop loop) {
    if (!loop.isSimpleForLoop() || loop.getBlocks().size() > 1) {
      return false;
    }
    if (loop.getIndVar() == null || loop.getIndVarInit() == null || loop.getIndVarEnd() == null
        || loop.getIndVarCondInst() == null || loop.getStep() == null) {
      return false;
    }
    return true;
  }

  // predLoop 的 exit 和 succLoop 的 preHeader 相同，去除可移动到 predLoop 的 preHeader 的指令后，只剩一个和 predLoop 的 preHeader 相同的 Br
  // 怎么判断可移动？
  //   operand 所在基本块都支配 predLoop header，取闭包；user 都被 predLoop header 支配，取闭包
  // 闭包外只剩一条 Br
  public boolean fusionConditionForTwoLoops(Loop predLoop, Loop succLoop) {
    if (predLoop.getParentLoop() != succLoop.getParentLoop() || predLoop == succLoop) {
      return false;
    }
    var predExitBlock = predLoop.getExitBlocks().iterator().next();
    var succPreHeader = succLoop.getPreHeader();
    if (predExitBlock != succPreHeader) {
      return false;
    }
    if (predLoop.getIndVarInit() != succLoop.getIndVarInit()
        || predLoop.getIndVarEnd() != succLoop.getIndVarEnd()
        || predLoop.getLatchCmpInst().tag != succLoop.getLatchCmpInst().tag
        || !predLoop.getIndVarCondInst().getOperands().contains(predLoop.getIndVar())
        || !succLoop.getIndVarCondInst().getOperands().contains(succLoop.getIndVar())
    ) {
      return false;
    }
    if (predLoop.getStep() instanceof ConstantInt && succLoop.getStep() instanceof ConstantInt
        && ((ConstantInt) predLoop.getStep()).getVal() != ((ConstantInt) succLoop.getStep())
        .getVal()) {
      return false;
    }
    if (!(predLoop.getStep() instanceof ConstantInt) && predLoop.getStep() != succLoop.getStep()) {
      return false;
    }

    var predPreBrInst = predLoop.getPreHeader().getList().getLast().getVal();
    var succPreBrInst = succPreHeader.getList().getLast().getVal();
    if (predPreBrInst.getNumOP() != 3 || succPreBrInst.getNumOP() != 3) {
      return false;
    }
    // Unroll 之前，相同操作数的 icmp 未合并
    Instruction predPreCmpInst = (Instruction) predPreBrInst.getOperands().get(0);
    Instruction succPreCmpInst = (Instruction) succPreBrInst.getOperands().get(0);
    if (predPreCmpInst.getOperands().get(0) != succPreCmpInst.getOperands().get(0)
        || predPreCmpInst.getOperands().get(1) != succPreCmpInst.getOperands().get(1)) {
      return false;
    }

//    System.out.println("fusioning loops");

    return true;
  }
}
