package ir.values;

import ir.Use;

import java.util.ArrayList;

/**
 * 借鉴的llvm ir 的设计方式，基本上所有变量/常量/表达式/符号都是 Value
 * 原因详情可见 https://www.cnblogs.com/Five100Miles/p/14083814.html
 *
 * */
public class Value {
    public Value(){this.usesList=new ArrayList<>();}

    public void addUse(Use u){this.usesList.add(u);}
    public void killUse(Use u){this.usesList.remove(u);}
    public void replaceAllUseWith(Value v){
        for (Use use : usesList) {
            //do replace
        }
    }
    private ArrayList<Use> usesList;//记录使用这个Value的Use
}
