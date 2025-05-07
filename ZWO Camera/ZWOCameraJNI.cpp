#include <iostream>
#include <string>
#include <thread>
#include <ctime>
#include <chrono>
#include <atomic>
#include <ASICamera2.h> //IF YOU WISH TO LOOK AT CAMERA FUNCTIONS AND PROGRAMMING INFORMATION LOOK UP ZWO CAMERA SDK
#include <opencv2/opencv.hpp>  //this is needed to generate pictures and look at live video


#include <jni.h> //need this for java wrapper
#include "Main.h" //this header file comes from a Java file and is generated from javac -h header file generation

using namespace std;
using namespace cv;

atomic<bool> thread_end = false;

void capture(ASI_CAMERA_INFO*, int); //image capture function

JNIEXPORT jint JNICALL Java_Main_Capture(JNIEnv *env, jobject obj, jboolean vidFlag, jint capTimer) { //creates this into a native java function that can be used in java

//int main(void){
	int cameraCount; //to store the number of cameras detected
	ASI_CAMERA_INFO* ZWOCamera = (ASI_CAMERA_INFO*)malloc(sizeof(ASI_CAMERA_INFO)); //allocates memory for ASI_CAMERA_INFO struct

	bool liveVid = (bool)vidFlag; //converts java arguments into c++ variables
	int capTime = (int)capTimer;

	//bool liveVid = true; //enable live video feed
	//int capTime = 10; //captures images every 10 seconds


	cameraCount = ASIGetNumOfConnectedCameras(); //detects if an ASI camera is connected
	cout << "Number of cameras connected: ";
	cout << cameraCount << endl;
	
	ASIGetCameraProperty(ZWOCamera,0); //collects the properties of 1st connected ASI camera into a _ASI_CAMERA_INFO struct
	cout << "Camera: " << ZWOCamera->Name << endl; //Shows the camera name. For this project it should say ASI676MC
	if (cameraCount != 1) { //if no camera or more than 1 camera is connected
		cout << "No Camera/Too Many Cameras Connected!" << endl;
		return -1;
	}

	cout << "Image Resolution: " << ZWOCamera->MaxWidth << "x" << ZWOCamera->MaxHeight << endl; //you can view resolution here
	cout << "Pixel Size: " << ZWOCamera->PixelSize << endl; //view pixel size here

	int width = ZWOCamera->MaxWidth; //store image resolution in width and height variables
	int height = ZWOCamera->MaxHeight;

	if (ASIOpenCamera(ZWOCamera->CameraID) == ASI_SUCCESS) {  //opens the camera
		cout << "Camera Successfully Opened" << endl;
	}
	else {
		cout << "Camera Didn't Open!" << endl;
		return -1;
	}

	if (ASIInitCamera(ZWOCamera->CameraID) == ASI_SUCCESS) { //initializes the camera
		cout << "Camera Successfully Initialized" << endl;
	}
	else {
		cout << "Camera Didn't Initialize!" << endl;
		return -1;
	}

	ASISetROIFormat(ZWOCamera->CameraID, width, height, 1, ASI_IMG_RGB24); //sets the camera up with the max image size and in the color mode format
	ASISetStartPos(ZWOCamera->CameraID, 0, 0); //sets the starting position of the camera. 0,0 means the camera capture is set at the center of the camera I think?



	long exposure = 100000; //in microseconds(us). This is the initial exposure value
	long gain = 250; //don't set gain too high or the image will be grainy. This is the initial gain value

	long* curr_exposure = (long*)malloc(sizeof(long)); //the current exposure value is stored here
	long* curr_gain = (long*)malloc(sizeof(long)); //the current gain value is stored here from

	ASI_BOOL* exposure_auto = (ASI_BOOL*)malloc(sizeof(ASI_BOOL)); //store status of if exposure is auto-adjusted here
	ASI_BOOL* gain_auto = (ASI_BOOL*)malloc(sizeof(ASI_BOOL)); //store the status of if gain is auto-adjusted here

	ASISetControlValue(ZWOCamera->CameraID, ASI_EXPOSURE, exposure, ASI_TRUE); //exposure of camera is initially set at  100000 microseconds. ASI_TRUE means exposure will auto-adjust
	//NOTE: If exposure becomes too long, the program will fail. I think it's because the code tries to capture an image that hasn't been made yet due to long exposure times. Be careful of this and adjust gain and exposure accordingly
	ASISetControlValue(ZWOCamera->CameraID, ASI_GAIN, gain, ASI_FALSE); //gain is initially set at 100. ASI_FALSE means gain won't auto-adjust.

	//cout << exposure << endl;
	//cout << gain << endl;

	//these functions are called continuously in the image capture while loop below
	ASIGetControlValue(ZWOCamera->CameraID, ASI_EXPOSURE, curr_exposure, exposure_auto); //this function is used to store the exposure and auto status
	ASIGetControlValue(ZWOCamera->CameraID, ASI_GAIN, curr_gain, gain_auto); //this function is used to store the gain and auto status

	//cout << *curr_exposure << endl;
	//cout << *curr_gain << endl;

	if (ASISetCameraMode(ZWOCamera->CameraID, ASI_MODE_NORMAL) == ASI_SUCCESS) { //set the camera
		cout << "Camera Mode Successfully Set" << endl;
	}
	else {
		cout << "Camera Mode Wasn't Set Properly!" << endl;
		return -1;
	}

	
	if (ASIStartVideoCapture(ZWOCamera->CameraID) == ASI_SUCCESS) { //start video capture
		cout << "Camera Video Successfully Started" << endl;
	}
	else {
		cout << "Camera Video Didn't Start!" << endl;
		return -1;
	}
	ASISetControlValue(ZWOCamera->CameraID, ASI_HIGH_SPEED_MODE, 1, ASI_FALSE); //sets the camera in high speed mode, which I think means the camera will work harder to bring higher performance (faster capture rates)
	
	Mat image(width, height, CV_8UC3); //opencv is used for image capture. The images captured by the camera are stored here. CV_8UC3 describes the type of image that the image variable is, which is a 3-channel 8-bit image
	Mat window; //for the video feed
	thread tCap(capture, ZWOCamera, capTime); //starts a thread for the video feed. So images can be captured and a video feed can happen at the same time.
	while (true) {
		if (thread_end == true) { //if image capture ends, the thread joins back and the program ends. This needs to be on at all times, so if this ends that means something messed up with the image capture
			tCap.join();
			cout << "Thread ended!" << endl;
			break;
		}
		if (liveVid == true) { //if the users wish to have live video enabled, this is if statement will occur and a window will open with the live feed in it
			if ((ASIGetVideoData(ZWOCamera->CameraID, image.data, width * height * 3, ASI_EXPOSURE * 2 + 500)) == ASI_SUCCESS) { //Captures an image which is stored in the image variable

				//cout << "Feed Started" << endl;

				resize(image, window, Size(1920, 1080)); //stores data from the image variable in the window variable, and the window variable is set for a 1920 x 1080 monitor
				imshow("Live Feed", window); //a 1920 x 1080 window will open up showing the live video feed
				waitKey(0); //this means the window will continuously update I think

				ASIGetControlValue(ZWOCamera->CameraID, ASI_EXPOSURE, curr_exposure, exposure_auto); //continuously stores the current exposure and if its on auto adjust
				ASIGetControlValue(ZWOCamera->CameraID, ASI_GAIN, curr_gain, gain_auto); //continuously stores the current gain and if its on auto adjust
				cout << *curr_exposure << endl; //shows current exposure
				cout << *curr_gain << endl; //shows current gain

			}
			else {
				cout << "Didn't Get Video Data!" << endl;
				break;
			}
		}
	}
	

	if (ASIStopVideoCapture(ZWOCamera->CameraID) == ASI_SUCCESS) { //stops video capture
		cout << "Camera Video Successfully Stopped" << endl;
	}
	else {
		cout << "Camera Video Didn't Stop!" << endl;
		return -1;
	}
	

	if (ASICloseCamera(ZWOCamera->CameraID) == ASI_SUCCESS) { //closes camera
		cout << "Camera Successfully Closed" << endl;
	}
	else {
		cout << "Camera Didn't Close!" << endl;
		return -1;
	}
	
	return 0;
}

