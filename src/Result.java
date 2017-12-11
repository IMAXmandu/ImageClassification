import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Result {
	private String resultPath;
	private MakeList<String> result;
	
	public Result(String resultPath, MakeList<String> resultImages) {
		this.resultPath = resultPath;
		this.result = resultImages;
	}
	
	
	/*
	 * Copies the resulting images to the specified folder and opens the corresponding folder
	 */
	public void resultOpen()	{ 
		for(int i=0;i<result.size();i++) {
			int index = result.get(i).lastIndexOf("\\");
			String fileName = result.get(i).substring(index); 
			String out = resultPath + "\\" + fileName + "";
			fileCopy(result.get(i), out);
		}
		
      	// Open result directory
   	 	Runtime rt = Runtime.getRuntime();
        try {
        	rt.exec("explorer.exe" + " " + resultPath);
        } catch (Exception ex) {}
	}
	
	private void fileCopy(String inFileName, String outFileName) {
		try {
    		FileInputStream fis = new FileInputStream(inFileName);
    		FileOutputStream fos = new FileOutputStream(outFileName);
    		
            int data = 0;
            while((data=fis.read())!=-1) {
            	fos.write(data);
            }
            fis.close();
            fos.close();        
    	} catch (Exception e) {}
    }    
}
