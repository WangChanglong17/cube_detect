#include <opencv.hpp>
#include <iostream>
#include <algorithm>
#include <Windows.h>
using namespace cv;
using namespace std;

//得到矩形框四个顶点
void cvBoxPoints(CvBox2D box, CvPoint2D32f pt[4])
{
	double angle = (-box.angle-90)*CV_PI / 180;
	float a = (float)cos(angle)*0.5f;
	float b = (float)sin(angle)*0.5f;

	pt[0].x = box.center.x - a*box.size.height - b*box.size.width;
	pt[0].y = box.center.y + b*box.size.height - a*box.size.width;
	pt[1].x = box.center.x + a*box.size.height - b*box.size.width;
	pt[1].y = box.center.y - b*box.size.height - a*box.size.width;
	pt[2].x = 2 * box.center.x - pt[0].x;
	pt[2].y = 2 * box.center.y - pt[0].y;
	pt[3].x = 2 * box.center.x - pt[1].x;
	pt[3].y = 2 * box.center.y - pt[1].y;
}

//画出矩形框
void DrawBox(CvBox2D box,Mat img)
{
	CvPoint2D32f point[4];
	int i;
	for (i = 0; i<4; i++)
	{
		point[i].x = 0;
		point[i].y = 0;
	}
	cvBoxPoints(box, point); //计算二维盒子顶点
	CvPoint pt[4];
	for (i = 0; i<4; i++)
	{
		pt[i].x = (int)point[i].x;
		pt[i].y = (int)point[i].y;
	}
	line(img, pt[0], pt[1], CV_RGB(0, 255, 0), 2, 8, 0);
	line(img, pt[1], pt[2], CV_RGB(0, 255, 0), 2, 8, 0);
	line(img, pt[2], pt[3], CV_RGB(0, 255, 0), 2, 8, 0);
	line(img, pt[3], pt[0], CV_RGB(0, 255, 0), 2, 8, 0);
}

