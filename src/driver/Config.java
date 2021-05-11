package driver;

/***
 * This is the config file of the compiler
 * options' meaning are self-explanatory
 ***/
public class Config {
    private static Config config = new Config();
    public static Config getInstance(){
        return config;
    }
    public boolean isDebugMode=false;
    public boolean recordParse=false;
    private Config() {
    }
}
