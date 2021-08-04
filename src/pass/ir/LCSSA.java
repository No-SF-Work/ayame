package pass.ir;

import ir.Analysis.LoopInfo;
import ir.Loop;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.values.Function;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.Phi;
import java.util.HashSet;
import java.util.Queue;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

/**
 * 在循环退出时跳转到的基本块开头插入冗余 phi 指令，phi 指令 use 循环内定义的值，循环后面 use 循环内定义的值替换成 use phi，方便循环上的优化
 */
public class LCSSA implements IRPass {

  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();
  private LoopInfo currLoopInfo;

  public static void addLoopToQueue(Loop loop, Queue<Loop> queue) {
    queue.add(loop);
    for (var subLoop : loop.getSubLoops()) {
      if (subLoop != null) {
        queue.add(subLoop);
      }
    }
  }

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
            set.add(userInst);
            break;
          }
        }
      }
    }
  }
}
