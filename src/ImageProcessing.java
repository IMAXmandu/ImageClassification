import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_core.cvSet2D;
import static org.bytedeco.javacpp.opencv_core.cvSetReal2D;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.IplImage;

public class ImageProcessing {
	public double SEA_VAL_TH = 3.0; 	// sea image`s ratio of horizontal edge & vertical edge
	public double RAIN_VAL_TH = 1.15;	// rain image`s ratio of horizontal edge & vertical edge
	public int VECTOR_MIN_TH = 24;  	
	public int DEGREE_MIN_TH = 70;		
	public int DEGREE_MAX_TH = 152;		
	public int verEgdeCnt = 0;			
	public int horEgdeCnt = 0;			
	public double A_length = 0.0;		// vector A`s length
	public double B_length = 0.0;		// vector B`s length
	public int degree = 0;				// angle between two direction vectors
	
	private int THRESHOLD = 2551; 		// threshold to filter out too small an edge (1786 <= th <= 3316)
	private int[] Ax_y = new int[2];	// vector A(x,y)
	private int[] Bx_y = new int[2];	// vector B(x,y)
	
	
	/*
	 * image reduction by bilinear interpolation
	 * width = 240 (height is calculated as its ratio)
	 * I(x,y) = (1-dx)(1-dy)f(x,y) + dx(1-dy)f(x+1,y) + dy(1-dx)f(x,y+1) + dxdy*f(x+1,y+1)
	 */
	public IplImage reSize(IplImage img, IplImage returnImg) {
		int channel = img.nChannels();
		// [0]Blue, [1]Green, [2]Red or [0]Gray
		int[] color = new int[channel];				// f(x,y)
		int[] color_x1 = new int[channel];			// f(x+1,y)
		int[] color_y1 = new int[channel];			// f(x,y+1)
		int[] color_x1y1 = new int[channel];		// f(x+1,y+1)
		double newColor[] = new double[channel];	
		int width = img.width();
		int height = img.height();
		
		if(width > 240) {
			int newWidth = 240;
			double ratio = (double)240/(double)width;
			int newHeight = (int)(height*ratio);
			returnImg = cvCreateImage(cvSize(newWidth,newHeight),8,channel);
			returnImg.widthStep(newWidth*channel);
			returnImg.imageSize(returnImg.widthStep()*height);
			for(int i = 0; i < newHeight-1; i++) {
				int y = (int)(i/ratio);
				double dy = (double)i/ratio - y;
				int idx = y*img.widthStep();
				int idx_y1 = (y+1)*img.widthStep();
				for(int j = 0; j < newWidth-1; j++) {
					int x = (int)(j/ratio);
					double dx = (double)j/ratio - x; 
					for(int k = 0; k < channel; k++) {
						color[k] = img.imageData().get(idx + x*channel+k);
						if(color[k] < 0) {
							color[k] += 256;
						}
						color_x1[k] = img.imageData().get(idx + (x+1)*channel+k);
						if(color_x1[k] < 0) {
							color_x1[k] += 256;
						}
						color_y1[k] = img.imageData().get(idx_y1 + x*channel+k);
						if(color_y1[k] < 0) {
							color_y1[k] += 256;
						}
						color_x1y1[k] = img.imageData().get(idx_y1 + (x+1)*channel+k);
						if(color_x1y1[k] < 0) {
							color_x1y1[k] += 256;
						}
						newColor[k] = (1-dx)*(1-dy)*color[k] + dx*(1-dy)*color_x1[k] + dy*(1-dx)*color_y1[k] + dx*dy*color_x1y1[k];
					}
					if(channel == 3) {
						cvSet2D(returnImg,i,j,cvScalar(newColor[0],newColor[1],newColor[2],0));
					}else if(channel == 1) {
						cvSetReal2D(returnImg,i,j,newColor[0]);
					}
				}
			}
		}else {
			returnImg = cvCreateImage(cvGetSize(img),8,channel);
			returnImg.widthStep(returnImg.width()*channel);
			returnImg.imageSize(returnImg.widthStep()*height);
			copy(img,returnImg);
		}
		return returnImg;
	}
	
	
	/*
	 * Sharpening
	 * {-1, -1, -1}
	 * {-1,  9, -1}
	 * {-1, -1, -1}
	 */
	public void sharpening(IplImage img, IplImage returnImg) {
		int channel = img.nChannels();
		int[][] sharpen = new int[9][channel];		// sharpening mask
		int[] sharpenVal = new int[channel];		// calculated sharpening filter value
		IplImage tempImg = cvCreateImage(cvGetSize(img),8,channel);
		tempImg.widthStep(tempImg.width()*channel);
		tempImg.imageSize(tempImg.widthStep()*tempImg.height());
		
		for(int i = 0; i < img.height(); i++) {
			int prevIdx = (i-1)*img.widthStep();
			int idx = i*img.widthStep();
			int nextIdx = (i+1)*img.widthStep();
			for(int j = 0; j < img.width(); j++) {
				for(int k = 0; k < channel; k++) {
					if(i > 0 && j > 0) {
						sharpen[0][k] = img.imageData().get(prevIdx + (j-1)*channel+k);
						if(sharpen[0][k] < 0) {
							sharpen[0][k] += 256;
						}
					}
					if(i > 0) {
						sharpen[1][k] = img.imageData().get(prevIdx + j*channel+k);
						if(sharpen[1][k] < 0) {
							sharpen[1][k] += 256;
						}
					}
					if(i > 0 && j < (img.width()-1)) {
						sharpen[2][k] = img.imageData().get(prevIdx + (j+1)*channel+k);
						if(sharpen[2][k] < 0) {
							sharpen[2][k] += 256;
						}
					}
					if(j > 0) {
						sharpen[3][k] = img.imageData().get(idx + (j-1)*channel+k);
						if(sharpen[3][k] < 0) {
							sharpen[3][k] += 256;
						}
					}
					sharpen[4][k] = img.imageData().get(idx + j*channel+k);
					if(sharpen[4][k] < 0) {
						sharpen[4][k] += 256;
					}
					if(j < (img.width()-1)) {
						sharpen[5][k] = img.imageData().get(idx + (j+1)*channel+k);
						if(sharpen[5][k] < 0) {
							sharpen[5][k] += 256;
						}
					}
					if(i < (img.height()-1) && j > 0) {
						sharpen[6][k] = img.imageData().get(nextIdx + (j-1)*channel+k);
						if(sharpen[6][k] < 0) {
							sharpen[6][k] += 256;
						}
					}
					if(i < (img.height()-1)) {
						sharpen[7][k] = img.imageData().get(nextIdx + j*channel+k);
						if(sharpen[7][k] < 0) {
							sharpen[7][k] += 256;
						}
					}
					if(i < (img.height()-1) && j < (img.width()-1)) {
						sharpen[8][k] = img.imageData().get(nextIdx + (j+1)*channel+k);
						if(sharpen[8][k] < 0) {
							sharpen[8][k] += 256;
						}
					}
					sharpenVal[k] = sharpen[4][k]*9 - sharpen[0][k] - sharpen[1][k] - sharpen[2][k] - sharpen[3][k] - sharpen[5][k] - sharpen[6][k] - sharpen[7][k] - sharpen[8][k];
				}
				if(channel == 3) {
					cvSet2D(tempImg,i,j,cvScalar(sharpenVal[0],sharpenVal[1],sharpenVal[2],0));
				}else {
					cvSetReal2D(tempImg,i,j,sharpenVal[0]);	
				}
			}
		}
		copy(tempImg,returnImg);
		cvReleaseImage(tempImg);
	}
	
	
	/*
	 * RGB to Gray
	 * Y = 0.2989*R + 0.5870*G + 0.1140 * B 
	 */
	public IplImage gray(IplImage img, IplImage returnImg) {
		if(img.nChannels() == 1) {
			return img;
		}else {
			returnImg = cvCreateImage(cvGetSize(img),8,1);
			returnImg.widthStep(returnImg.width());
			returnImg.imageSize(returnImg.widthStep()*returnImg.height());
			int channel = img.nChannels();
			int[] color = new int[channel];
			for(int i = 0; i < img.height(); i++) {
				int idx = i*img.widthStep();
				for(int j = 0; j < img.width(); j++) {
					for(int k = 0; k < channel; k++) {
						color[k] = img.imageData().get(idx + j*channel+k);
						if(color[k] < 0) {
							color[k] += 256;
						}
					}
					double grayVal = 0.2989*color[0] + 0.587*color[1] + 0.114*color[2];
					cvSetReal2D(returnImg, i, j, grayVal);
				}
			}
		}
		return returnImg;
	}
	
	
	/*
	 * Sobel derivative - apply Scharr Filter
	 * Scharr Filter
	 * { -3, 0,  3} {-3, -10, -3}
	 * {-10, 0, 10} { 0,   0,  0}
	 * { -3, 0,  3} { 3,  10,  3}
	 * only use binarization image
	 */
	public void sobel(IplImage img) {
		if (img.nChannels() == 1) {
			for(int i = 1; i < img.height()-1; i++) {
				int prevIdx = (i-1)*img.width();
				int idx = i*img.width();
				int nextIdx = (i+1)*img.width();
				for(int j = 1; j < img.width()-1; j++) {
					int sobel1 = img.imageData().get(prevIdx + j-1);
					if(sobel1 < 0) {
						sobel1 += 256;
					}
					int sobel2 = img.imageData().get(prevIdx + j);
					if(sobel2 < 0) {
						sobel2 += 256;
					}
					int sobel3 = img.imageData().get(prevIdx + j+1);
					if(sobel3 < 0) {
						sobel3 += 256;
					}
					int sobel4 = img.imageData().get(idx + j-1);
					if(sobel4 < 0) {
						sobel4 += 256;
					}
					int sobel6 = img.imageData().get(idx + j+1);
					if(sobel6 < 0) {
						sobel6 += 256;
					}
					int sobel7 = img.imageData().get(nextIdx + j-1);
					if(sobel7 < 0) {
						sobel7 += 256;
					}
					int sobel8 = img.imageData().get(nextIdx + j);
					if(sobel8 < 0) {
						sobel8 += 256;
					}
					int sobel9 = img.imageData().get(nextIdx + j+1);
					if(sobel9 < 0) {
						sobel9 += 256;
					}
					
					int sobelVal = sobel3*3 + sobel6*10 + sobel9*3 - sobel1*3 - sobel4*10 - sobel7*3;
					if(sobelVal < 0) {
						sobelVal = -sobelVal;
					}
					if(sobelVal > THRESHOLD) {
						verEgdeCnt++;
					}
					sobelVal = sobel7*3 + sobel8*10 + sobel9*3 - sobel1*3 - sobel2*10 - sobel3*3;
					if(sobelVal < 0) {
						sobelVal = -sobelVal;
					}
					if(sobelVal > THRESHOLD) {
						horEgdeCnt++;
					}
				}
			}
		}
	}
	
	
	/*
	 * create Integral Image
	 * apply 1 channel gray
	 */
	public int[] integralImage(IplImage img, boolean square) {
		if(img.nChannels() == 3) {
			return null;
		}else {
			int[] integral = new int[img.width()*img.height()];
			integral[0] = img.imageData().get(0);
			if(integral[0] < 0) {
				integral[0] += 256;
			}
			if(square == true) {
				integral[0] *= integral[0];
			}
			for(int i = 1; i < img.width(); i++) {
				int x = img.imageData().get(i);
				if(x < 0) {
					x += 256;
				}
				if(square == true) {
					x *= x;
				}	
				integral[i] = x + integral[i-1];
			}
			for(int j = 1; j < img.height(); j++) {
				int y = img.imageData().get(j*img.width());
				if(y < 0) {
					y += 256;
				}
				if(square == true) {
					y *= y;
				}
				integral[j*img.width()] = y + integral[(j-1)*img.width()];
			}
			for(int i = 1; i < img.height(); i++) {
				int row = i*img.width();
				int pre_row = (i-1)*img.width();
				for(int j = 1; j < img.width(); j++) {
					// I(x,y)=f(x,y) + I(x-1,y) + I(x,y-1) - I(x-1,x-1)
					int fx_y = img.imageData().get(row + j);
					if(fx_y < 0) {
						fx_y += 256;
					}
					if(square == true) {
						fx_y *= fx_y;
					}
					integral[row + j] = fx_y + integral[pre_row + j] + integral[row + j-1] - integral[pre_row + j-1];
				}
			}
			return integral;
		}
	}
	
	
	/*
	 * Adaptive binarization
	 * use integral image to improve computation speed
	 * ROIhalf = set Region Of Interest radius
	 */
	public IplImage binarization(IplImage img, IplImage returnImg, int ROIhalf) {
		if(img.nChannels() == 3) {
			return null;
		}else {
			returnImg = cvCreateImage(cvGetSize(img),8,1);
			returnImg.widthStep(returnImg.width());
			returnImg.imageSize(returnImg.widthStep()*returnImg.height());
			
			// create integal image
			int[] integral = integralImage(img, false);
			
			for(int i = 0; i < img.height(); i++) {
				for(int j = 0; j < img.width(); j++) {
					int Ix1_y2 = 0;		// I(x1-1,y2)
					int Ix2_y1 = 0;		// I(x2,y1-1)
					int Ix1_y1 = 0;		// I(x1-1,y1-1)
					// set ROI
					int y1 = i-ROIhalf;
					int y2 = i+ROIhalf;
					int x1 = j-ROIhalf;
					int x2 = j+ROIhalf;
					
					// boundary check 
					if(x2 >= img.width()) {
						x2 = img.width()-1;
					}
					if(y2 >= img.height()) {
						y2 = img.height()-1;
					}
					if(x1 <= 0 && y1 <= 0) {
						x1 = 0;
						y1 = 0;
						Ix1_y2 = 0;
						Ix2_y1 = 0;
						Ix1_y1 = 0;
					} else if(x1 <= 0 && 0 < y1 && y2 < img.height()) {
						x1 = 0;
						Ix1_y2 = 0;
						Ix2_y1 = integral[(y1-1)*img.width() + x2];
						Ix1_y1 = 0;
					} else if(0 < x1 && x2 < img.width() && y1 <= 0) {
						y1 = 0;
						Ix1_y2 = integral[y2*img.width() + x1-1];
						Ix2_y1 = 0;
						Ix1_y1 = 0;
					} else {
						Ix1_y2 = integral[y2*img.width() + x1-1];
						Ix2_y1 = integral[(y1-1)*img.width() + x2];
						Ix1_y1 = integral[(y1-1)*img.width() + x1-1];
					}
					
					// sum of ROI = I(x2,y2) - I(x2,y1-1) - I(x1-1,y2) + I(x1-1,y1-1)
					int sum = integral[y2*img.width() + x2] - Ix2_y1 - Ix1_y2 + Ix1_y1;
					int fx_y = img.imageData().get(i*img.width() + j);
					if(fx_y < 0) {
						fx_y += 256;
					}
					if(fx_y * (x2-x1)*(y2-y1) <= sum) {
						cvSetReal2D(returnImg,i,j,0);
					} else{
						cvSetReal2D(returnImg,i,j,255);
					}
				}
			}
		}
		return returnImg;
	}
	
	
	/*
	 * remove a specific color value.
	 * only use color image
	 */
	public void colorRemoval(IplImage img) {
		if(img.nChannels() == 1) {
			return ;
		}else {
			for(int i = 0; i < img.height(); i++) {
				int idx = i*img.widthStep();
				for(int j = 0; j < img.width(); j++) {
					int blue = img.imageData().get(idx + j*3);
					if(blue < 0) {
						blue += 256;
					}
					int green = img.imageData().get(idx + j*3+1);
					if(green < 0) {
						green += 256;
					}
					int red = img.imageData().get(idx + j*3+2);
					if(red < 0) {
						red += 256;
					}
					// remove sky
					if((blue > 117 && blue > red && blue > green) || (blue > 210 && green > 210 && red > 190)) { 
						if(blue >= 170 || (blue - green) >= 50) {
							cvSet2D(img,i,j,cvScalar(0,0,0,0));
						}
					}	
				}
			}
		}
	}
	
	
	/*
	 * Image Erosion
	 * use 3 * 3 kernel
	 * only use binarization image
	 */
	public void erode(IplImage img, IplImage returnImg) {
		if(img.nChannels() == 1) {
			IplImage tempImg = cvCreateImage(cvGetSize(img),8,1);
			tempImg.widthStep(tempImg.width());
			tempImg.imageSize(tempImg.widthStep()*tempImg.height());
			
			// boundary initialize 0
			for(int j = 0; j < img.width(); j++) {
				cvSetReal2D(tempImg,0,j,0);
				cvSetReal2D(tempImg,img.height()-1,j,0);
			}
			for(int i = 1; i < img.height()-1; i++) {
				int idx = i*img.widthStep();
				int prevIdx = (i-1)*img.widthStep();
				int nextIdx = (i+1)*img.widthStep();
				for(int j = 0; j < img.width(); j++) {
					if(j == 0) {
						cvSetReal2D(tempImg,i,j,0);
						continue;
					}else if(j == img.width()-1) {
						cvSetReal2D(tempImg,i,j,0);
						continue;
					}
					// IplImage stores 255 as -1.
					// perform only when the middle pixel is white to speed up
					// 8-way inspection
					if(img.imageData().get(idx + j) == -1) {
						int sum = img.imageData().get(prevIdx + j-1) + img.imageData().get(prevIdx + j) + img.imageData().get(prevIdx + j+1) + img.imageData().get(idx + j-1) + img.imageData().get(idx + j+1) + img.imageData().get(nextIdx + j-1) + img.imageData().get(nextIdx + j) + img.imageData().get(nextIdx + j+1);
						if(sum > -8) {
							cvSetReal2D(tempImg,i,j,0);
						}else {
							cvSetReal2D(tempImg,i,j,255);
						}
					}else {
						cvSetReal2D(tempImg,i,j,0);
					}
				}
			}
			copy(tempImg, returnImg);
			cvReleaseImage(tempImg);
		}
	}
	
	
	/*
	 * Image Dilatation
	 * use 3 * 3 kernel
	 * only use binarization image
	 */
	public void dilate(IplImage img, IplImage returnImg) {
		if(img.nChannels() == 1) {
			IplImage tempImg = cvCreateImage(cvGetSize(img),8,1);
			tempImg.widthStep(tempImg.width());
			tempImg.imageSize(tempImg.widthStep()*tempImg.height());
			
			// boundary initialize 0
			for(int j = 0; j < img.width(); j++) {
				cvSetReal2D(tempImg,0,j,0);
				cvSetReal2D(tempImg,img.height()-1,j,0);
			}
			for(int i = 1; i < img.height()-1; i++) {
				int idx = i*img.widthStep();
				int prevIdx = (i-1)*img.widthStep();
				int nextIdx = (i+1)*img.widthStep();
				for(int j = 0; j < img.width(); j++) {
					if(j == 0) {
						cvSetReal2D(tempImg,i,j,0);
						continue;
					}else if(j == img.width()-1) {
						cvSetReal2D(tempImg,i,j,0);
						continue;
					}
					// IplImage stores 255 as -1.
					// perform only when the middle pixel is black to speed up
					// 8-way inspection
					if(img.imageData().get(idx + j) == 0) {
						int sum = img.imageData().get(prevIdx + j-1) + img.imageData().get(prevIdx + j) + img.imageData().get(prevIdx + j+1) + img.imageData().get(idx + j-1) + img.imageData().get(idx + j+1) + img.imageData().get(nextIdx + j-1) + img.imageData().get(nextIdx + j) + img.imageData().get(nextIdx + j+1);
						if(sum < 0) {
							cvSetReal2D(tempImg,i,j,255);
						}else {
							cvSetReal2D(tempImg,i,j,0);
						}
					}else {
						cvSetReal2D(tempImg,i,j,255);
					}
				}
			}
			copy(tempImg, returnImg);
			cvReleaseImage(tempImg);
		}
	}
	
	
	public void copy(IplImage img, IplImage returnImg) {
		if(img.imageSize() == returnImg.imageSize()) {
			int channel = img.nChannels();
			int[] color = new int [channel];
			
			for(int i = 0; i < img.height(); i++) {
				int idx = i*img.widthStep();
				for(int j = 0; j < img.width(); j++) {
					for(int k = 0; k < channel; k++) {
						color[k] = img.imageData().get(idx + j*channel+k);
						if(color[k] < 0) {
							color[k] += 256;
						}
					}
					if(channel == 3) {
						cvSet2D(returnImg,i,j,cvScalar(color[0],color[1],color[2],0));
					}else if(channel == 1){
						cvSetReal2D(returnImg,i,j,color[0]);
					}
				}
			}	
		}
	}
	
	
	/*
	 * Calculate the angle between two lines using inner product of vector
	 * only use binarization image
	 * perform linear approximation
	 */
	public void innerProduct(IplImage img) {
		boolean find = false;
		if(img.nChannels() == 1) {
			for(int i = 0; i < img.height(); i++) {
				int idx = i*img.widthStep();
				for(int j = 0; j < img.width(); j++) {
					// meet 255, set it as a reference point
					if(img.imageData().get(idx + j) == -1) {
						Ax_y[0] = 0;
						Ax_y[1] = 0;
						Bx_y[0] = 0;
						Bx_y[1] = 0;

						leftInnerProduct(img, i, j, Ax_y[0], Ax_y[1]);
						rightInnerProduct(img, i, j, Bx_y[0], Bx_y[1]);
						find = true;
						break;
					}
				}
				if(find == true) {
					break;
				}
			}			
		}
		
		degree = 0;
		A_length = Math.sqrt(Ax_y[0]*Ax_y[0] + Ax_y[1]*Ax_y[1]);	// |A|
		B_length = Math.sqrt(Bx_y[0]*Bx_y[0] + Bx_y[1]*Bx_y[1]);	// |B|
		// cos(еш) = (Ax*Bx + Ay*By) / (|A|*|B|)
		double innerVal = 0;
		if(A_length != 0 && B_length != 0) {
			innerVal = (Ax_y[0]*Bx_y[0] + Ax_y[1]*Bx_y[1])/(A_length*B_length);
			degree = (int)((Math.acos(innerVal)*180/Math.PI) + 0.5);
		}
	}
	
	
	/*
	 * Perform linear approximation to the left
	 * {0, 0, 0}
	 * {1, 0, 0}
	 * {2, 3, 0}
	 * number is order of execution
	 */ 
	private void leftInnerProduct(IplImage img, int heightVal, int widthVal, int xCnt, int yCnt) {
		int width = img.width();
		int halfWidth = width/2; 
		int height = img.height();
		
		while(0 <= widthVal && widthVal < width && heightVal < height) {
			// direction 1
			if(img.imageData().get(heightVal*img.widthStep() + widthVal-1) == -1) {
				Ax_y[0] -= 1;
				widthVal -= 1;
			}
			// direction 2
			else if(img.imageData().get((heightVal+1)*img.widthStep() + widthVal-1) == -1) {
				Ax_y[0] -= 1;
				Ax_y[1] += 1;
				heightVal += 1;
				widthVal -= 1;
			}
			// direction 3
			else if(img.imageData().get((heightVal+1)*img.widthStep() + widthVal) == -1) {
				Ax_y[1] += 1;
				heightVal += 1;
			}else {
				break;
			}
			// stop, if it perform half of image
			if(Ax_y[1] > halfWidth) {
				break;
			}
		}
	}
	
	
	/*
	 * Perform linear approximation to the right
	 * {0, 0, 0}
	 * {0, 0, 1}
	 * {0, 3, 2}
	 * number is order of execution
	 */ 
	private void rightInnerProduct(IplImage img, int heightVal, int widthVal, int xCnt, int yCnt) {
		int width = img.width();
		int halfWidth = width/2; 
		int height = img.height();
		
		while(0 <= widthVal && widthVal < width && heightVal < height) {
			// direction 1
			if(img.imageData().get(heightVal*img.widthStep() + widthVal+1) == -1) {
				Bx_y[0] += 1;
				widthVal += 1;
			}
			// direction 2
			else if(img.imageData().get((heightVal+1)*img.widthStep() + widthVal+1) == -1) {
				Bx_y[0] += 1;
				Bx_y[1] += 1;
				heightVal += 1;
				widthVal += 1;
			}
			// direction 3
			else if(img.imageData().get((heightVal+1)*img.widthStep() + widthVal) == -1) {
				Bx_y[1] += 1;
				heightVal += 1;
			}else {
				break;
			}
			// stop, if it perform half of image
			if(Bx_y[1] > halfWidth) {
				break;
			}
		}
	}
	
	
	/*
	 * Face detection
	 * select weak classifier from several Haar-like features by Adaboost method
	 * create a single strong classifier with a weak classifier (haarcascade_frontalface_alt2.xml) 
	 * use cascade method to improve computation speed
	 * feature size = 20x20		scaleUpdate = 1.1		step = 2
	 */
	public boolean detectFace(IplImage img, double scaleUpdate, MakeList<Stage> stages) {
		double scale = 1;
	    int windowSize = 20;
	    int[] integral = integralImage(img, false);
	    int[] integralSquare = integralImage(img, true);
	    
	    while(windowSize < img.width()) {
	    	// window size is enlarged by scaleUpdate
	        windowSize = (int)(20*scale);
	        // set the amount of pixels to skip. default is two
	        int step = 2;
	        if(scale > 2) {
	        	step = (int)scale;
	        }
	        
	        // face detection
	        for(int i = 1; i < img.height() - windowSize; i += step) {
	            for(int j = 1; j < img.width() - windowSize; j += step) {
	            	// get the standard deviation of the pixels inside the window
	            	double mean = (integral[(i+windowSize)*img.width() + j+windowSize] - integral[(i-1)*img.width() + j+windowSize] - integral[(i+windowSize)*img.width() + j-1] + integral[(i-1)*img.width() + j-1])/(windowSize*windowSize);
	            	double variance = (integralSquare[(i+windowSize)*img.width() + j+windowSize] - integralSquare[(i-1)*img.width() + j+windowSize] - integralSquare[(i+windowSize)*img.width() + j-1] + integralSquare[(i-1)*img.width() + j-1])/(windowSize*windowSize) - (mean*mean);
	            	double standard_deviation = 0;
	            	if(variance > 0) {
	                    standard_deviation = Math.sqrt(variance);
	                }else {
	                    standard_deviation = 1;
	                }
	            	
	            	// load each stage
	            	int stageIdx = 0;
	            	for(stageIdx = 0; stageIdx < stages.size(); stageIdx++) {
	                    Stage stage = stages.get(stageIdx);
	                    double stage_sum = 0;
	                    
	                    // load each tree
	                    for(int treeIdx=0; treeIdx < stage.trees.size(); treeIdx++) {
	                        Tree tree = stage.trees.get(treeIdx);
	                        int leafIdx = 0;
	                        
	                        // load each leaf
	                        do {
	                        	Leaf leaf = tree.leafs.get(leafIdx);
	                        	double sum = 0;
	                        	for(int rectNum = 0; rectNum < leaf.rects.size(); rectNum++) {
	                        		// rearrange rectangles in leaf to fit window size
	                        		Rect rect = leaf.rects.get(rectNum);
	                        		int x = (int)(rect.x * scale) + j;
	                        		int y = (int)(rect.y * scale) + i;
	                        		int width = (int)(rect.width * scale);
	                        		int height = (int)(rect.height * scale);
	                        		int weight = rect.weight;
	                        		double correction_ratio = 1; 
	                        		// if the feature is tilted at 45 degrees, the weight is halved
	                        		if(leaf.tilted == true) {
	                        			correction_ratio = 0.5;
	                        		}
	                        		double newWeight = weight * correction_ratio;
	                        		int getRect = integral[(y+height)*img.width() + x+width] - integral[(y-1)*img.width() + x+width] - integral[(y+height)*img.width() + x-1] + integral[(y-1)*img.width() + x-1];
	                        		newWeight *= getRect;
	                                sum += newWeight;
	                        	}
	                        	sum = sum / (windowSize * windowSize);
	                        	
	                        	if(sum < leaf.threshold * standard_deviation) {
	                        		if(leaf.left_node == true) {
	                            		leafIdx++;	
	                            	}else {
	                            		leafIdx = 0;
	                            	}
	                                stage_sum += leaf.left_val;
	                            }else {
	                            	if(leaf.right_node == true) {
	                            		leafIdx++;	
	                            	}else {
	                            		leafIdx = 0;
	                            	}
	                                stage_sum += leaf.right_val;
	                            }
	                        } while(leafIdx > 0);
	                    } 

	                    if(stage_sum >= stage.stageThreshold) {
	                    	// Cascade Pass
	                        continue; 
	                    }
	                    else {
	                    	// Cascade Stop
	                        break;    
	                    }
	                }
	            	// It is human. if it pass all stage
	                if(stageIdx == stages.size()) {
	                    return true;
	                }
	            }
	        }
	        scale *= scaleUpdate;
	    }
	    return false;
	}
}

