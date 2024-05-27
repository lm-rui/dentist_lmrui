// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.
#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include <opencv2/opencv.hpp>
#include <iomanip>
#include <opencv2/core/types_c.h>

#include "yolo.h"
#include "yolo-seg.h"
#include "ndkcamera.h"


#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

static Yolo* g_yolo = 0;
static Yolo_seg* g_yolo_seg = 0;
static ncnn::Mutex lock;

class MyNdkCamera : public NdkCameraWindow
{
public:
    virtual void on_image_render(cv::Mat& rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{
    // nanodet
    {
        ncnn::MutexLockGuard g(lock);
        //Detect推理与后处理
        if (g_yolo)
        {
            std::vector<Object> objects;
            g_yolo->detect(rgb, objects);
            g_yolo->draw(rgb, objects);
            __android_log_print(ANDROID_LOG_ERROR, "lmrui", "draw-yolo");
        }
        //Segment推理与后处理
        else if(g_yolo_seg)
        {
            std::vector<Object_seg> objects;
            g_yolo_seg->detect(rgb, objects);
            g_yolo_seg->draw(rgb, objects);
            __android_log_print(ANDROID_LOG_ERROR, "lmrui", "draw-yoloseg");
        }
        else
        {
            draw_unsupported(rgb);
        }
    }
    draw_fps(rgb);
}

static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);
        if (g_yolo)
            delete g_yolo;
        if (g_yolo_seg)
            delete g_yolo_seg;
        g_yolo = 0;
        g_yolo_seg = 0;
    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL Java_com_example_tencent_yolov8ncnn_Yolov8Ncnn_loadModel
(JNIEnv *env, jobject thiz,jobject assetManager, jint modelid,jint cpugpu) {
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_ERROR, "lmrui", "loadModel-modelid %d", modelid);

    const char *modeltypes[] =
            {
//                    "n",
//                    "s",
//                    "n-seg",
//                    "s-seg",
                    "n-teethv8",
                    "s-teeth-segv2"
            };

    const int target_sizes[] =
            {
                    320,
                    320,
                    320,
                    320,
                    320,
                    320,
            };

    const float mean_vals[][3] =
            {
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
            };

    const float norm_vals[][3] =
            {
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
                    {1 / 255.f, 1 / 255.f, 1 / 255.f},
            };

