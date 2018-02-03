
 
import java.awt.Color;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Timer;


//Accepts connections for requests from different nodes

public class MutexReqRec extends Thread {
	private ServerSocket LocServersock;
	private boolean close = false;
	static final int MAX = 1000;
        FileWriter fw;
        File file;
       JButton jButton0;
       JButton jButton1;
       JButton jButton2;

      public JButton getjButton1() {
        return jButton1;
    }

    public void setjButton1(JButton jButton0) {
        this.jButton1 = jButton0;
    }   
    public JButton getjButton2() {
        return jButton2;
    }

    public void setjButton2(JButton jButton0) {
        this.jButton2 = jButton0;
    } 

      public  JButton getjButton0() {
        return jButton0;
    }

    public void setjButton0(JButton jButton0) {
        this.jButton0 = jButton0;
    }

	public MutexReqRec(int port) throws IOException {
		LocServersock = new ServerSocket(port);
		LocServersock.setSoTimeout(10000);
	}

	public void run( ) { 
            
            /*************************** panne panne panne *************************************************/          
		  int delais=8600 ;
		  ActionListener tache_timer ; 
		  /* Action réalisé par le timer */ 
		  tache_timer= new ActionListener()
		  {
		  public void actionPerformed(ActionEvent e1)
		  {
                      boolean b=false,bb=false;int i=0;
                      bb=false;int nombreAleatoire;
                      try {
                          b=isPanne(DistMutEx.my_id);
                      } catch (IOException ex) {
                          Logger.getLogger(MutexReqRec.class.getName()).log(Level.SEVERE, null, ex);
                      }
                      Random rand = new Random();
                        nombreAleatoire = rand.nextInt(100 - 1 + 1) + 1;
                      if ((nombreAleatoire)<20 && !b && DistMutEx.my_id!=0 && !DistMutEx.hastoken) try {
                          {  panne(DistMutEx.my_id,true);
                         bb=true;
                         getjButton0().setBackground(Color.red);
                        // getjLabel5().setBackground(Color.red);
                         getjButton0().setText("En panne !");
                         MutexReqRec.msjlog(DistMutEx.my_id,"\nEn panne");
                          }
                      } catch (IOException ex) {
                          Logger.getLogger(MutexReqRec.class.getName()).log(Level.SEVERE, null, ex);
                      }
                      if(b &&!bb) {
                          try {
                              panne(DistMutEx.my_id,false);
                          } catch (IOException ex) {
                              Logger.getLogger(MutexReqRec.class.getName()).log(Level.SEVERE, null, ex);
                          }
                          getjButton0().setBackground(Color.GREEN);
                          getjButton0().setText("revenu après panne !");
                          try {
                              MutexReqRec.msjlog(DistMutEx.my_id,"\nRevenu après panne");
                          } catch (IOException ex) {
                              Logger.getLogger(MutexReqRec.class.getName()).log(Level.SEVERE, null, ex);
                          }
                      }
                  }
		  };
 		  final Timer timer1= new Timer(delais,tache_timer); 
		  timer1.start();
                  
                 // timer2.start();
            /****************************************************************************************/
              file = new File("out.txt");
            
            try {
                fw = new FileWriter(file.getAbsoluteFile()); 
            } catch (IOException ex) {
                Logger.getLogger(MutexReqRec.class.getName()).log(Level.SEVERE, null, ex);
            }
		 BufferedWriter bw = new BufferedWriter(fw);
		while (!close) {
                 // try {
                    //  if(!close) ){
                          try {
                              
                              Socket server = LocServersock.accept();
                              InputStream is = server.getInputStream();
                              ObjectInputStream ois = new ObjectInputStream(is);
                              Object obj = ois.readObject();
                              // Listen for requests and decide..
                              if(obj instanceof Token){
                                 // this.getjButton0().setText("We got a token"); 
                                  System.out.println("We got a token");
                                  if (!isPanne(DistMutEx.my_id)){
                                      MutexReqRec.msjlog(DistMutEx.my_id,"\nRecevoir (token,jeton)");
                                      getjButton0().setText("Recevoir (token,jeton)");
                                  }
                                  else autresend();
                                  //bw.write("We got a token\n");
                                  DistMutEx.token = (Token)obj;
                                  DistMutEx.hastoken = true;
                                  DistMutEx.tokenInUse = true;
                                  DistMutEx.tokenRecvd = true;
                                  //for (int i=0;i<DistMutEx.token.LN.length;i++)
                                  //{
                                  //  if (DistMutEx.token.LN[i]==MAX) DistMutEx.nodes.get(Integer.toString(i)).alive=false;
                                  //}
                              }
                              else if(obj instanceof Request){
                                  
                                  
                                  Request request = (Request)obj;
                                  //System.out.println("Recieved a Token Request from process P"+request.getId());
                                  if (!isPanne(DistMutEx.my_id)){
                                      MutexReqRec.msjlog(DistMutEx.my_id,"\nRecevoir (req,"+request.reqNo+","+request.getId()+") de P"+request.getId() );
                                      getjButton0().setText("Recevoir (req,"+request.reqNo+","+request.getId()+") de P"+request.getId());
                                  }
                                  
                                  // bw.write("Recieved a Token Request from process P"+request.getId());
                                  
                                  DistMutEx.RN[request.getId()] = (DistMutEx.RN[request.getId()]>request.getReqNo()?
                                          DistMutEx.RN[request.getId()]:
                                          request.getReqNo());
                                  if(DistMutEx.hastoken && !DistMutEx.tokenInUse ){
                                      //Adding the Request from other node to Queue of Token
                                      if(request.getReqNo() > DistMutEx.token.LN[request.getId()] && !DistMutEx.token.getQueue().contains(request.getId()) && DistMutEx.nodes.get(Integer.toString(request.getId())).alive)
                                      {
                                          Request req=(Request) obj;
                                          DistMutEx.token.getQueue().add(req.getId());
                                      }
                                      MutexReqRec.sendToken();
                                      System.out.println("Request added successfully");
                                      
                                  }
                                  
                              }
                              //If the Queue of Token is not empty.
                              
                          } catch (IOException e) { 
                              if (e.getCause() instanceof SocketTimeoutException){
                                  System.out.println("No inbound connections, application terminating");
                              } 
                          } catch (ClassNotFoundException e) {
                              e.printStackTrace();
                          } 
		}
		System.out.println("Application shutting down......."
				+ "\nall connections are terminated ");
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(MutexReqRec.class.getName()).log(Level.SEVERE, null, ex);
            }
		return;
	}
	
	public static synchronized void sendToken() throws IOException{
            boolean b = DistMutEx.token.getQueue().isEmpty();
            int  requestServing = DistMutEx.token.getQueue().poll();
                 if(isPanne(requestServing) )    {
                                              b = DistMutEx.token.getQueue().isEmpty();
                                              requestServing = DistMutEx.token.getQueue().poll(); ;
                 }
		if (DistMutEx.hastoken && !b    ) { 	
			//Sending the Token to the next Request in the queue
			try {
				
                                int i=0;
				Node node = DistMutEx.nodes.get(Integer.toString(requestServing));
				//System.out.println("Sending token to process P" +node.getId());
                               if (!isPanne(DistMutEx.my_id)) MutexReqRec.msjlog(DistMutEx.my_id,"\nEnvoyer (token,jeton) à P" +node.getId() );
                                 
                               // getjButton0().setText("Envoyer (token,jeton) à P"+node.getId());
                                   
				Socket client = new Socket(node.getHostname(), Integer.parseInt(node.getPort()));
				OutputStream os = client.getOutputStream();  
				ObjectOutputStream oos = new ObjectOutputStream(os);  
				oos.writeObject(DistMutEx.token);  
				oos.close();  
				os.close();  
				client.close();
				DistMutEx.hastoken = false;
				DistMutEx.requestMade = false;
				DistMutEx.tokenRecvd = false; 
			}
			catch (IOException e) { 
				//e.printStackTrace();
				//DistMutEx.token.LN[requestServing]=MAX;
				DistMutEx.nodes.get(Integer.toString(requestServing)).alive=false;
				MutexReqRec.retry();
				
			}
		}
	}
        
        public synchronized void autresend() throws IOException{
            try {
				
                                int i=0;
				Node node = DistMutEx.nodes.get(Integer.toString(0));
				//System.out.println("Sending token to process P" +node.getId());
                               if (!isPanne(DistMutEx.my_id)) MutexReqRec.msjlog(DistMutEx.my_id,"\nEnvoyer (token,jeton) à P" +node.getId() );
                                 
                               // getjButton0().setText("Envoyer (token,jeton) à P"+node.getId());
                                   
				Socket client = new Socket(node.getHostname(), Integer.parseInt(node.getPort()));
				OutputStream os = client.getOutputStream();  
				ObjectOutputStream oos = new ObjectOutputStream(os);  
				oos.writeObject(DistMutEx.token);  
				oos.close();  
				os.close();  
				client.close();
				DistMutEx.hastoken = false;
				DistMutEx.requestMade = false;
				DistMutEx.tokenRecvd = false; 
			}
			catch (IOException e) { 
				//e.printStackTrace();
				//DistMutEx.token.LN[requestServing]=MAX;
				DistMutEx.nodes.get(Integer.toString(1)).alive=false;
			        MutexReqRec.retry();
				
			}
        }
        
	static void retry() throws IOException{
		MutexReqRec.sendToken();
	}
	public void closeThread() throws IOException{
		MutexReqRec.sendToken();
		this.close = true; 
		return;
		
	}
        
        
 static void panne(int numsite, boolean b) throws FileNotFoundException, IOException{  
                                      new File("etat"+numsite+"r.txt").delete();
                                      try {
                                         
                                                String nom="etat"+numsite+"r.txt";
						File file = new File(nom); 
						FileWriter fw = new FileWriter(file.getAbsoluteFile(), false);
						BufferedWriter bw = new BufferedWriter(fw); 
						if (b) bw.write("0");
                                                else bw.write("1"); 
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
 
        }
        
         static boolean isPanne(int numsite) throws FileNotFoundException, IOException{ 
                   File file = new File("etat"+numsite+"r.txt"); 
		   FileReader fr = new FileReader (file);
                   char[] buf = {'ù'};
                    fr.read(buf); 			
                    System.out.print(buf);
                   return buf[0]=='0';
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
}
