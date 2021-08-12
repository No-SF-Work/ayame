package pass.ir;

import ir.Analysis.DomInfo;
import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.Use;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.UndefValue;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.Instruction.TAG_;
import ir.values.instructions.MemInst.Phi;
import pass.Pass.IRPass;
import util.Mylogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * 在循环退出时跳转到的基本块开头插入冗余 phi 指令，phi 指令 use 循环内定义的值，循环后面 use 循环内定义的值替换成 use phi，方便循环上的优化
 */
public class LCSSA implements IRPass {

  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;

  @Override
  public String getName() {
    return "LCSSA";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : LCSSA");

    for (var funcNode : m.__functions) {
      var func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        runOnFunction(func);
      }
    }
  }

  public void runOnFunction(Function func) {
    DomInfo.computeDominanceInfo(func);
    var loopInfoFullAnalysis = new LoopInfoFullAnalysis();
    loopInfoFullAnalysis.runOnFunction(func);

    currLoopInfo = func.getLoopInfo();
    for (var topLoop : currLoopInfo.getTopLevelLoops()) {
      runOnLoop(topLoop);
    }
  }

  public void runOnLoop(Loop loop) {
    for (var subLoop : loop.getSubLoops()) {
      if (subLoop != null) {
        runOnLoop(subLoop);
      }
    }

    HashSet<Instruction> usedOutLoopSet = getUsedOutLoopSet(loop); // 循环里定义，循环外使用的指令
    if (usedOutLoopSet.isEmpty()) {
      return;
    }

    HashSet<BasicBlock> exitBlocks = loop.getExitBlocks();
    if (exitBlocks == null || exitBlocks.isEmpty()) {
      return;
    }

    for (var inst : usedOutLoopSet) {
      generateLoopClosedPhi(inst, loop);
    }
  }

  // 删掉 inst 在循环外的 use，用 phi 代替
  private void generateLoopClosedPhi(Instruction inst, Loop loop) {
    var bb = inst.getBB();
    HashMap<BasicBlock, Value> bbToPhiMap = new HashMap<>();

    // 在循环出口的基本块开头放置 phi，参数为 inst，即循环内定义的变量
    for (var exitBB : loop.getExitBlocks()) {
      if (!bbToPhiMap.containsKey(exitBB) && exitBB.getDomers().contains(bb)) {
        Phi phi = new Phi(TAG_.Phi, inst.getType(), exitBB.getPredecessor_().size(), exitBB);
        bbToPhiMap.put(exitBB, phi);
        for (int i = 0; i < exitBB.getPredecessor_().size(); i++) {
          phi.CoReplaceOperandByIndex(i, inst);
        }
      }
    }

    // 维护 inst 的循环外 user
    ArrayList<Use> usesList = new ArrayList<>(inst.getUsesList());
    for (var use : usesList) {
      var userInst = (Instruction) use.getUser();
      var userBB = userInst.getBB();
      if (userInst instanceof Phi) {
        var phi = (Phi) userInst;
        int idx = 0;
        for (var value : phi.getIncomingVals()) {
          if (value.getUsesList().contains(use)) {
            userBB = phi.getBB().getPredecessor_().get(idx);
          }
          idx++;
        }
      }

      if (userBB == bb || loop.getBlocks().contains(userBB)) {
        continue;
      }

      // 维护 phi 的 use
      var value = getValueForBB(userBB, inst, bbToPhiMap, loop);
      userInst.COReplaceOperand(inst, value);
    }
  }

  // inst 在到达 bb 时的值
  public Value getValueForBB(BasicBlock bb, Instruction inst,
      HashMap<BasicBlock, Value> bbToPhiMap, Loop loop) {
    if (bb == null) {
      return new UndefValue();
    }
    if (bbToPhiMap.get(bb) != null) {
      return bbToPhiMap.get(bb);
    }

    var idom = bb.getIdomer();
    if (!loop.getBlocks().contains(idom)) {
      var value = getValueForBB(idom, inst, bbToPhiMap, loop);
      bbToPhiMap.put(bb, value);
      return value;
    }

    var phi = new Phi(TAG_.Phi, factory.getI32Ty(), bb.getPredecessor_().size(), bb);
    bbToPhiMap.put(bb, phi);
    for (int i = 0; i < bb.getPredecessor_().size(); i++) {
      phi.CoReplaceOperandByIndex(i,
          getValueForBB(bb.getPredecessor_().get(i), inst, bbToPhiMap, loop));
    }
    return phi;
  }

  public HashSet<Instruction> getUsedOutLoopSet(Loop loop) {
    HashSet<Instruction> set = new HashSet<>();

    for (var bb : loop.getBlocks()) {
      for (var instNode : bb.getList()) {
        var inst = instNode.getVal();
        for (var use : inst.getUsesList()) {
          var user = use.getUser();
          assert user instanceof Instruction;
          var userInst = (Instruction) user;
          var userBB = userInst.getBB();
          if (userInst instanceof Phi) {
            var phi = (Phi) userInst;
            int idx = 0;
            for (var value : phi.getIncomingVals()) {
              if (value.getUsesList().contains(use)) {
                userBB = phi.getBB().getPredecessor_().get(idx);
              }
              idx++;
            }
          }

          if (bb != userBB && !loop.getBlocks().contains(userBB)) {
            set.add(inst);
            break;
          }
        }
      }
    }

    return set;
  }
}
