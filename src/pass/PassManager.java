package pass;

import backend.CodeGenManager;
import ir.MyModule;
import pass.Pass.IRPass;
import pass.Pass.MCPass;
import pass.ir.*;
import pass.mc.MergeMachineBlock;
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
    irPasses.add(new BBPredSucc());
//        irPasses.add(new EmitLLVM("tt.ll"));
    irPasses.add(new InterproceduralAnalysis());
    irPasses.add(new GlobalVariableLocalize());
    irPasses.add(new Mem2reg());
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());

    irPasses.add(new LCSSA());
//    irPasses.add(new EmitLLVM("beforeLoopIdiom.ll"));
    irPasses.add(new LoopIdiom());
//    irPasses.add(new EmitLLVM("afterLoopIdiom.ll"));
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());

    irPasses.add(new LCSSA());
    irPasses.add(new ConstantLoopUnroll());
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());

    irPasses.add(new LCSSA());
//    irPasses.add(new EmitLLVM("beforeUnroll.ll"));
    irPasses.add(new LoopUnroll());
//    irPasses.add(new EmitLLVM("afterUnroll.ll"));
    irPasses.add(new InterProceduralDCE());
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());

    irPasses.add(new LCSSA());
//    irPasses.add(new EmitLLVM("beforeTwiceUnroll.ll"));
    irPasses.add(new LoopUnroll());
//    irPasses.add(new EmitLLVM("afterTwiceUnroll.ll"));
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());

    irPasses.add(new FunctionInline());
    irPasses.add(new InterProceduralDCE());

    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM());
    irPasses.add(new LocalArrayPromotion());
    irPasses.add(new GVNGCM());

    irPasses.add(new LCSSA());
//    irPasses.add(new EmitLLVM("beforeRedundant.ll"));
    irPasses.add(new RedundantLoop());
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM(true));

    irPasses.add(new InterProceduralDCE());
    irPasses.add(new LCSSA());
//    irPasses.add(new EmitLLVM("beforeMerge.ll"));
    irPasses.add(new LoopMergeLastBreak());
//    irPasses.add(new EmitLLVM("afterMerge.ll"));
    irPasses.add(new BranchOptimization());
    irPasses.add(new GVNGCM(true));

    irPasses.add(new LoopInfoFullAnalysis());
    irPasses.add(new EmitLLVM());

    mcPasses.add(new RegAllocator());
    mcPasses.add(new PeepholeOptimization());
    mcPasses.add(new MergeMachineBlock());
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
