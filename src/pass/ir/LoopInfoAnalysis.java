package pass.ir;

import ir.MyModule;
import pass.Pass.IRPass;

public class LoopInfoAnalysis implements IRPass {

  @Override
  public String getName() {
    return "loopInfoAnalysis";
  }

  @Override
  public void run(MyModule m) {
    for (var funcNode: m.__functions) {
      var func = funcNode.getVal();
      if (!func.isBuiltin_()) {
        func.getLoopInfo().computeLoopInfo(func);
      }
    }
  }
}
