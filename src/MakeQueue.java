public class MakeQueue<E> {
	private Node<E> front;
	private Node<E> rear;
	private int size;
	
	public MakeQueue() {
		this.front = null;
		this.rear = null;
		size = 0;
	}
	
	// add node
	public void add(E data) {
		Node<E> newNode = new Node<E>(data);
		if(front == null) {
			front = newNode;
			rear = newNode;
		}else {
			rear.next = newNode;
			rear = newNode;
		}
		size++;
	}
	
	// dequeue
	public E get() {
		E returnVal = front.data;
		if(front.next == null) {
			front = null;
		}else {
			front = front.next;	
		}
		size--;
		return returnVal;
	}
	
	public int size() {
		return size;
	}
}
