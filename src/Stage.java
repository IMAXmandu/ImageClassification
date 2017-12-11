public class Stage {
	public MakeList<Tree> trees;
	public double stageThreshold;
	
	public Stage() {
		trees = new MakeList<Tree>();
		stageThreshold = 0.0;
	}
}

class Tree {
	public MakeList<Leaf> leafs;
	
	public Tree() {
		leafs = new MakeList<Leaf>();
	}
}

class Leaf {
	public MakeList<Rect> rects;
	public boolean tilted;
	public double threshold;
	public double left_val;
	public double right_val;
	public boolean left_node;
	public boolean right_node;
	public Leaf() {
		rects = new MakeList<Rect>();
		tilted = false;
		threshold = 0.0;
		left_val = 0.0;
		left_node = false;
		right_val = 0.0;
		right_node = false;
	}
}

class Rect {
	public int x;
	public int y;
	public int width;
	public int height;
	public int weight;
	
	public Rect() {
		x = 0;
		y = 0;
		width = 0;
		height = 0;
		weight = 0;
	}
}