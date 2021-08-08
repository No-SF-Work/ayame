package pass;

import backend.CodeGenManager;
import ir.MyModule;
import pass.Pass.IRPass;
import pass.Pass.MCPass;
import pass.ir.*;
import pass.mc.CondExec;
import pass.mc.ListScheduling;
import pass.mc.PeepholeOptimization;
import pass.mc.RegAllocator;
import util.Mylogger;

import java.util.ArrayList;
import java.util.logging.Logger;

public class PassManager {

  private Logger mylogger = Mylogger.getLogger(PassManager.class);
  private static PassManager passManager = new PassManager();
  public ArrayList<String> openedPasses_ = new ArrayList<>();
  private ArrayList<IRPass> irPasses = new ArrayList<>();
  private ArrayList<MCPass> mcPasses = new ArrayList<>();

  private PassManager() {
    //pass执行的顺序在这里决定,如果加了而且是open的，就先加的先跑
    irPasses.add(new BBPredSucc());
//        irPasses.add(new EmitLLVM("tt.ll"));
    irPasses.add(new InterproceduralAnalysis());
    irPasses.add(new GlobalVariableLocalize());
    irPasses.add(new Mem2reg());
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());

    irPasses.add(new FunctionInline());
    irPasses.add(new MarkConstantArray());
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());
    irPasses.add(new DeadCodeEmit());

    irPasses.add(new LoopInfoFullAnalysis());
    irPasses.add(new EmitLLVM("beforeLCSSA.ll"));
    irPasses.add(new LCSSA());
    irPasses.add(new EmitLLVM("beforeUnroll.ll"));
    irPasses.add(new LoopUnroll());
    irPasses.add(new LoopInfoFullAnalysis());
    irPasses.add(new EmitLLVM("afterUnroll.ll"));
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());
    irPasses.add(new DeadCodeEmit());
    irPasses.add(new EmitLLVM());

    mcPasses.add(new RegAllocator());
    mcPasses.add(new PeepholeOptimization());
    mcPasses.add(new ListScheduling());
    mcPasses.add(new PeepholeOptimization());
    mcPasses.add(new CondExec());
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
      if (openedPasses_.contains(pass.getName())) {
        mylogger.info("running pass :" + pass.getName());
        pass.run(m);
      }
    });

  }

  public void runMCPasses(CodeGenManager cgm) {
    mcPasses.forEach(pass -> {
      if (openedPasses_.contains(pass.getName())) {
        mylogger.info("running pass :" + pass.getName());
        pass.run(cgm);
      }
    });
  }
}
