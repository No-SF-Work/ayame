package ir;

import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.GlobalVariables;
import ir.values.instructions.Instruction;
import ir.values.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * IR结构中的顶层container，存有函数，全局变量，符号表，基本块表,指令的表以及其他所有需要的信息 由于SysY只需要支持单文件编译，所以Module事实上是单例存在的
 */
public class Module {

  public static Module getInstance() {
    return module;
  }

  public void addBlock(BasicBlock bb) {
    this.Blocks.add(bb);
  }

  public void addGlobalVariable(GlobalVariables gl) {
    globalVariables.add(gl);
  }

  public void addFunction(Function fn) {
    functions.add(fn);
  }

  public void addInstruction(Instruction inst) {
    instructionsTables.add(inst);
  }

  private ArrayList<BasicBlock> Blocks;
  private ArrayList<GlobalVariables> globalVariables;
  private ArrayList<Function> functions;
  private LinkedList<Instruction> instructionsTables;
  private HashMap<String, Value> valueMap;
  private static Module module = new Module();

  private Module() {
  }

}
