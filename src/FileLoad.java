import java.io.File;

public class FileLoad {
	private MakeList<String> returnFiles;
	private MakeQueue<String> queue;
	
	public FileLoad() {
		returnFiles = new MakeList<String>();
		queue = new MakeQueue<String>();
	}
	
	/*
	 *  BFS using queue
	 *  faster than DFS
	 */
	public MakeList<String> loading(String path) {
		File[] files = new File(path).listFiles();
		do{
			if(queue.size() > 0) {
				files = new File(queue.get()).listFiles();
			}
			for(int i = 0; i < files.length; i++) {
				// if '.jpg'
				if(files[i].getName().indexOf(".jpg") != -1) {
					returnFiles.add(files[i].getPath());
				}
				// if directory
				else if(files[i].isDirectory() == true) {
					queue.add(files[i].getPath());
				}
			}	
		}while(queue.size() > 0);
		return returnFiles;
	}
}