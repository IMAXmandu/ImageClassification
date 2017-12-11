import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.StringTokenizer;

public class LoadXML {
	byte[] data;				// data in XML
	MakeList<Stage> stages;		// Stage list
	int startOffset;
	
	public LoadXML(MakeList<Stage> stages) {
		data = null;
		this.stages = stages;
		startOffset = 0;
	}
	
	
	/*
	 * Load XML
	 */
	public void load(String XMLpath) {
		try {
        	FileInputStream xmlFile = new FileInputStream(XMLpath);
        	// gain speed advantage when using BufferedInputStream
        	BufferedInputStream xmlBuffer = new BufferedInputStream(xmlFile);
        	data = new byte[xmlFile.available()];
        	xmlBuffer.read(data, 0, data.length);
        	if (data != null) {
		    	// stage load
        		if(loadValues() == false) {
        			System.out.println("file load fail");
        		}
	        }
			xmlFile.close();
			xmlBuffer.close();
		} catch (Exception e) {}
	}
	
	
	/*
	 * load XML then create Stage
	 */
	private boolean loadValues() {
		// stages is set the root
	    if(xmlSearchTag("<stages>") == false) {
	    	System.out.println("can't find <stages>");
	        return false;
	    }
	    do {
	    	// find stage
	    	if(xmlSearchTag("<!-- stage") == true) {
	    		Stage stage = new Stage();
	    		if(findTree(stage) == true) {
	    			String value = findValue("stage_threshold", data.length);
	    			if(!"".equals(value)) {
	    				stage.stageThreshold = Double.parseDouble(value);
	    			}	    			
	    			stages.add(stage);
	    			continue;
	    		}else {
	    			System.out.println("can't find tree");
	    			return false;
	    		}
	    	}
	    	startOffset++;
	    }while(startOffset < data.length);
	    if(stages.size() == 0) {
	    	System.out.println("has no stages");
	    	return false;
	    }
	    return true;
	}
		
	
	/*
	 * Find next stage in tree.
	 */
	private boolean findTree(Stage stage) {
		while(startOffset < data.length) {
			// </tree> check
			if((char)data[startOffset] == '<' && (char)data[startOffset+1] == '/' && (char)data[startOffset+2] == 't') {
				String compare = (char)data[startOffset+2] + "" + (char)data[startOffset+3] + "" + (char)data[startOffset+4] + "" + (char)data[startOffset+5] + "";
				if(compare.equals("tree")) {
					break;
				}
			}
			if((char)data[startOffset] == '<' && (char)data[startOffset+1] == '!') {
				String compare = (char)data[startOffset+5] + "" + (char)data[startOffset+6] + "" + (char)data[startOffset+7] + "" + (char)data[startOffset+8] + "" + (char)data[startOffset+9] + "";
   				if(compare.equals("stage")) {
   					break;
   				}
   				else if(compare.equals("tree ")) {
   					Tree tree = new Tree();
   					
   					if(xmlSearchTag("<!-- root") == true) {
   	   					Leaf leaf = new Leaf();
   	   					if(findRect(leaf) == true) {
   	   						saveNodeValue(leaf);
   				    		tree.leafs.add(leaf);
   				    	}else {
   			    			System.out.println("can't find rect");
   			    			return false;
   			    		}
   	   				}else {
   		    			System.out.println("can't find root node");
   		    			return false;
   		    		}
   		    		
   		    		if(findLeaf(tree) == true) {
   		    			stage.trees.add(tree);
   		    			continue;
   		    		}else {
   		    			System.out.println("can't find leaf");
   		    			return false;
   		    		}
   				}
			}
			startOffset++;
		}
		if(stage.trees.size() == 0) {
			System.out.println("has no trees");
			return false;
		}
		return true;
	}
	
	
	/*
	 * Find next leaf in tree
	 */
	private boolean findLeaf(Tree tree) {
		while(startOffset < data.length) {
			if((char)data[startOffset] == '<' && (char)data[startOffset+1] == '!') {
				String compare = (char)data[startOffset+5] + "" + (char)data[startOffset+6] + "" + (char)data[startOffset+7] + "" + (char)data[startOffset+8] + "";
   				if(compare.equals("tree")) {
   					break;
   				}
   				else if(compare.equals("node")) {
   					Leaf leaf = new Leaf();
   					if(findRect(leaf) == true) {
			    		saveNodeValue(leaf);
			    		tree.leafs.add(leaf);
			    	}else {
		    			System.out.println("can't find rect");
		    			return false;
		    		}
   				}
   			}
			// </tree> check
			else if((char)data[startOffset] == '<' && (char)data[startOffset+1] == '/' && (char)data[startOffset+2] == 't') {
				String compare = (char)data[startOffset+2] + "" + (char)data[startOffset+3] + "" + (char)data[startOffset+4] + "" + (char)data[startOffset+5] + "";
   				if(compare.equals("tree")) {
   					break;
   				}
			}
			startOffset++;
		}
		if(tree.leafs.size() == 0) {
			System.out.println("has no leafs");
			return false;
		}
		return true;
	}
	
