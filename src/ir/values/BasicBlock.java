package ir.values;

import ir.values.instructions.Instruction;

import java.util.LinkedList;

public class BasicBlock extends Value {
    public void addInstruction(Instruction inst) {
    }

    public BasicBlock create() {
        return null;
    }


    private LinkedList<Instruction> holdInstructions;


}
