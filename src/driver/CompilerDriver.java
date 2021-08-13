package driver;

import backend.CodeGenManager;
import frontend.SysYLexer;
import frontend.SysYParser;
import frontend.Visitor;
import ir.MyModule;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import pass.PassManager;
import util.Mylogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * 编译器驱动部分
 */
public class CompilerDriver {

  /**
   * @param args:从命令行未处理直接传过来的参数
   */
  private static Logger logger;

  public static void setConfig(Config config, Namespace res) throws IOException {
    config.isIRMode = res.get("emit");
    config.isDebugMode = res.get("debug");
    config.isOutPutMode = res.get("output");
    config.isO2 = true;
    //only severe level msg will be recorded in console if not in debug mode
    Mylogger.loadLogConfig(config.isDebugMode);
  }

  public static void run(String[] args) {
    Mylogger.init();
    Config config = Config.getInstance();
    PassManager pm = PassManager.getPassManager();
    ArgumentParser argParser =
        ArgumentParsers.newFor("Compiler").prefixChars("-+").build()
            .description("A compiler for language SysY2021");
    argParser.addArgument("-S", "--source").required(true).action(Arguments.storeTrue());
    argParser.addArgument("-o", "--output").action(Arguments.storeTrue())
        .help("write asm to output-file");
    argParser.addArgument("-e", "--emit").action(Arguments.storeTrue())
        .help("write llvm ir to debug_{$source file name}.ll");

    argParser.addArgument("-d", "--debug").action(Arguments.storeTrue())
        .help("use debug mode,which will record actions in current path");
    argParser.addArgument("-O2").action(Arguments.storeTrue());
    //新的可选指令加在上面
    argParser.addArgument("source");
    argParser.addArgument("target");
    try {
      Namespace res = argParser.parseArgs(args);
      Mylogger.init();
      setConfig(config, res);
      logger = Mylogger.getLogger(CompilerDriver.class);
      logger.warning("Config -> " + res);
      String source = res.get("source");
      String target = res.get("target");
      CharStream input = CharStreams.fromFileName(source);

      logger.info("Lexing");
      SysYLexer lexer = new SysYLexer(input);
      lexer.addErrorListener(new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
            int pos, String msg, RecognitionException e) {
          throw new RuntimeException("Lex Error in line: " + line + "pos: " + pos);
        }
      });
      CommonTokenStream tokens = new CommonTokenStream(lexer);

      SysYParser parser = new SysYParser(tokens);
      parser.setErrorHandler(new BailErrorStrategy());
      ParseTree tree = parser.program();
      MyModule.getInstance().init();
      logger.info("generating MIR");
      Visitor visitor = new Visitor(/* OptionsTable table */);
      visitor.visit(tree);

      //Driver只用来自测,强制开了
      config.isO2 = true;
      pm.openedPasses_.add("bbPredSucc");
      pm.openedPasses_.add("interproceduralAnalysis");
      pm.openedPasses_.add("Mem2reg");
      pm.openedPasses_.add("RegAlloc");
      if (Config.getInstance().isO2) {
        pm.openedPasses_.add("gvlocalize");
        pm.openedPasses_.add("branchOptimization");
        pm.openedPasses_.add("emitllvm");
        pm.openedPasses_.add("gvngcm");
        pm.openedPasses_.add("deadcodeemit");
        pm.openedPasses_.add("funcinline");
        pm.openedPasses_.add("markConstantArray");
        pm.openedPasses_.add("ListScheduling");
        pm.openedPasses_.add("Peephole");
        pm.openedPasses_.add("CondExec");
        pm.openedPasses_.add("loopInfoFullAnalysis");
        pm.openedPasses_.add("LCSSA");
        pm.openedPasses_.add("loopUnroll");
        pm.openedPasses_.add("constantLoopUnroll");
        pm.openedPasses_.add("MergeMachineBlock");
        pm.openedPasses_.add("redundantLoop");
        pm.openedPasses_.add("loopIdiom");
        pm.openedPasses_.add("loopMergeLastBreak");
        pm.openedPasses_.add("promotion");
      }

      logger.info("running MIR passes");
      pm.runIRPasses(MyModule.getInstance());
      if (config.isIRMode) {
        return;
      }
      CodeGenManager cgm = CodeGenManager.getInstance();
      cgm.load(MyModule.getInstance());
      logger.info("generating MachineCode");
      cgm.MachineCodeGeneration();
      logger.info("running MC passes");
      pm.runMCPasses(CodeGenManager.getInstance());
      if (config.isOutPutMode) {
        File f = new File(target);
        FileWriter fw = new FileWriter(f);
        fw.append(cgm.genARM());
        fw.close();
      }
    } catch (HelpScreenException e) {
      //当使用 -h指令时抛出该异常，catch后直接退出。
    } catch (ArgumentParserException e) {
      System.err.println("ArgParsing error,use \"-h\" for more information ");
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
