package backend.reg;

import backend.machinecodes.MachineCode;

import java.util.HashMap;
import java.util.Objects;

public class VirtualReg extends Reg {

    //是否是由SSA指令定义的
    private boolean isSSA=false;

    private static int Name = 0;

    public boolean isGlobal() {
        return isGlobal;
    }

    private static final int inf=10;

    public int getCost() {
        return cost;
    }


    public MachineCode getDefMC() {
        return defMC;
    }

    //该虚拟寄存器被多次定义
    private boolean isMultDef=false;

    //该指令不可被重新计算
    private boolean isUnMoveable=false;

    public void setDef(MachineCode defMC, int cost) {
     //   assert(defMC!=null);
        if(this.defMC==null && !isMultDef && !isUnMoveable){
            this.defMC = defMC;
            this.cost=cost;
        }else{
            this.defMC=null;
            this.isMultDef=true;
            this.cost=inf;
        }

    }

    public void setCost(int cost){
        this.cost=cost;
    }

    public void setUnMoveable(){
        this.isUnMoveable=true;
        this.cost=inf;
    }

    //计算出此VirgralReg需要花费的周期数
    private int cost=-1;

    //定义
    private MachineCode defMC=null;



    private boolean isGlobal=false;

    private String name;

    public void setSSA(){
        this.isSSA=true;
    }


    boolean isSSA(){
        return isSSA;
    }

    @Override
    public String getName() {
        return name;
    }

    //该virtualreg在某条指令中被使用的时候被分配的物理寄存器，如果没被分配则usemap中不存在该key
    private HashMap<MachineCode,state> useMap=new HashMap<>();

    public VirtualReg(String name){
        super(state.virtual);
        if(name==""){
            this.name="%%"+Integer.toString(Name++);
        }else{
            this.name=name;
        }
    }

    public VirtualReg(){
        super(state.virtual);
        this.name="$"+Integer.toString(Name++);
    }

    public VirtualReg(String name, boolean isGlobal){
        super(state.virtual);
        if(name.startsWith("@")){
            int l=name.lastIndexOf('@');
            this.name=name.substring(l+1,name.length());
        }else{
            this.name=name;
        }
//        this.idx = Integer.parseInt(name.substring(2));
        this.isGlobal=isGlobal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualReg that = (VirtualReg) o;
        return isSSA == that.isSSA && isGlobal == that.isGlobal && Objects.equals(name, that.name) && Objects.equals(useMap, that.useMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSSA, isGlobal, name, useMap);
    }
}
