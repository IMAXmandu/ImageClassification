import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

class UI extends JFrame implements ActionListener {
	private JTextField folderPath;
	private JTextField resultPath;
    private JButton openButton;
    private JButton startButton;
    private JButton resultButton;
    private JButton confirmButton;
    private JCheckBox rain;
    private JCheckBox sea;
    private JCheckBox mountain;
    private JCheckBox human;
    private JFileChooser jfc;
    private JProgressBar bar;
    private Font font;
    private FileLoad files;
    private boolean rainChecked;
    private boolean seaChecked;
    private boolean mtChecked;
    private boolean humanChecked;
    private boolean cancel;
    private MakeList<String> fileList;
    private MakeList<String> resultImages;
    private MakeList<Stage> stages;			// Haar-feature Cascade
    
        
    public UI() {
    	super("Image Classification 1.0");

    	folderPath = new JTextField();
        resultPath = new JTextField();
        openButton = new JButton("Open");
        resultButton = new JButton("Result");
        startButton = new JButton("Start");
        confirmButton = new JButton("Stop");
        rain = new JCheckBox("Rain");
        sea = new JCheckBox("Sea");
        mountain = new JCheckBox("Mountain");
        human = new JCheckBox("Human");
    	jfc = new JFileChooser();
    	bar = new JProgressBar();
        font = new Font("µ¸¿ò",Font.BOLD, 15);
        files = new FileLoad();
        rainChecked = false;
        seaChecked = false;
        mtChecked = false;
        humanChecked = false;
        cancel = false;
        fileList = new MakeList<String>();
        resultImages = new MakeList<String>();
        stages = null;
        
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setVisible(true);
        this.setSize(710, 200);
        this.setResizable(false);
        setLayout(null);
        
        init();
    }
    
    private void init() {
    	add(bar);
    	add(folderPath);
    	add(resultPath);
        add(openButton);
        add(confirmButton);
        add(startButton);
        add(resultButton);

        add(rain);
        add(sea);
        add(mountain);
        add(human);
        
        folderPath.setBounds(10, 10, 500, 40);
        resultPath.setBounds(10, 55, 500, 40);
        openButton.setBounds(520, 10, 80, 40);
        openButton.setFont(font);
        resultButton.setBounds(520, 55, 80, 40);
        resultButton.setFont(font);
        startButton.setBounds(610, 10, 80, 40);
        startButton.setFont(font);
        confirmButton.setBounds(610, 55, 80, 40);
        confirmButton.setFont(font);
        rain.setBounds(10, 105, 60, 20);
        rain.setFont(font);
        sea.setBounds(80, 105, 60, 20);
        sea.setFont(font);
        mountain.setBounds(145, 105, 100, 20);
        mountain.setFont(font);
        human.setBounds(255, 105, 90, 20);
        human.setFont(font);
        bar.setBounds(15, 135, 670, 20);
    	openButton.addActionListener(this);
    	resultButton.addActionListener(this);
    	startButton.addActionListener(this);
    	confirmButton.addActionListener(this);
    	jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);   	
    }
 
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openButton) {
            if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                folderPath.setText(jfc.getSelectedFile().toString());
            }
        }
        else if(e.getSource() == resultButton){
        	if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                resultPath.setText(jfc.getSelectedFile().toString());
            }
        }
        else if(e.getSource() == startButton){
        	cancel = false;
        	rainChecked = rain.isSelected();
        	seaChecked = sea.isSelected();
        	mtChecked = mountain.isSelected();
        	humanChecked = human.isSelected();
        	
        	// check the directory or checkbox is selected
        	if(!"".equals(folderPath.getText()) && !"".equals(resultPath.getText()) && (rainChecked || seaChecked || mtChecked || humanChecked)) {
        		openButton.setEnabled(false);
            	resultButton.setEnabled(false);
            	startButton.setEnabled(false);
            	rain.setEnabled(false);
            	sea.setEnabled(false);
            	mountain.setEnabled(false);
            	human.setEnabled(false);
            	
        		fileList.clean();
            	fileList = files.loading(folderPath.getText());
            	int size = fileList.size();
            	
            	bar.setValue(0);
            	bar.setMaximum(size);
            	MyThread thread = new MyThread(size);
            	thread.start();
            }
        }
        else if(e.getSource() == confirmButton){
        	cancel = true;
        	Result result = new Result(resultPath.getText(), resultImages);
        	result.resultOpen();
        }
    }
    
    class MyThread extends Thread {
    	private int size;			// count of file
    	
    	public MyThread(int size) {
    		this.size = size;
    	}
    	
    	public void run() {
    		if(humanChecked == true && stages == null) {
    			// if it is searching human, load XML and initialize stage
    			stages = new MakeList<Stage>();
    			LoadXML loadXML = new LoadXML(stages);
    			loadXML.load(".//xml//haarcascade_frontalface_alt2.xml");
    		}
    		for(int i=0; i<size; i++){
    			if(cancel == true) {
    				break;
    			}
    			String filePath = fileList.get(i);
                
    			// Start image processing 
                ImageClassification imageClassification = new ImageClassification(rainChecked, mtChecked, seaChecked, humanChecked, filePath, stages);
                
                // save the image path that you want to save
                if(imageClassification.main()) {
                	resultImages.add(fileList.get(i));
                }
                try {
                	bar.setValue(i+1);
    				Thread.sleep(10);
    		    } catch (Exception ex){}
        	}
    		
    		openButton.setEnabled(true);
    		resultButton.setEnabled(true);
        	startButton.setEnabled(true);
        	rain.setEnabled(true);
        	sea.setEnabled(true);
        	mountain.setEnabled(true);
        	human.setEnabled(true);
        }
    }
}


