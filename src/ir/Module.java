package ir;

import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.GlobalVariables;
import ir.values.instructions.Instruction;
import ir.values.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Module {
    //因为只对单文件进行编译，所有bb都属于这个module，所以Module事实上是单例的
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

    //所有的运行时存储数据都应该有引用放在这个类中
    private ArrayList<BasicBlock> Blocks;
    private ArrayList<GlobalVariables> globalVariables;
    private ArrayList<Function> functions;
    private LinkedList<Instruction> instructionsTables;
    private HashMap<String, Value> valueMap;
    private static Module module = new Module();

    private Module() {
    }

}