void capture(ASI_CAMERA_INFO* ZWOCamera, int captureTime) { //this function is for capturing the images
	struct tm date; //all of this is to create a timestamp for the image
	__time64_t timestamp;
	string timestring;
	string image_type = ".png";
	char timebuff[50];
	string buffstring;

	int width = ZWOCamera->MaxWidth; //image resolution
	int height = ZWOCamera->MaxHeight;

	Mat image(width, height, CV_8UC3); //image variable to store camera capture in
	while (true) {
		if ((ASIGetVideoData(ZWOCamera->CameraID, image.data, width * height * 3, ASI_EXPOSURE * 2 + 500)) == ASI_SUCCESS) { //gets data from camera and stores it in capture variable
			_time64(&timestamp); //gets the time data
			_localtime64_s(&date, &timestamp); //configures time data in local time
			asctime_s(timebuff, 50, &date); //converts time data into a readable format (year, month, day, day of the week, hours, min, seconds)
			timebuff[strlen(timebuff) - 1] = '\0';//gets rid of a newline in the string
			buffstring = timebuff; //this is done due to program issues with variable type
			replace(buffstring.begin(), buffstring.end(), ':', '-'); //imwrite won't create files with colons in it. replaces colons with dashes
			replace(buffstring.begin(), buffstring.end(), ' ', '-'); //replaces spaces with dashes
			timestring = buffstring + image_type; //this is the complete name of the image
			cout << "Video Data Is Captured!" << endl;
			//cout << timestring << endl;
			imwrite(timestring, image); //stores the image in a .png file. Need to figure out how to write these images to an absolute file path, but it does store the image locally in the same folder as the coding project
		}
		else {
				cout << "Didn't Get Video Data!" << endl;
				thread_end = true;
				break;
		}
		this_thread::sleep_for(chrono::seconds(captureTime)); //this is for capture rates. If the user wants it capture an image every 60 seconds or 60 minutes, you change the captureTime variable
	}
}
