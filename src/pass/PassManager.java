package pass;

import ir.MyModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import pass.Pass.IRPass;
import pass.Pass.MAPass;
import util.Mylogger;

public class PassManager {

  private static PassManager passManager = new PassManager();
  private ArrayList<String> offedPasses_ = new ArrayList<>();
  private ArrayList<IRPass> irPasses = new ArrayList<>();
  private ArrayList<MAPass> maPasses = new ArrayList<>();

  private PassManager() {
    //pass执行的顺序在这里决定
    irPasses.add(new DeadCodeEmit());
    //irPasses.add(new opt);

    //todo  maPasses.add(new opt );

  }

  public static PassManager getPassManager() {
    return passManager;
  }

  public void addOffedPasses_(String passName) {
    offedPasses_.add(passName);
  }

  //todo
  //把pass手动加上来
  public void runIRPasses(MyModule m) {
    for (IRPass irPass : irPasses) {
      if (!offedPasses_.contains(irPass.getName())) {
        irPass.run(m);
      }
    }
  }

  //todo
  public void runMAPasses(/*MaModule ma*/) {
    for (MAPass maPass : maPasses) {
      if (!offedPasses_.contains(maPass.getName())) {
        maPass.run();
      }
    }
  }
}
