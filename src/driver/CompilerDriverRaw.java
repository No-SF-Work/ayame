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

      source = cmd;
    }

    assert source != null;
    assert target != null;

    try {
      Mylogger.init();
      logger = Mylogger.getLogger(CompilerDriverRaw.class);
      CharStream input = CharStreams.fromFileName(source);

      logger.info("Lex begin");
      SysYLexer lexer = new SysYLexer(input);
      lexer.addErrorListener(new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
            int pos, String msg, RecognitionException e) {
          throw new RuntimeException("Lex Error in line: " + line + "pos: " + pos);
        }
      });
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      logger.info("Lex finished");

      logger.info("parse begin");
      SysYParser parser = new SysYParser(tokens);
      parser.setErrorHandler(new BailErrorStrategy());
      ParseTree tree = parser.program();
      logger.info("parse finished");
      logger.info("IR program generating");
      MyModule.getInstance().init();
      Visitor visitor = new Visitor(/* OptionsTable table */);
      visitor.visit(tree);
      logger.info("IR program generated");

      //todo convert to ssa
      pm.runIRPasses(MyModule.getInstance());
      //todo convert to LIR
      if (config.isIRMode) {//做到可以同时使用 -o -f 指令，-o的文件进行底层的优化，-f的文件只进行中高层的优化
        //   pm.addpass(/* llvm ir generate */);
      }
      CodeGenManager cgm = CodeGenManager.getInstance();
      cgm.load(MyModule.getInstance());
      cgm.MachineCodeGeneration();
      pm.runMCPasses(CodeGenManager.getInstance());
      //todo output

      File f = new File(target);
      FileWriter fw = new FileWriter(f);
      fw.append(cgm.genARM());
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
