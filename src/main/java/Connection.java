/**
 * Created by Mauricio on 18/01/2017.
 */
public class Connection {

    private Long shell_port;

    private Long iopub_port;

    private Long hb_port;

    private Long control_port;

    private Long stdin_port;

    private String ip;

    private String transport;

    private String key;

    private String kernel_name;

    private String signature_scheme;

    public Connection() {

    }

    public Long getShellPort() {
        return shell_port;
    }

    public void setShellPort(Long shellPort) {
        this.shell_port = shellPort;
    }

    public Long getIoPubPort() {
        return iopub_port;
    }

    public void setIoPubPort(Long ioPubPort) {
        this.iopub_port = ioPubPort;
    }

    public Long getHbPort() {
        return hb_port;
    }

    public void setHbPort(Long hb_port) {
        this.hb_port = hb_port;
    }

    public Long getControlPort() {
        return control_port;
    }

    public void setControlPort(Long controlPort) {
        this.control_port = controlPort;
    }

    public Long getStdinPort() {
        return stdin_port;
    }

    public void setStdinPort(Long stdinPort) {
        this.stdin_port = stdinPort;
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
        return kernel_name;
    }

    public void setKernelName(String kernelName) {
        this.kernel_name = kernelName;
    }

    public String getSignatureScheme() {
        return signature_scheme;
    }

    public void setSignatureScheme(String signatureScheme) {
        this.signature_scheme = signatureScheme;
    }
}
