package pass;

import ir.MyModule;
import ir.values.Function;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.IList.INode;
import util.Mylogger;

public class GVNGCM implements IRPass {

  Logger log = Mylogger.getLogger(IRPass.class);

  @Override
  public String getName() {
    return "gvngcm";
  }

  public void run(MyModule m) {
    log.info("Running pass : GVNGCM");

    for (INode<Function, MyModule> funcNode : m.__functions) {
      runGVNGCM(funcNode.getVal());
    }
  }

  public void runGVNGCM(Function func) {

  }
}
