package communication;


public class Connection {

	// -----------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------

	private Long shell;

	private Long IOPub;

	private Long hbPort;

	private Long control;

	private Long stdin;

	private String ip;

	private String transport;

	private String key;

	private String kernelName;

	private String signatureScheme;

	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------

	public Connection() {
	}

	// -----------------------------------------------------------------
	// Methods
	// -----------------------------------------------------------------

	public Long getShellPort() {
		return shell;
	}


	public Long getIOPubPort() {
		return IOPub;
	}


	public Long getHbPort() {
		return hbPort;
	}


	public Long getControlPort() {
		return control;
	}


	public Long getStdinPort() {
		return stdin;
	}

	public String getShellURI() {
		return toUri(shell);
	}


	public String getIOPubURI() {
		return toUri(IOPub);
	}


	public String getHbURI() {
		return toUri(hbPort);
	}


	public String getControlURI() {
		return toUri(control);
	}


	public String getStdinURI() {
		return toUri(stdin);
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

	private String toUri(Long pPort) {
		return String.format("%s://%s:%d", getTransport(), getIp(), pPort);
	}


	public void printConnectionSettings() {
		System.out.println("SHELL PORT: " + this.getShellPort());
		System.out.println("IO pub PORT: " + this.getIOPubPort());
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
