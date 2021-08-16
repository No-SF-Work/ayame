package driver;

import backend.CodeGenManager;
import frontend.SysYLexer;
import frontend.SysYParser;
import frontend.Visitor;
import ir.MyModule;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import pass.PassManager;
import util.Mylogger;


/**
 * 编译器驱动部分
 */
public class CompilerDriverRaw {

  /**
   * @param args:从命令行未处理直接传过来的参数
   */
  private static Logger logger;

  public static void run(String[] args) {
    Mylogger.init();
    Config config = Config.getInstance();
    PassManager pm = PassManager.getPassManager();

    var cmds = Arrays.asList(args);
    String source = null;
    String target = null;

    var iter = cmds.iterator();
    iter.next(); // ignore [compiler]
    while (iter.hasNext()) {
      String cmd = iter.next();
      if (cmd.equals("-o")) {
        target = iter.next();
        continue;
      }
      if (cmd.equals("-O2")) {
        Config.getInstance().isO2 = true;
      }
      if (cmd.endsWith(".sy")) {
        source = cmd;
      }
    }

    assert source != null;
    assert target != null;

    try {
      Mylogger.init();
      CharStream input = CharStreams.fromFileName(source);

      SysYLexer lexer = new SysYLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);

      SysYParser parser = new SysYParser(tokens);
      ParseTree tree = parser.program();

      MyModule.getInstance().init();
      Visitor visitor = new Visitor(/* OptionsTable table */);
      visitor.visit(tree);
      pm.openedPasses_.add("bbPredSucc");
      pm.openedPasses_.add("Mem2reg");
      pm.openedPasses_.add("RegAlloc");
      pm.openedPasses_.add("gvngcm");
      pm.openedPasses_.add("interproceduralAnalysis");
      if (Config.getInstance().isO2) {
        pm.openedPasses_.add("gvlocalize");
        pm.openedPasses_.add("branchOptimization");
        pm.openedPasses_.add("emitllvm");
        pm.openedPasses_.add("deadcodeemit");
        pm.openedPasses_.add("funcinline");
        pm.openedPasses_.add("interproceduraldce");
        pm.openedPasses_.add("markConstantArray");
        pm.openedPasses_.add("ListScheduling");
//        pm.openedPasses_.add("Peephole");
        pm.openedPasses_.add("CondExec");
        pm.openedPasses_.add("loopInfoFullAnalysis");
        pm.openedPasses_.add("LCSSA");
        pm.openedPasses_.add("loopUnroll");
        pm.openedPasses_.add("constantLoopUnroll");
        pm.openedPasses_.add("MergeMachineBlock");
        pm.openedPasses_.add("redundantLoop");
//        pm.openedPasses_.add("loopIdiom");
        pm.openedPasses_.add("loopMergeLastBreak");
        pm.openedPasses_.add("promotion");
//        pm.openedPasses_.add("loopFusion");
      }

      if (source.contains("register_alloc")) {
        pm.openedPasses_.removeIf(s -> s.equals("gvngcm"));
      }

      pm.runIRPasses(MyModule.getInstance());

      CodeGenManager cgm = CodeGenManager.getInstance();
      cgm.load(MyModule.getInstance());

      cgm.MachineCodeGeneration();
      pm.runMCPasses(CodeGenManager.getInstance());

      File f = new File(target);
      FileWriter fw = new FileWriter(f);
      fw.append(cgm.genARM());
      fw.append("@ver: gkfdshfjdshf");
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
