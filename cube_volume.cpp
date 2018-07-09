//
// Created by WangChanglong on 2018/6/28.
//
#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

int convert_yuv_to_rgb_pixel(int y, int u, int v)
{
    unsigned int pixel32 = 0;
    unsigned char *pixel = (unsigned char *)&pixel32;
    int r, g, b;
    //RGB和yuyv的范围都是0-255，其转换公式如下：
    //r = y + 1.370705 * v ;
    //g = y - 0.698001 * v  - 0.337633 * u ;
    //b = y + 1.732446 * u ;
    r = y * 3;
    g = u * 3;
    b = v * 3;
    if (r > 255) r = 255;
    if (g > 255) g = 255;
    if (b > 255) b = 255;
    if (r < 0) r = 0;
    if (g < 0) g = 0;
    if (b < 0) b = 0;
    pixel[0] = b ;
    pixel[1] = g ;
    pixel[2] = r ;
    return pixel32;
}

int convert_yuv_to_rgb_buffer(unsigned char *yuv, unsigned char *rgb, unsigned int width, unsigned int height)
{
    unsigned int in, out = 0;
    unsigned int pixel_16;
    unsigned char pixel_24[3];
    unsigned int pixel32;
    int y0, u, y1, v;
    for (in = 0; in < width * height * 2; in += 4) {
        pixel_16 =
                yuv[in + 3] << 24 |
                yuv[in + 2] << 16 |
                yuv[in + 1] << 8 |
                yuv[in + 0];
        y0 = (pixel_16 & 0x000000ff);
        u = (pixel_16 & 0x0000ff00) >> 8;
        y1 = (pixel_16 & 0x00ff0000) >> 16;
        v = (pixel_16 & 0xff000000) >> 24;
        pixel32 = convert_yuv_to_rgb_pixel(y0, u, v);
        pixel_24[0] = (pixel32 & 0x000000ff);
        pixel_24[1] = (pixel32 & 0x0000ff00) >> 8;
        pixel_24[2] = (pixel32 & 0x00ff0000) >> 16;
        rgb[out++] = pixel_24[0];
        rgb[out++] = pixel_24[1];
        rgb[out++] = pixel_24[2];
       // rgb[out++] = 0;
        pixel32 = convert_yuv_to_rgb_pixel(y1, u, v);
        pixel_24[0] = (pixel32 & 0x000000ff);
        pixel_24[1] = (pixel32 & 0x0000ff00) >> 8;
        pixel_24[2] = (pixel32 & 0x00ff0000) >> 16;
        rgb[out++] = pixel_24[0];
        rgb[out++] = pixel_24[1];
        rgb[out++] = pixel_24[2];
        //rgb[out++] = 0;
    }
    return 0;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_esp_uvc_usbcamera_CameraMainActivity_cube_1volume(JNIEnv *env, jclass type,jbyteArray yuyv_frame,jint yuyv_length,
                                                           jintArray pixels_, jint w, jint h)
{

    jint *cbuf;
    jboolean ptfalse = false;

    cbuf = env->GetIntArrayElements(pixels_, &ptfalse);

    if(cbuf == NULL){
        return 0;
    }

    jbyte *bbuf;
    bbuf = env->GetByteArrayElements(yuyv_frame, &ptfalse);

    unsigned char * yuyv = (unsigned char *) bbuf;

   // unsigned char *input_buf = new unsigned char[w*h*4];

    //convert_yuv_to_rgb_buffer(yuyv, input_buf, w, h);

    string s_result;

   // Mat real_depth1(h, w * 2, CV_16UC1, cbuf);
    Mat real_depth1(h, w * 2, CV_16UC1);
    int kum = 0;
    for(int i=0;i<h;i++)
    {
        for(int j = 0; j<2*w;j++)
        {
            real_depth1.at<ushort>(i,j) = ushort(cbuf[kum]);
            kum++;
        }
    }

    Mat input2(h, w * 2, CV_8UC1,yuyv);

    /*
     *
    int numm = 0;
    for(int i = 0; i<input2.rows;i++)
    {
        for(int j = 0 ; j<input2.cols;j++)
        {
            input2.at<uchar>(i,j) = uchar (yuyv[numm]);
            numm++;
        }
    }
     */


    Mat input1(h, w * 2, CV_8UC1); //将图像内部像素每个点乘3倍，增加边缘梯度

    for(int i = 0; i<input1.rows;i++)
    {
        for(int j = 0 ; j<input1.cols;j++)
        {
            input1.at<uchar>(i,j) = 3 * (input2.at<uchar>(i,j));
        }
    }

    double xxxx = double(input2.at<uchar>(235,320));

    double xxx = double(input2.at<uchar>(240,320));


    //Rect rect(input1.cols * 0.2, input1.rows* 0.1, input1.cols*0.8, input1.rows*0.9);
    Rect rect(input1.cols * 0.15, 0, input1.cols*0.85, input1.rows);
    Mat input = input1(rect);

    Mat real_depth = real_depth1(rect);
    //去除深度数据中一些巨大偏离点

    for(int i =0; i<h; i++)
    {
        for(int j = 0; j<w*2;j++)
        {
            int tem_depth = int(real_depth.at<ushort>(i,j));
            if(tem_depth > 3000)
                real_depth.at<ushort>(i,j) = 0;
        }
    }

    Mat imggray;
    input.copyTo(imggray);
    //cvtColor(input, imggray, CV_BGR2GRAY);


    //将灰度图像复制到另两个个矩阵中
    Mat imggray_copy, imggray_copy2,imggray_copy_threshold;
    imggray.copyTo(imggray_copy);
    imggray.copyTo(imggray_copy2);


    //此处需要对彩色图像进行处理，而不是深度图像，需要获取灰度图的RGB值
    Mat imggaussian;
    GaussianBlur(input, imggaussian, Size(5, 5), 0, 0);


    //边缘检测
    Mat imgcanny;
    //Sobel(imggaussian, imgcanny,-1,1,1 );
    Canny(imggaussian, imgcanny, 8, 17, 3);


    //对图像进行膨胀,效果有待检验
    Mat imgDilate;
    Mat element = getStructuringElement(MORPH_DILATE, Size(7, 7));
    dilate(imgcanny, imgDilate, element);

    //轮廓识别与描绘
    //std::vector<std::vector<cv::Point>> contours;
    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    //轮廓内面积
    vector<double> interArea;


   //对膨胀后图像进行反二值化操作
   Mat thresholdimg;
   threshold(imgDilate, thresholdimg, 10, 255, THRESH_BINARY_INV);

   //反二值化后，得到白色区域的黑色轮廓
   findContours(imgDilate, contours, hierarchy, RETR_TREE, CV_CHAIN_APPROX_SIMPLE);

    if(contours.size() == 0)
    {
        s_result = "no cube detect";
        env->ReleaseIntArrayElements(pixels_, cbuf, 0);
        env->ReleaseByteArrayElements(yuyv_frame, bbuf, 0);

        return env->NewStringUTF(s_result.c_str());
    }

   for (int i = 0; i < contours.size(); i++)
   {
       interArea.push_back(contourArea(contours[i]));
   }
   //对轮廓内面积进行排序

   vector<double> interAreaTem = interArea;

   sort(interAreaTem.begin(),interAreaTem.end());

    //int count = interAreaTem.size();


   vector<double>::iterator the_contours = find(interArea.begin(), interArea.end(), interAreaTem[interAreaTem.size()-1]);

   int thearea ;
   double areabiggest;
   int kkk = 2;


   thearea = the_contours - interArea.begin();


  // while(pointPolygonTest(contours[thearea], Point(input.cols / 2, input.rows / 2), false) != 1)
  // {
  //     the_contours = find(interArea.begin(), interArea.end(), interAreaTem[interAreaTem.size() - kkk]);
  //     kkk++;
  //     thearea = the_contours - interArea.begin();}


   if (the_contours != interArea.end())
   {
       areabiggest = interAreaTem[interAreaTem.size() - 1];
       drawContours(imggray_copy, contours, thearea, cv::Scalar::all(255), CV_FILLED);

   }

   //二值化目标轮廓图
   threshold(imggray_copy, imggray_copy_threshold, 254, 255, THRESH_BINARY);
   //将二值化轮廓图与深度数据矩阵（先用灰度图代替）点乘，得到新的矩阵求均值，可认为为顶面深度数据

  // Mat depth_pic_top;
  // imggray_copy_threshold.convertTo(imggray_copy_threshold,CV_16UC1,1,0);
  // multiply(real_depth, imggray_copy_threshold, depth_pic_top, 1, CV_32F);
   double sum_1 = 0; int k = 0;
   for (int i = 0; i < real_depth.rows; i++)
   {
       for (int j = 0; j < real_depth.cols;j++)
           if (imggray_copy_threshold.at<uchar>(i, j) > 0)
           {
               if(real_depth.at<short>(i, j) != 0)
               {
                   sum_1 += double(real_depth.at<short>(i, j));
                   //cout << real_depth.at<short>(i, j);
                   k++;
               }

           }
   }
   double depth_top = double(sum_1 / k);

   //double depth_top = mean(depth_pic_top)[0] / mean(imggray_copy_threshold)[0];

   //除最大轮廓外全部涂白
   vector<double>::iterator biggest_contours = find(interArea.begin(), interArea.end(), interAreaTem[interAreaTem.size() - 1]);
   int biggest_contours_num = biggest_contours - interArea.begin();
   int area_sum = 0;
   for (int i = 0; i < contours.size(); i++)
   {
       //if (i != biggest_contours_num)
       {
           drawContours(imggray_copy2, contours, i, cv::Scalar::all(255), -1);
       }
   }
   //涂白后的图像反二值化操作，得到模板图
   Mat imggray_copy2_threshold;
   threshold(imggray_copy2, imggray_copy2_threshold, 254, 255, THRESH_BINARY_INV);
   //计算点乘后矩阵
   //Mat depth_pic_bottom;
   //imggray_copy2_threshold.convertTo(imggray_copy2_threshold,CV_16UC1,1,0);
   //multiply(real_depth, imggray_copy2_threshold, depth_pic_bottom, 1,CV_32F);
   //double depth_bottom = mean(depth_pic_bottom)[0]/ mean(imggray_copy2_threshold)[0];
   double sum_2 = 0; int kk = 0;
   for (int i = 0; i < real_depth.rows; i++)
   {
       for (int j = 0; j < real_depth.cols; j++)
           if (imggray_copy2_threshold.at<uchar>(i, j) > 0)
           {
               if(real_depth.at<short>(i, j) != 0)
               {
                   sum_2 += double(real_depth.at<short>(i, j));
                   //cout << real_depth.at<short>(i, j);
                   kk++;
               }

           }
   }
   double depth_bottom = double(sum_2 / kk);

   double height = abs(depth_top-depth_bottom) ;

   //获取所需轮廓外接矩形
   CvBox2D box = minAreaRect(contours[thearea]);
   //DrawBox(box, input);
   box.size.height = box.size.height / sqrt(box.size.height * box.size.width / areabiggest);
   box.size.width  = box.size.width  / sqrt(box.size.height * box.size.width / areabiggest);

   double pra = 0.72 * double(sqrt(double(1) / double(input1.rows * input1.cols)));
   box.size.width = pra * box.size.width * depth_top;
   box.size.height= pra * box.size.height * depth_top;
   double cube[4];
   cube[0] = box.size.width > box.size.height? box.size.width:box.size.height -5;
   //cube[1] = box.size.height;
   cube[1] = box.size.width > box.size.height? box.size.height:box.size.width;
   cube[2] = height;
   cube[3] = areabiggest*0.72*0.72*depth_top*depth_top / (640 * 480) * height;

   int cube_int[4];
   cube_int[0] = int(cube[0]); cube_int[1] = int(cube[1]); cube_int[2] = int(cube[2]); cube_int[3] = int(cube[3]);
    //最后需要根据实际深度将实际长度和宽度采集出来


   s_result = to_string(cube_int[0]/10)+"."+to_string(cube_int[0]%10)+
           "," +to_string(cube_int[1]/10)+"."+to_string(cube_int[1]%10)+
           ","+to_string(cube_int[2]/10)+"."+to_string(cube_int[2]%10)+
           ","+to_string(cube_int[3]/1000)+"."+to_string(cube_int[3]%1000);

    //delete []input_buf;

    // s_result = to_string(contours.size());
    //s_result = "one: "+to_string(count[0]) + "two: "+ to_string(count[1]) + "third: "+ to_string(count[2]);
    env->ReleaseIntArrayElements(pixels_, cbuf, 0);
    env->ReleaseByteArrayElements(yuyv_frame, bbuf, 0);
    return env->NewStringUTF(s_result.c_str());
}