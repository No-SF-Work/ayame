package ir.values;

import ir.Type;
import ir.Use;

import java.util.ArrayList;

/**
 * 借鉴的llvm ir 的设计方式，基本上所有变量/常量/表达式/符号都是 Value
 * 原因详情可见 https://www.cnblogs.com/Five100Miles/p/14083814.html
 */
public abstract class Value {


    public Value(Type type) {this.type = type;}

    public void setName(String name) { this.name = name; }

    public String getName() { return name; }

    public void addUse(Use u) {this.usesList.add(u);}

    public void killUse(Use u) {this.usesList.remove(u);}

    public void replaceAllUseWith(Value v) {
        for (Use use : usesList) {
            //todo replace
        }
    }

    public void deleteValue() {
        //todo 删除一个value
    }

    public Type getType() { return type; }//所有的Value都指明了一个Type的

    private Value parent;
    private ArrayList<Use> usesList;//记录使用这个Value的Use
    private String name;
    private final Type type;
}
