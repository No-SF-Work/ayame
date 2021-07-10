package backend.reg;

import java.util.HashMap;

public class PhyReg extends Reg{

    private String name;

    private static HashMap<Integer,String> nameMap=new HashMap();
    static {
        nameMap.put(0,"r0");
        nameMap.put(1,"r1");
        nameMap.put(2,"r2");
        nameMap.put(3,"r3");
        nameMap.put(4,"r4");
        nameMap.put(5,"r5");
        nameMap.put(6,"r6");
        nameMap.put(7,"r7");
        nameMap.put(8,"r8");
        nameMap.put(9,"r9");
        nameMap.put(10,"r10");
        nameMap.put(11,"r11");
        nameMap.put(12,"r12");
        nameMap.put(13,"r14");
        nameMap.put(14,"r14");
        nameMap.put(15,"r15");
    }
    public PhyReg(String name){
        super(state.phy);
        this.name=name;
    }

    public PhyReg(int n){
        super(state.phy);
        this.name=nameMap.get(n);
    }
}
