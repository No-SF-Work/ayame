package pass.ir;

import ir.MyFactoryBuilder;
import ir.MyModule;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

public class LoopMergeLastBreak implements IRPass {

  private static final Logger log = Mylogger.getLogger(IRPass.class);
  private static final MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  @Override
  public String getName() {
    return "loopMergeLastBreak";
  }

  @Override
  public void run(MyModule m) {
    log.info("Running pass : LoopMergeLastBreak");

    // TODO 需要递归判断无用返回值
  }
}
