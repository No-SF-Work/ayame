package pass.ir;

import ir.MyModule;
import ir.Use;
import ir.values.Constant;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.Value;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.Phi;
import ir.values.instructions.TerminatorInst.CallInst;
import pass.Pass.IRPass;
import util.IList.INode;

public class MarkArgs implements IRPass {

  @Override
  public String getName() {
    return "markglobal";
  }

  MyModule m;

  @Override
  public void run(MyModule m) {
    this.m = m;
    for (INode<Function, MyModule> function : m.__functions) {
      var fun = function.getVal();
      var uses = fun.getUsesList();
      for (Use use : uses) {
        if (use.getUser() instanceof CallInst) {
          CallInst call = (CallInst) use.getUser();
          for (int i = 1; i < call.getOperands().size(); i++) {
            var opr = call.getOperands().get(i);
            if (opr instanceof GEPInst) {
              if (((GEPInst) opr).getAimTo() instanceof AllocaInst) {
                fun.getArgList().get(i - 1).setMustBeGlobal(false);
              }
            }
            if (opr instanceof LoadInst) {
              fun.getArgList().get(i - 1).setMustBeGlobal(false);
            }
            if (opr instanceof Constant) {
              fun.getArgList().get(i - 1).setMustBeGlobal(false);
            }
            if (opr instanceof Phi) {
              fun.getArgList().get(i - 1).setMustBeGlobal(false);
            }
          }
        }
      }
    }
  }
}
