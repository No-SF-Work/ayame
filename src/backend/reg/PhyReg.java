package backend.reg;

import java.util.HashMap;
import java.util.Objects;

public class PhyReg extends Reg{

    private String name;
    private int idx;

    @Override
    public String getName(){return name;}

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
        nameMap.put(13,"sp");
        nameMap.put(14,"lr");
        nameMap.put(15,"pc");
        nameMap.put(16,"cspr");
    }

    public void setAllocated() {
        isAllocated = true;
    }

    public boolean isAllocated=false;

    public PhyReg(int n){
        super(state.phy);
        this.idx = n;
        this.name=nameMap.get(n);
    }

    public int getIdx() {
        return idx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhyReg phyReg = (PhyReg) o;
        return idx == phyReg.idx && isAllocated == phyReg.isAllocated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idx, isAllocated);
    }
}
