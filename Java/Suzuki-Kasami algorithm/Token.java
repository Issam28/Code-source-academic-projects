import java.util.LinkedList;
import java.util.Queue;
import java.io.Serializable;

public class Token implements Serializable{
	int[] LN ;
	 Queue<Integer> queue;
	public Token(int size){
		LN =  new int[size];
		for(int i = 0;i<size;i++){
			LN[i] = -1;
		}
		queue = new LinkedList<Integer>();
	}
	
	public int[] getLN() {
		return LN;
	}
	public void setLN(int[] lN) {
		this.LN = lN;
	}
	public  Queue<Integer> getQueue() {
		return this.queue;
	}
	public  void setQueue(Queue<Integer> q) {
		queue = q;
	}
	
}	