int main()
{
	DWORD start = GetTickCount();
	Mat input1 = imread("E:\\Save\\visualstudio2013\\Projects\\cubedectect\\cubedectect\\depth.png");
	namedWindow("input", WINDOW_AUTOSIZE);
	Mat input;
	resize(input1, input, Size(input1.cols / 2, input1.rows / 2), 0, 0, INTER_LINEAR);
	
	cout << "输入深度图像分辨率：" << input.cols <<"×" <<input.rows << endl;
	/*
	fstream file1;
	file1.open("E:\\Save\\visualstudio2013\\Projects\\cubedectect\\cubedectect\\depth2.txt");

	Mat input = Mat::zeros(254, 512,CV_32F);

	for (int i = 0; i < 254; i++)
	{
		for (int j = 0; j < 512; j++)
		{
			file1 >> input.at<float>(i, j);
		}
	}
	*/
	
	namedWindow("imggray", WINDOW_AUTOSIZE);
	Mat imggray;
	cvtColor(input, imggray, CV_BGR2GRAY);
	
	//将灰度图像复制到另一个矩阵中
	Mat imggray_copy, imggray_copy2;
	imggray.copyTo(imggray_copy);
	imggray.copyTo(imggray_copy2);
	namedWindow("imggray_copy", WINDOW_AUTOSIZE);
	namedWindow("imggray_copy2", WINDOW_AUTOSIZE);

	
	namedWindow("gaussian", WINDOW_AUTOSIZE);
	Mat imggaussian;
	GaussianBlur(input, imggaussian, Size(7, 7), 0, 0);
	

	

	/*
		//直方图均衡化
	Mat imgEqualizeHist;
	equalizeHist(imggray, imgEqualizeHist);
	namedWindow("EqualizeHist", WINDOW_AUTOSIZE);
	imshow("EqualizeHist", imgEqualizeHist);

	//图像闭运算，将亮的区域连在一起
	Mat imgclose;
	Mat element1 = getStructuringElement(MORPH_RECT, Size(15, 15));
	morphologyEx(imgEqualizeHist, imgclose, CV_MOP_BLACKHAT, element1);
	namedWindow("imgclose", WINDOW_AUTOSIZE);
	imshow("imgclose", imgclose);
	*/

	//边缘检测
	namedWindow("canny", WINDOW_AUTOSIZE);
	Mat imgcanny;
	Canny(imggaussian, imgcanny, 20, 60, 3);

	//对图像进行膨胀
	Mat imgDilate;
	Mat element = getStructuringElement(MORPH_DILATE, Size(5, 5));
	dilate(imgcanny, imgDilate, element);
	namedWindow("imgDilate", WINDOW_AUTOSIZE);
	

	/*
	//膨胀之后腐蚀
	Mat imgErode;
	Mat element2 = getStructuringElement(MORPH_RECT, Size(3, 3));
	erode(imgcanny, imgErode, element2);
	namedWindow("imgErode", WINDOW_AUTOSIZE);
	imshow("imgErode", imgErode);
	*/	
	

	/*
	//绘制出霍夫变换线段
	vector<Vec4i> Lines;
	HoughLinesP(imgDilate, Lines, 1, CV_PI / 180, 50, 50, 10);
	cout <<"经霍夫变换找到了" <<Lines.size() << "条线段" << endl;
	for (size_t i = 0; i < Lines.size(); i++)
	{
	Point pp1, pp2;
	pp1 = Point(Lines[i][0], Lines[i][1]);
	pp2 = Point(Lines[i][2], Lines[i][3]);
	//cout << i << ": " << Lines[i][0] << " " << Lines[i][1] << " " << Lines[i][2] << " " << Lines[i][3] << endl;
	line(input, pp1, pp2, Scalar(0, 255, 0), 2);
	}
	*/
	
	
	//轮廓识别与描绘
	//std::vector<std::vector<cv::Point>> contours;
	vector<vector<Point>> contours;
	vector<Vec4i> hierarchy;
	//轮廓内面积
	vector<double> interArea;
	
	//对膨胀后图像进行反二值化操作
	namedWindow("thresholdimg", WINDOW_AUTOSIZE);
	Mat thresholdimg;
	threshold(imgDilate, thresholdimg, 10, 255, THRESH_BINARY_INV);

	//反二值化后，得到白色区域的黑色轮廓
	findContours(thresholdimg, contours, hierarchy, RETR_TREE, CV_CHAIN_APPROX_SIMPLE);

	for (int i = 0; i < contours.size(); i++)
	{
		interArea.push_back(contourArea(contours[i]));
		//cout <<"第"<<i<<"个轮廓区域面积："<< interArea[i] << endl;
	}
	//对轮廓内面积进行排序
	vector<double> interAreaTem = interArea;

	sort(interAreaTem.begin(),interAreaTem.end());	

	vector<double>::iterator third_contours = find(interArea.begin(), interArea.end(), interAreaTem[interAreaTem.size()-3]);
	
	int thearea;
	if (third_contours != interArea.end())
	{
		thearea = third_contours - interArea.begin();
		cout << "要找的轮廓序号为：" << thearea << endl;
		cout << "其面积为：" << interAreaTem[interAreaTem.size() - 3] << endl;
		drawContours(imggray_copy, contours, thearea, cv::Scalar::all(255), -1);		
	}
	else
		cout << "There is no result." << endl;

	//二值化目标轮廓图
	Mat imggray_copy_threshold;
	threshold(imggray_copy, imggray_copy_threshold, 254, 255, THRESH_BINARY);
	//将二值化轮廓图与深度数据矩阵（先用灰度图代替）点乘，得到新的矩阵求均值，可认为为顶面深度数据
	Mat depth_pic_top;
	//cout << "二值化图平均值" << " " << mean(imggray_copy_threshold)[0]<<endl;
	//cout << "灰度图平均值" << " " << mean(imggray)[0] << endl;
	multiply(imggray, imggray_copy_threshold, depth_pic_top, 1, CV_32F);
	namedWindow("depth_pic_top", WINDOW_AUTOSIZE);
	double depth_top = mean(depth_pic_top)[0] / mean(imggray_copy_threshold)[0];
	cout << "盒顶部平均深度：" << " " << depth_top<<endl;


	//除最大轮廓外全部涂白
	vector<double>::iterator biggest_contours = find(interArea.begin(), interArea.end(), interAreaTem[interAreaTem.size() - 1]);
	int biggest_contours_num = biggest_contours - interArea.begin();
	int area_sum = 0;
	for (int i = 0; i < contours.size(); i++)
	{
		if (i != biggest_contours_num)
		{
			drawContours(imggray_copy2, contours, i, cv::Scalar::all(255), -1);
		}		
	}	
	//涂白后的图像反二值化操作，得到模板图
	Mat imggray_copy2_threshold;
	threshold(imggray_copy2, imggray_copy2_threshold, 254, 255, THRESH_BINARY_INV);
	//计算点乘后矩阵
	Mat depth_pic_bottom;
	//cout << "二值化图平均值" << " " << mean(imggray_copy2_threshold)[0];
	multiply(imggray, imggray_copy2_threshold, depth_pic_bottom, 1,CV_32F);
	double depth_bottom = mean(depth_pic_bottom)[0]/ mean(imggray_copy2_threshold)[0];

	cout << "盒底部平均深度：" << " " << depth_bottom <<endl;

	cout << "快递盒深度为" <<  abs(depth_top-depth_bottom) << endl;

	//获取所需轮廓外接矩形
	CvBox2D box = minAreaRect(contours[thearea]);
	//DrawBox(box, input);
	cout << "快递盒长：" << box.size.width << " " << "快递盒长：" << box.size.height << " " << "快递盒面积：" << box.size.height * box.size.width << endl;
	DWORD end = GetTickCount();

	cout << "运行时间：" << end - start << endl;
	
	imshow("imgDilate", imgDilate);
	imshow("input", input);
	//imshow("gaussian", imggaussian);
	//imshow("gaussian", imggaussian);
	imshow("canny", imgcanny);
	imshow("imggray", imggray);
	imshow("depth_pic_top", depth_pic_top);
	imshow("imggray_copy", imggray_copy);
	imshow("thresholdimg", imggray_copy_threshold);
	imshow("imggray_copy2", imggray_copy2);
	waitKey(0);
	system("PAUSE");
	return 0;

}