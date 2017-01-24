package communication;


/**
 * Created by Mauricio on 18/01/2017.
 */
public class Connection {

    private Long shellPort;

    private Long iopubPort;

    private Long hbPort;

    private Long controlPort;

    private Long stdinPort;

    private String ip;

    private String transport;

    private String key;

    private String kernelName;

    private String signatureScheme;

    public Connection() {
    }

    public Long getShellPort() {
        return shellPort;
    }


    public Long getIoPubPort() {
        return iopubPort;
    }


    public Long getHbPort() {
        return hbPort;
    }


    public Long getControlPort() {
        return controlPort;
    }


    public Long getStdinPort() {
        return stdinPort;
    }


    public String getIp() {
        return ip;
    }


    public String getTransport() {
        return transport;
    }


    public String getKey() {
        return key;
    }


    public String getKernelName() {
        return kernelName;
    }


    public String getSignatureScheme() {
        return signatureScheme;
    }


    public void printConnectionSettings() {
        System.out.println("SHELL PORT: " + this.getShellPort());
        System.out.println("IO pub PORT: " + this.getIoPubPort());
        System.out.println("HB PORT: " + this.getHbPort());
        System.out.println("Control PORT: " + this.getControlPort());
        System.out.println("STDIN PORT: " + this.getStdinPort());
        System.out.println("IP: " + this.getIp());
        System.out.println("Transport: " + this.getTransport());
        System.out.println("key: " + this.getKey());
        System.out.println("kernel name: " + this.getKernelName());
        System.out.println("signature scheme: " + this.getSignatureScheme());
    }
}
