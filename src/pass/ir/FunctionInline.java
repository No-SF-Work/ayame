package pass.ir;

import ir.MyModule;
import pass.Pass;
import pass.Pass.IRPass;

public class FunctionInline implements IRPass {

  @Override
  public String getName() {
    return "simpleFuncInline";
  }

  @Override
  public void run(MyModule m) {

  }


}
