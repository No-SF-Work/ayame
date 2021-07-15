package pass.ir;

import ir.MyModule;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.Mylogger;

public class DeadCodeEmit implements IRPass {

  Logger log = Mylogger.getLogger(IRPass.class);

  @Override
  public String getName() {
    return "deadcodeemit";
  }

  public void run(MyModule m) {
    log.info("Running pass : DeadCodeEmit");
    //do something
  }
}
