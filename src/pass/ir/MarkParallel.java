package pass.ir;

import ir.MyModule;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

// 识别到可并行循环后，在循环头基本块处标记
public class MarkParallel implements IRPass {

  private static Logger log = Mylogger.getLogger(IRPass.class);

  @Override
  public String getName() {
    return "markParallel";
  }

  @Override
  public void run(MyModule m) {

  }
}
