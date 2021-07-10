package pass;

import ir.MyModule;
import ir.types.Type;
import pass.Pass.IRPass;

public class TypeCheck implements IRPass {

  @Override
  public String getName() {
    return "typeCheck";
  }

  @Override
  public void run(MyModule m) {

  }
}
