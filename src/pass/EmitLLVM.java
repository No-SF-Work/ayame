package pass;

import ir.MyModule;
import pass.Pass.IRPass;

public class EmitLLVM implements IRPass {

  private int vnc = 0;//variable name counter

  @Override
  public String getName() {
    return "emitllvm";
  }

  @Override
  public void run(MyModule m) {
    StringBuilder sb = new StringBuilder(

    );
    m.__globalVariables.forEach(gb -> {

    });
    m.__functions.forEach(func -> {

    });
  }

  private void nameVariable() {

  }

  private void printIR() {

  }

}
