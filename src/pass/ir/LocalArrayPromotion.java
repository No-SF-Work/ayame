package pass.ir;

import ir.MyModule;
import pass.Pass.IRPass;

public class LocalArrayPromotion implements IRPass {

  @Override
  public String getName() {
    return "promotion";
  }

  @Override
  public void run(MyModule m) {

  }
}