	private void saveNodeValue(Leaf leaf) {
		int i = startOffset;
		
		while(i < data.length) {
			if((char)data[i] == '<' && (char)data[i+1] == '/' && (char)data[i+2] == '_') {
				break;
			}
			i++;
		}
		String value = "";
		
		value = findValue("tilted", i);
   		if(!"".equals(value)) {
   			if(!value.equals("0")) {
		    		leaf.tilted = true;	
	    		}	
   		}
   		value = findValue("threshold", i);
   		if(!"".equals(value)) {
	    		leaf.threshold = Double.parseDouble(value);
	    	}
   		value = findValue("left_val", i);
   		if(!"".equals(value)) {
	    		leaf.left_val = Double.parseDouble(value);
	    	}
   		value = findValue("left_node", i);
   		if(!"".equals(value)) {
   			if(value.equals("1")) {
		    		leaf.left_node = true;	
	    		}	
   		}
   		value = findValue("right_val", i);
   		if(!"".equals(value)) {
	    		leaf.right_val = Double.parseDouble(value);
	    	}
   		value = findValue("right_node", i);
   		if(!"".equals(value)) {
   			if(value.equals("1")) {
		    		leaf.right_node = true;	
	    		}	
   		}
   		startOffset = i;
	}
	
	private boolean findRect(Leaf leaf) {
		String value = "";
		while(startOffset < data.length) {
			if((char)data[startOffset] == '<') {
				if((char)data[startOffset+1] == '_' && (char)data[startOffset+2] == '>') {
					Rect rect = new Rect();
					// read the value between <_> ~ .</_>
					startOffset = startOffset+3;
					while(startOffset < data.length) {
						if((char)data[startOffset] == '.') {
							break;
						}
						value = value + (char)data[startOffset] + "";
						startOffset++;
					}
					StringTokenizer strTokenizer = new StringTokenizer(value, " ");
					if(strTokenizer.countTokens() == 5) {
						String str = "";
						str = strTokenizer.nextToken();
						rect.x = Integer.parseInt(str);
						str = strTokenizer.nextToken();
						rect.y = Integer.parseInt(str);
						str = strTokenizer.nextToken();
						rect.width = Integer.parseInt(str);
						str = strTokenizer.nextToken();
						rect.height = Integer.parseInt(str);
						str = strTokenizer.nextToken();
						rect.weight = Integer.parseInt(str);

						leaf.rects.add(rect);
					}else {
						System.out.println("can't find value");
		    			return false;
					}
					value = "";
				}
				// </rects> check
				else if((char)data[startOffset+1] == '/') {
					String compare = (char)data[startOffset+2] + "" + (char)data[startOffset+3] + "" + (char)data[startOffset+4] + "" + (char)data[startOffset+5] + "" + (char)data[startOffset+6] + "";
	   				if(compare.equals("rects")) {
	   					break;
	   				}
				}
			}	
			startOffset++;
		}
		if(leaf.rects.size() == 0) {
			System.out.println("has no rects");
			return false;
		}		
		return true;
	}
	
	private boolean xmlSearchTag(String elementName) {
		boolean find = false;
		int i = startOffset;
		int length = elementName.length();
		char firstText = elementName.charAt(0);
		while(i < data.length) {
			if((char)data[i] == firstText) {
		    	for(int j = 1; j < length; j++) {
					if(elementName.charAt(j) != (char)data[i+j]) {
						find = false;
						break;
					} else{
						find = true;
					}
				}
		    	if(find == true) {
		    		startOffset = i + length;
		    		return true;
		    	}
			}
        	i++;
		}
		return false;
	}

	private String findValue(String tag, int end) {
		String returnStr = "";
		boolean find = false;
		int i = startOffset;
		char firstText = tag.charAt(0);
		while(i < end) {
			if((char)data[i] == '<') {
				if((char)data[i+1] == firstText) {
					for(int j = 1; j < tag.length(); j++) {
						if(tag.charAt(j) != (char)data[i+1+j]) {
							find = false;
							break;
						} else{
							find = true;
						}
					}
					if(find == true) {
						i = i + tag.length() + 2;
						while(i < end) {
							if((char)data[i] == '<' && (char)data[i+1] == '/') {
								break;
							}
							returnStr = returnStr + (char)data[i] + "";
							i++;
						}
			    		return returnStr;
			    	}
				}
			}
			i++;
		}
		return returnStr;
	}	
}