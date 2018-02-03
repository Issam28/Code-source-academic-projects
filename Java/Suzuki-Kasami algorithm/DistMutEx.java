 
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import javax.swing.JButton;

public class DistMutEx {
	static HashMap<String, Node> nodes = new HashMap<String, Node>();
	static int my_id;
	static int counter = 0;
	// RN, LN and Queue as required for the algo
	static int[] RN;
	static Token token;
	static boolean hastoken = false;
	static boolean tokenRecvd = false;
	static boolean requestMade = false;
	static boolean tokenInUse = false;
	static boolean exit = false;


	MutexReqRec mutexRecvReqThread ;
	Scanner rfile;
	int num_nodes = 0;
	String rline;
	int valcn = 0, p;

	public DistMutEx(int my_id, JButton jButton1,JButton jButton2,JButton jButton3) {
		this.my_id = my_id;
		// Reading Config file
		try {
                      
			rfile = new Scanner(new File("Config.txt"));
                         
			while (rfile.hasNextLine()) {
				rline = rfile.nextLine();
				if (rline.charAt(0) != '%') {
					// Skipping the first line of input, Which has only the
					// count of the nodes
					if (valcn == 0) {
						num_nodes = Integer.parseInt(rline);
						valcn = 1;
						rline = rfile.nextLine();
					}
					String[] params = rline.split(" ");
					Node temp = new Node(params[0], params[1], params[2],true);
					temp.print();
					nodes.put(params[0], temp);
				}
			}
			// Initializing the Token object
			token = new Token(num_nodes);
			// Initializing the RN array
			RN = new int[DistMutEx.nodes.size()];
			//Initializing RN
			for (int i = 0; i < DistMutEx.nodes.size(); i++) {
				RN[i] = -1;
			}
			// IF the node id is 0, Hold the token for initial
			if (my_id == 0) {
				hastoken = true;
			}
			// Starting MutEx thread to accept requests
			p = Integer.parseInt(nodes.get(Integer.toString(my_id)).port);
			mutexRecvReqThread = new MutexReqRec(p);
                        int f = 0;
                        jButton1.setBackground(Color.green);
                        mutexRecvReqThread.setjButton0(jButton1);
                        mutexRecvReqThread.setjButton1(jButton2);
                        mutexRecvReqThread.setjButton2(jButton3); 
                        mutexRecvReqThread.start();
			rfile.close();
                        
		} catch (IOException e) {
			rfile.close();
			e.printStackTrace();
		}
	}

	public boolean csEnter( JButton jButton1,JButton jButton2,JButton jButton3) throws InterruptedException, IOException {
		
		if (hastoken && token.queue.isEmpty()) {
			tokenInUse = true;
                       // jButton.setBackground(Color.GREEN);
                        Thread.sleep(2000);       
			return hastoken;
		}
		
		else {
			if (hastoken && !token.queue.isEmpty()){
				MutexReqRec.sendToken();
                                jButton3.setText("Jeton Abson !");
                                jButton3.setBackground(Color.red);
			}
			if(!exit)
			{
                            
				// Generate CS request
				requestMade = true;
				counter++;
				// Increment RN of my_id********
				DistMutEx.RN[my_id]++;
				//Generating a new request
				Request request = new Request(my_id, DistMutEx.RN[my_id]);
				// Broadcast request message to all nodes
				for (String node : nodes.keySet()) {
					try {
						if (!nodes.get(node).id.equals(Integer.toString(my_id)) && nodes.get(node).alive ) {
							String hostname = nodes.get(node).hostname;
							int port = Integer.parseInt(nodes.get(node).port);
							 
                                                      if (!isPanne(my_id)) { msjlog(my_id,"\nEnvoyer (req,"+request.reqNo+","+nodes.get(node).id+") à P"+nodes.get(node).id);
                                                        jButton1.setText("Envoyer (req,"+request.reqNo+","+nodes.get(node).id+") à P"+nodes.get(node).id);
                                                      }Thread.sleep(100);
							Socket client = new Socket(hostname, port);
							OutputStream os = client.getOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(os);
							oos.writeObject(request);
							oos.close();
							os.close();
							client.close();
						}
					} catch (IOException e) {
						DistMutEx.nodes.get(node).alive=false;
					//	e.printStackTrace();
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
				}
				int counter = 0;
				while (!tokenRecvd && counter<50) {
					try {
						counter++;
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("waiting for token...");
                                       jButton1.setText("waiting for token...");
				}
				if(tokenRecvd){
                                         
					System.out.println("Token recieved");
                                        jButton3.setBackground(Color.GREEN);
                                        jButton3.setText("Jeton Reçu !");
					tokenInUse = true;
					return tokenRecvd;
				}
				else{
					System.out.println("Request operation timed out: retrying with new cs request");
					DistMutEx.RN[my_id]--;
					return false;
				}
			}
			else{
				System.out.println("Nodes requested for termination");
				return false;
			}
		}
		// wait until Token is recvd
		
	}
        
	public void csLeave(JButton jButton,JButton jButton1) throws IOException {
		tokenInUse = false;
               // jButton.setBackground(Color.BLUE);
		if(requestMade){
			token.LN[my_id] = RN[my_id];
			requestMade = false;
		}

		for(int i=0; i< num_nodes;i++)
		{
			if(i!= this.my_id)
			{
				
				if(!token.getQueue().contains(i) && (RN[i]>token.LN[i]) && (nodes.get(Integer.toString(i)).alive))
				{
					
					token.getQueue().add(i);
				}

			}
		}
		
		//Checking if the queue is not empty
		if (hastoken && !token.getQueue().isEmpty()) {
			MutexReqRec.sendToken();
                      jButton1.setText("Jeton Abson !");
                        jButton1.setBackground(Color.red);
					
		}
	}
		    static void msjlog(int numsite ,String buf ) throws FileNotFoundException, IOException{
                                  try {
                                      String nom="site"+numsite+"r.txt";
						File file = new File(nom); 
						FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
						BufferedWriter bw = new BufferedWriter(fw);
						bw.write(buf);
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
                }
	
	
	public void close() throws IOException{
		if(mutexRecvReqThread!=null)
			System.out.println("Reached maximum number of executions");
		
			mutexRecvReqThread.closeThread();
			//System.out.println("Closing algo thread");
		return;
	}
                 static boolean isPanne(int numsite) throws FileNotFoundException, IOException{ 
                   File file = new File("etat"+numsite+"r.txt"); 
		   FileReader fr = new FileReader (file);
                   char[] buf = {'a'};
                    fr.read(buf); 			
                    System.out.print(buf);
                   return buf[0]=='0';
        }
                 
}
