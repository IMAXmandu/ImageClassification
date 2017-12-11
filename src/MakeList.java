public class MakeList<E> {
	private Node<E> head;
	private Node<E> tail;
	private int size;
	
	public MakeList() {
		this.head = null;
		this.tail = null;
		size = 0;
	}
	
	// initialize list
	public void clean() {
		head = null;
		tail = null;
		size = 0;
	}
	
	// add node
	public void add(E data) {
		Node<E> newNode = new Node<E>(data);
		if(head == null) {
			head = newNode;
			tail = head;
		}else {
			tail.next = newNode;
			tail = newNode;
		}
		size++;
	}
	
	public int size() {
		return size;
	}
	
	public E get(int index) {
		Node<E> temp = head;
		for(int i=0; i<index; i++) {
			temp = temp.next;
		}
		return temp.data;
	}
}

class Node<E> {
	public E data;
	public Node<E> next;
	public Node(E data) {
		this.data = data;
		this.next = null;
	}
}
