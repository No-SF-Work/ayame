package pass;

import backend.CodeGenManager;
import ir.MyModule;

import java.util.ArrayList;
import java.util.logging.Logger;

import pass.Pass.IRPass;
import pass.Pass.MCPass;
import pass.ir.BBPredSucc;
import pass.ir.BranchOptimization;
import pass.ir.DeadCodeEmit;
import pass.ir.EmitLLVM;
import pass.ir.GVNGCM;
import pass.ir.InterproceduralAnalysis;

import pass.ir.Mem2reg;
import pass.mc.RegAllocator;
import util.Mylogger;

public class PassManager {

  private Logger mylogger = Mylogger.getLogger(PassManager.class);
  private static PassManager passManager = new PassManager();
  private ArrayList<String> openedPasses_ = new ArrayList<>() {{
    //  add("typeCheck");
    add("bbPredSucc");
    add("Mem2reg");
    add("branchOptimization");
    add("emitllvm");
    add("interproceduralAnalysis");
    add("gvngcm");
    add("deadcodeemit");
    add("RegAlloc");
    //  add("ListScheduling");
  }};
  private ArrayList<IRPass> irPasses = new ArrayList<>() {
  };
  private ArrayList<MCPass> mcPasses = new ArrayList<>();

  private PassManager() {
    //pass执行的顺序在这里决定,如果加了而且是open的，就先加的先跑
    irPasses.add(new BBPredSucc());
    irPasses.add(new Mem2reg());
    //irPasses.add(new EmitLLVM());
    irPasses.add(new BranchOptimization());
    irPasses.add(new InterproceduralAnalysis());
    irPasses.add(new GVNGCM());
    irPasses.add(new BranchOptimization());
    irPasses.add(new DeadCodeEmit());
//    irPasses.add(new EmitLLVM());

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
