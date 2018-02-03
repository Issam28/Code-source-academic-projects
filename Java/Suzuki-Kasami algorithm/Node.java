public class Node {
	String id;
	String hostname;
	String port;
	Boolean alive;

	public Node(String i, String h, String p,Boolean a) {
		this.id = i;
		this.hostname = h;
		this.port = p;
		this.alive=a;

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void print() {
		System.out.println("");
		System.out.print(" " + this.id + " " + this.hostname + " " + this.port);
	}
}