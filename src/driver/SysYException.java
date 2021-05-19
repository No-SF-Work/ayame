package driver;

public class SysYException extends RuntimeException {
    /*
     * 这些异常被抛出一次后就会退出程序，所以我让他们static了
     * */
    public SysYException() {
    }

    public static class TypeCheckException extends SysYException {

    }

    public static class PassException extends SysYException {
    }

    public static class AsmWriterException extends SysYException {
    }


}
