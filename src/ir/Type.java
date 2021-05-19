package ir;

public class Type {
    public boolean isLeft;
    public String name;

    public Type(String name) {//默认是右值
        this.name = name;
        this.isLeft = false;
    }

    public Type(String name, boolean type) {
        this.name = name;
        this.isLeft = type;
    }


    public class IntType extends Type {

        public IntType(String name) {
            super(name);
        }

        public IntType(String name, boolean type) {
            super(name, type);
        }
    }

    public class VoidType extends Type {

        public VoidType(String name) {
            super(name);
        }

        public VoidType(String name, boolean type) {
            super(name, type);
        }
    }
}
