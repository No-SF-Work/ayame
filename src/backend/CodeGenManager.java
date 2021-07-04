package backend;
import backend.LiveInterval;
import backend.machinecodes.MachineCode;
import backend.machinecodes.MachineFunction;
import backend.reg.VirtualReg;
import ir.MyModule;

import java.util.ArrayList;

/**
 * 后端的顶层模块，管理整个后端的流程，
 */
public class CodeGenManager {

    // all functions
    private ArrayList<MachineFunction> machineFunctionsfs;

    //global virtualregs
    private ArrayList<VirtualReg> globalVirtualRegs;

    //ir moudle
    private static MyModule myModule;

    private static final CodeGenManager codeGenManager = new CodeGenManager(myModule);

    //ir->machinecode
    public static CodeGenManager getInstance(MyModule myModule){
        return codeGenManager;
    }

    private void MachineCodeGeneration(MyModule myModule){

    }

    private CodeGenManager(MyModule myModule){
        MachineCodeGeneration(myModule);
    }
}
