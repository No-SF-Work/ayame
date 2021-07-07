package pass;

import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import java.util.ArrayList;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import util.IList.INode;
import util.Mylogger;

public class GVNGCM implements IRPass {

  Logger log = Mylogger.getLogger(IRPass.class);

  ArrayList<BasicBlock> bbRpoList = new ArrayList<>();

  @Override
  public String getName() {
    return "gvngcm";
  }

  public void run(MyModule m) {
    log.info("Running pass : GVNGCM");

    // I don't know if there is a better way than using Tsinghua's bbopt->gvngcm->bbopt steps:
    // while (code_changed) {
    //    bb_opt();
    //    gvngcm();
    // }
    for (INode<Function, MyModule> funcNode : m.__functions) {
      runGVNGCM(funcNode.getVal());
    }
  }

  public void runGVNGCM(Function func) {

  }
}
