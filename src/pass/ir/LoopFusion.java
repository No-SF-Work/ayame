package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.instructions.Instruction;
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
    var predHeader = predLoop.getLoopHeader();
    var commonBB = succLoop.getPreHeader();
    HashSet<Instruction> opIrrelevantInstSet = new HashSet<>();
    HashSet<Instruction> userIrrelevantInstSet = new HashSet<>();
    HashSet<Instruction> irrelevantInstSet = new HashSet<>();
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

    }
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
        || predLoop.getLatchCmpInst().tag != succLoop.getLatchCmpInst().tag) {
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
