package pass;

import backend.CodeGenManager;
import ir.MyModule;
import java.util.ArrayList;
import pass.Pass.IRPass;
import pass.Pass.MCPass;
import pass.ir.Mem2reg;
import pass.mc.RegAllocator;

public class PassManager {

  private static PassManager passManager = new PassManager();
  private ArrayList<String> openedPasses_ = new ArrayList<>() {{
    add("typeCheck");
  }};
  private ArrayList<IRPass> irPasses = new ArrayList<>(){};
  private ArrayList<MCPass> mcPasses = new ArrayList<>();

  private PassManager() {
    //pass执行的顺序在这里决定,如果加了而且是open的，就先加的先跑
    irPasses.add(new Mem2reg());

    mcPasses.add(new RegAllocator());

  }

  public static PassManager getPassManager() {
    return passManager;
  }

  public void addOffedPasses_(String passName) {
    openedPasses_.add(passName);
  }

  //把pass手动加上来
  public void runIRPasses(MyModule m) {
    irPasses.forEach(pass -> {
      if (!openedPasses_.contains(pass.getName())) {
        pass.run(m);
      }
    });

  }

  public void runMCPasses(CodeGenManager cgm) {
    mcPasses.forEach(pass -> {
      if (!openedPasses_.contains(pass.getName())) {
        pass.run(cgm);
      }
    });
  }
}
