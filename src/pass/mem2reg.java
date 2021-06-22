package pass;

import ir.CFGInfo;
import ir.values.Function;

public class mem2reg {

  public void run(Function func) {
    CFGInfo.computeDominanceInfo(func);
    CFGInfo.computeDominanceFrontier(func);
  }
}
