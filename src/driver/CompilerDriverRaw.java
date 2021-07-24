package driver;

import frontend.SysYLexer;
import frontend.SysYParser;
import frontend.Visitor;
import ir.MyModule;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import pass.PassManager;
import backend.CodeGenManager;

import java.io.IOException;
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
      if (source.contains("register_alloc")) {
        pm.openedPasses_.removeIf(s -> s.equals("gvmgcm"));
      }
      pm.runIRPasses(MyModule.getInstance());

      CodeGenManager cgm = CodeGenManager.getInstance();
      cgm.load(MyModule.getInstance());

      cgm.MachineCodeGeneration();
      pm.runMCPasses(CodeGenManager.getInstance());

      File f = new File(target);
      FileWriter fw = new FileWriter(f);
      fw.append(cgm.genARM());
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
