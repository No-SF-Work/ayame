package backend.machinecodes;

import backend.CodeGenManager;
import backend.reg.MachineOperand;
import backend.reg.PhyReg;
import backend.reg.Reg;
import backend.reg.VirtualReg;
import ir.values.instructions.Instruction;
import util.IList;
import util.IList.INode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class MachineFunction {

    public void insertBlock(MachineBlock mb){
        mb.getNode().insertAtEnd(mbList);
//        if(mbList.getNumNode()==0) {
//            mbNode.insertAtEntry(mbList);
//        } else {
//            mbNode.insertAfter(mbList.getLast());
//        }
    }

    //使基本块线性化，存储在mbList中
//    public void serializeBlocks(){}

    //get prev and next
    private INode<MachineFunction, CodeGenManager> node;

    public int getStackSize(){
        return stackSize;
    }

    public void addStackSize(int n){
        stackSize+=n;
    }

    //basic block list
    private IList<MachineBlock,MachineFunction> mbList = new IList<>(this);

    public IList<MachineBlock,MachineFunction> getmbList(){return mbList;}

    private ArrayList<PhyReg> phyRegs=new ArrayList<>();

    //all the virtual regs in this function
    private HashMap<String, VirtualReg> regMap=new HashMap<>();

    public void addVirtualReg(VirtualReg vr){
        regMap.put(vr.getName(),vr);
    }

    public HashMap<String,VirtualReg> getVRegMap(){return regMap;}

    public ArrayList<MCMove> getArgMoves() {
        return argMoves;
    }

    private ArrayList<MCMove> argMoves=new ArrayList<>();

    //PhyReg nums
    private int regNums=0;

    public boolean isUsedLr() {
        return usedLr;
    }

    public void setUsedLr(boolean usedLr) {
        this.usedLr = usedLr;
    }

    private boolean usedLr=false;

    //size of stack allocated for virtual register
    private int stackSize = 0;

    private CodeGenManager cgm;

    public ArrayList<PhyReg> getUsedSavedRegs() {
        return usedSavedRegs;
    }

    private ArrayList<PhyReg> usedSavedRegs=new ArrayList<>();

    public PhyReg getPhyReg(String name){
        return new PhyReg(regNameMap.get(name));
    }

    public PhyReg getAllocatedReg(String name){
        PhyReg r=new PhyReg(regNameMap.get(name));
        r.setAllocated();
        return r;
    }

    public PhyReg getAllocatedReg(int n){
        PhyReg r=new PhyReg(n);
        r.setAllocated();
        return r;
    }

    public PhyReg getPhyReg(int n){
        return new PhyReg(n);
    }

    private static HashMap<String,Integer>regNameMap=new HashMap<>();
    static {
        regNameMap.put("r0",0);
        regNameMap.put("r1",1);
        regNameMap.put("r2",2);
        regNameMap.put("r3",3);
        regNameMap.put("r4",4);
        regNameMap.put("r5",5);
        regNameMap.put("r6",6);
        regNameMap.put("r7",7);
        regNameMap.put("r8",8);
        regNameMap.put("r9",9);
        regNameMap.put("r10",10);
        regNameMap.put("r11",11);
        regNameMap.put("r12",12);
        regNameMap.put("r13",13);
        regNameMap.put("r14",14);
        regNameMap.put("r15",15);
        regNameMap.put("fp",11);
        regNameMap.put("ip",12);
        regNameMap.put("sp",13);
        regNameMap.put("lr",14);
        regNameMap.put("pc",15);
    }

    private String name;

    public String getName(){return name;}

    public MachineFunction(CodeGenManager cgm,String name){
        this.cgm=cgm;
        this.name=name;
    }

    public HashSet<Integer> getUsedRegIdxs() {
        var ret = new HashSet<Integer>();
        for (var blockEntry : getmbList()) {
            var block = blockEntry.getVal();

            for (var instrEntry : block.getmclist()) {
                var instr = instrEntry.getVal();
                var defs = instr.getDef();
                var uses = instr.getUse();
//                assert defs.stream().allMatch(x -> x instanceof PhyReg);
//                assert uses.stream().allMatch(x -> x instanceof PhyReg);
                ret.addAll(defs.stream()
                        .filter(PhyReg.class::isInstance)
                        .map(x -> ((PhyReg) x).getIdx())
                        .collect(Collectors.toCollection(HashSet::new)));
                ret.addAll(uses.stream()
                        .filter(PhyReg.class::isInstance)
                        .map(x -> ((PhyReg) x).getIdx())
                        .collect(Collectors.toCollection(HashSet::new)));
            }
        }
        return ret;
    }
}
