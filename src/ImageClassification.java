import static org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_highgui.cvDestroyWindow;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_highgui.cvShowImage;
import static org.bytedeco.javacpp.opencv_highgui.cvWaitKey;

public class ImageClassification {
	private ImageProcessing imgProcessing;	// pre-process
	private IplImage original;				
	private IplImage img;					
	private IplImage gray;					
	private IplImage binary;				
	private String  filePath;				
	private MakeList<Stage> stages;			// Haar-feature Cascade 
	private boolean doSharpen;				
	private boolean doSobel;				
	private boolean rain;
	private boolean mountain;
	private boolean sea;
	private boolean human;
	private boolean isSave;
	
	public ImageClassification(boolean rainChecked, boolean mtChecked, boolean seaChecked, boolean humanChecked, String path, MakeList<Stage> stages) {
		this.rain = rainChecked;
		this.mountain = mtChecked;
		this.sea = seaChecked;
		this.human = humanChecked;
		this.filePath = path;
		this.stages = stages;
		this.isSave = false;
	}
	
	private void init() {
		imgProcessing = new ImageProcessing();
		original = cvLoadImage(filePath);
		if(original == null) {
			System.out.println("Image File Load Error");
			return;
		}
		img = imgProcessing.reSize(original,img);	
		doSobel = false;
		doSharpen = false;
	}
	
	public boolean main() {
		init();
		if(img == null) {
			System.out.println("Image resize Error");
			return false;
		}
		if(rain == true) {
			isRain();
		}
		if(sea == true) {
			isSea();
		}
		if(mountain == true) {
			isMountain();
		}
		if(human == true) {
			isHuman();
		}
		
		// Release Memory
		if(binary != null) {
			cvReleaseImage(binary);
			binary = null;
		}
		if(gray != null) {
			cvReleaseImage(gray);
			gray = null;
		}
		cvReleaseImage(img);
		img = null;
		cvReleaseImage(original);
		original = null;
		doSharpen = false;
		doSobel = false;
		
		return isSave;
	}
	
	
	/*
	 * The image of rain shows difference between length edge and width edge by RAIN_VAL_TH at the value above a certain Threshold.
	 * Between horizontal edge and vertical edge in the rain image have difference which is more than RAIN_VAL_TH
	 * convert image to gray scale, sharpening and binarization.
	 * calculate edge count
	 */
	private void isRain() {
		if(gray == null) {
			gray = imgProcessing.gray(img, gray);	
		}
		if(doSharpen == false) {
			imgProcessing.sharpening(gray, gray);
		}
		if(binary == null) {
			binary = imgProcessing.binarization(gray, binary, 5);	
		}
		if(doSobel == false) {
			imgProcessing.sobel(binary);
			doSobel = true;
		}
		if(imgProcessing.horEgdeCnt != 0) {
			if((double)imgProcessing.verEgdeCnt / (double)imgProcessing.horEgdeCnt > imgProcessing.RAIN_VAL_TH)	{
				cvShowImage("img", original);
				int key = cvWaitKey(0);
				// press s , S to save
				if(key == 83 || key == 115) {
					isSave = true;
				}
				cvDestroyWindow("img");
			}
		}
	}
	
	
	/*
	 * The image of sea shows difference between length edge and width edge by SEA_VAL_TH at the value above a certain Threshold.
	 * convert image to gray scale, sharpening and binarization.
	 * calculate edge counts
	 */
	private void isSea() {
		if(gray == null) {
			gray = imgProcessing.gray(img, gray);	
		}
		if(doSharpen == false) {
			imgProcessing.sharpening(gray, gray);
		}
		if(binary == null) {
			binary = imgProcessing.binarization(gray, binary, 5);	
		}
		if(doSobel == false) {
			imgProcessing.sobel(binary);
			doSobel = true;
		}
		if(imgProcessing.verEgdeCnt != 0) {
			if((double)imgProcessing.horEgdeCnt / (double)imgProcessing.verEgdeCnt > imgProcessing.SEA_VAL_TH) {
				cvShowImage("img", original);
				int key = cvWaitKey(0);
				// press s , S to save
				if(key == 83 || key == 115) {
					isSave = true;
				}
				cvDestroyWindow("img");
			}
		}
	}
	
	
	/*
	 * In the mountain image, the included angle between the ridges is DEGREE_MIN_TH < еш < DEGREE_MAX_TH. 
	 * first, remove sky. and convert image to gray scale, binarization.
	 * to get foreground mountain image, perform morphology(one dilate, two erode)
	 * find peak in that image, create a vector in both ridge directions and then calculate the dot product between the two vectors
	 */
	private void isMountain() {
		imgProcessing.colorRemoval(img);
		if(gray == null) {
			gray = imgProcessing.gray(img, gray);	
		}
		if(binary == null) {
			binary = imgProcessing.binarization(gray, binary, 20);	
		}
		imgProcessing.dilate(binary, binary);
		imgProcessing.erode(binary, binary);
		imgProcessing.erode(binary, binary);
		imgProcessing.innerProduct(binary);
		
		if((imgProcessing.A_length > imgProcessing.VECTOR_MIN_TH && imgProcessing.B_length > imgProcessing.VECTOR_MIN_TH) &&
			 imgProcessing.degree > imgProcessing.DEGREE_MIN_TH && imgProcessing.degree < imgProcessing.DEGREE_MAX_TH) {
			cvShowImage("img", original);
			int key = cvWaitKey(0);
			// press s , S to save
			if(key == 83 || key == 115) {
				isSave = true;
			}
			cvDestroyWindow("img");
		}
		else if(((imgProcessing.A_length > 0 && imgProcessing.B_length > 169) || (imgProcessing.B_length > 0 && imgProcessing.A_length > 169)) &&
				 imgProcessing.degree > imgProcessing.DEGREE_MIN_TH && imgProcessing.degree < imgProcessing.DEGREE_MAX_TH) {
			cvShowImage("img", original);
			int key = cvWaitKey(0);
			// press s , S to save
			if(key == 83 || key == 115) {
				isSave = true;
			}
			cvDestroyWindow("img");
		}
	}
	
	
	/*
	 * convert image to gray scale and find faces using previously learned information.
	 * I was not interested in the number of humans, so I found out that once I found it, I immediately returned it and speeded up. 
	 */
	private void isHuman() {
		if(gray == null) {
			gray = imgProcessing.gray(img, gray);	
		}
		if(imgProcessing.detectFace(gray, 1.1, stages) == true) {
			cvShowImage("img", original);
			int key = cvWaitKey(0);
			// press s , S to save
			if(key == 83 || key == 115) {
				isSave = true;
			}
			cvDestroyWindow("img");	
		}
	}
}