    const char *modeltype = modeltypes[(int) modelid];
    int target_size = target_sizes[(int) modelid];
    bool use_gpu = (int) cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);
        //使用GPU
        if (use_gpu && ncnn::get_gpu_count() == 0) {
            __android_log_print(ANDROID_LOG_ERROR, "lmrui", "reload gpu %d", use_gpu);
            // no gpu
            if (g_yolo)
                delete g_yolo;
            if (g_yolo_seg)
                delete g_yolo_seg;
            g_yolo = 0;
            g_yolo_seg = 0;
        }
            //使用CPU
        else {
            __android_log_print(ANDROID_LOG_ERROR, "lmrui", "reload cpu%d", use_gpu);
//            //加载Detect模型
//            if (!g_yolo && (int) modelid < 2)
//                g_yolo = new Yolo;
//            if ((int) modelid < 2) {
//                __android_log_print(ANDROID_LOG_ERROR, "lmrui", "load-yolo %d", modelid);
//                delete g_yolo_seg;
//                g_yolo_seg = 0;
//                g_yolo->load(mgr, modeltype, target_size, mean_vals[(int) modelid],
//                             norm_vals[(int) modelid], use_gpu);
//            }
//            //加载Segment模型
//            if (!g_yolo_seg && (int) modelid >= 2 && (int) modelid < 4)
//                g_yolo_seg = new Yolo_seg;
//            if ((int) modelid >= 2 && (int) modelid < 4) {
//                __android_log_print(ANDROID_LOG_ERROR, "lmrui", "load-yoloseg %d", modelid);
//                delete g_yolo;
//                g_yolo = 0;
//                g_yolo_seg->load(mgr, modeltype, target_size, mean_vals[(int) modelid],
//                                 norm_vals[(int) modelid], use_gpu);
//            }
            //加载Detect-teeth模型
            if (!g_yolo && (int) modelid == 0)
                g_yolo = new Yolo;
            if ((int) modelid == 0) {
                __android_log_print(ANDROID_LOG_ERROR, "lmrui", "load-yolo-teeth %d", modelid);
                delete g_yolo_seg;
                g_yolo_seg = 0;
                g_yolo->load(mgr, modeltype, target_size, mean_vals[(int) modelid],
                                 norm_vals[(int) modelid], use_gpu);
            }
            //加载Segment-teeth模型
            if (!g_yolo_seg && (int) modelid == 1)
                g_yolo_seg = new Yolo_seg;
            if ((int) modelid == 1) {
                __android_log_print(ANDROID_LOG_ERROR, "lmrui", "load-yoloseg-teeth %d", modelid);
                delete g_yolo;
                g_yolo = 0;
                g_yolo_seg->load(mgr, modeltype, target_size, mean_vals[(int) modelid],
                                 norm_vals[(int) modelid], use_gpu);
            }
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL Java_com_example_tencent_yolov8ncnn_Yolov8Ncnn_openCamera(JNIEnv *env, jobject thiz, jint facing) {
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);
    //前后置摄像头
    g_camera->open((int) facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL Java_com_example_tencent_yolov8ncnn_Yolov8Ncnn_closeCamera(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_com_example_tencent_yolov8ncnn_Yolov8Ncnn_setOutputWindow(JNIEnv *env, jobject thiz,jobject surface) {
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_com_example_tencent_yolov8ncnn_Yolov8Ncnn_detectImage(JNIEnv *env, jobject thiz,jintArray buf, int w, int h) {
    //*----------------------图片原地TP-start----------------------*/
    //数组转mat
    jint *cbuf;
    cbuf = env->GetIntArrayElements(buf, JNI_FALSE);
    if (cbuf == NULL) {
        return 0;
    }
    cv::Mat rgb(h, w, CV_8UC4, (unsigned char *) cbuf);
    //相册路径
    std::string directory = "/storage/emulated/0/DCIM/Camera";
    // 获取当前时间
    std::time_t rawtime = std::time(nullptr);
    std::tm *timeinfo = std::localtime(&rawtime);
    std::stringstream ss;
    ss << std::put_time(timeinfo, "%Y%m%d%H%M%S");
    std::string sourceTime = ss.str();
    // 在目录路径下创建结果图片的路径
    std::string sourceImagePath = directory + "source" + sourceTime + ".jpg";
    cv::imwrite(sourceImagePath, rgb);
    /*----------------------图片原地TP-end-------------------*/
    cv::Mat resultImage = cv::imread(sourceImagePath,1);
    //ncnn::MutexLockGuard g(lock);
    //Detect推理与后处理
    if (g_yolo) {
        std::vector<Object> objects;
        g_yolo->detect(resultImage, objects);
        g_yolo->draw(resultImage, objects);
        __android_log_print(ANDROID_LOG_ERROR, "lmrui", "draw-yolo");
    }
    //Segment推理与后处理
    else if (g_yolo_seg) {
        std::vector<Object_seg> objects;
        g_yolo_seg->detect(resultImage, objects);
        g_yolo_seg->draw(resultImage, objects);
        __android_log_print(ANDROID_LOG_ERROR, "lmrui", "draw-yoloseg");
    } else {
        draw_unsupported(resultImage);
    }
    //draw_fps(resultImage);
    std::string detectImagePath = directory + "detect" + sourceTime + ".jpg";
    cv::imwrite(detectImagePath, resultImage);
    jstring resultUrl;
    resultUrl = env->NewStringUTF(detectImagePath.c_str());
    env->ReleaseIntArrayElements(buf, cbuf, 0);
    return resultUrl;

}


}