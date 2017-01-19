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

    public void setShellPort(Long shellPort) {
        this.shellPort = shellPort;
    }

    public Long getIoPubPort() {
        return iopubPort;
    }

    public void setIoPubPort(Long ioPubPort) {
        this.iopubPort = ioPubPort;
    }

    public Long getHbPort() {
        return hbPort;
    }

    public void setHbPort(Long hb_port) {
        this.hbPort = hb_port;
    }

    public Long getControlPort() {
        return controlPort;
    }

    public void setControlPort(Long controlPort) {
        this.controlPort = controlPort;
    }

    public Long getStdinPort() {
        return stdinPort;
    }

    public void setStdinPort(Long stdinPort) {
        this.stdinPort = stdinPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKernelName() {
        return kernelName;
    }

    public void setKernelName(String kernelName) {
        this.kernelName = kernelName;
    }

    public String getSignatureScheme() {
        return signatureScheme;
    }

    public void setSignatureScheme(String signatureScheme) {
        this.signatureScheme = signatureScheme;
    }
}
