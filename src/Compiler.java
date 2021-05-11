import frontend.AntlrTester;

public class Compiler {
    public static void main(String[] args) {
        String treeString = AntlrTester.genTreeString(args);
        System.out.println(treeString);
    }
}
