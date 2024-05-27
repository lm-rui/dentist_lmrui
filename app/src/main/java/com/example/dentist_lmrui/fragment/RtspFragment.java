package com.example.dentist_lmrui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.dentist_lmrui.R;
import com.example.dentist_lmrui.player.SampleVideo;
import com.example.tencent.yolov8ncnn.Yolov8Ncnn;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RtspFragment extends Fragment implements SampleVideo.OnScreenShotListener
{
    private static final int PERMISSION_REQUEST_CODE = 123;
    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();
    private Spinner modelSpinner;
    private Spinner cpuGpuSpinner;
    private ImageView imageView;
    private int current_model = 0;
    private int current_cpugpu = 0;
    SampleVideo sampleVideo;
    GSYVideoOptionBuilder gsyVideoOption;
    private boolean isPlay;
    private boolean isPause;
    private String resultUrl;
    private Bitmap resultImage;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e("lmrui", "oncreate");
        View view = inflater.inflate(R.layout.fragment_rtsp, container, false);
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，则请求权限
            Log.e("lmrui", "申请权限");
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
        //屏幕常亮
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //播放器初始化
        sampleVideo = (SampleVideo) view.findViewById(R.id.video_player);
        //获取监听器
        sampleVideo.setOnScreenShotListener(this);
        init();
        //图片视图初始化
        imageView = view.findViewById(R.id.imageView);
        //模型列表选项初始化
        modelSpinner = view.findViewById(R.id.spinnerModel);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_model)
                {
                    current_model = position;
                    Log.e("lmrui", "current_model=" + current_model);
                    reload();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });
        //CPU-GPU列表选项
        cpuGpuSpinner = view.findViewById(R.id.spinnerCPUGPU);
        cpuGpuSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_cpugpu)
                {
                    current_cpugpu = position;
                    reload();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });
        //初始化加载模型。
        reload();
        return view;
    }
    //模型加载
    private void reload() {
        boolean ret_init = yolov8ncnn.loadModel(getActivity().getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov8ncnn loadModel failed");
        }
    }
    //播放器初始化
    private void init() {
    //URL
        //String url = "file:///storage/emulated/0/DCIM/Camera/share_2a823fd118d070c49196d08a91964e3f.mp4";
        //String url = "file:///storage/emulated/0/Pictures/QQ/oceans.mp4";
        String url = "file:///storage/emulated/0/Pictures/QQ/根管治疗现场版.346722281.mp4";
        //String url = "rtsp://rtsp-test-server.viomic.com:554/stream";
        //String url = "http://pic.nalaos.com/presentation/%e5%86%85%e9%83%a8%e9%87%87%e9%94%80%e5%87%86%e5%a4%87%e5%b7%a5%e4%bd%9c.mp4";
        //增加title
        sampleVideo.getTitleTextView().setVisibility(View.GONE);
        //添加按钮
        sampleVideo.getBackButton().setVisibility(View.GONE);
        //rtsp配置
        List<VideoOptionModel> list = new ArrayList<>();
        VideoOptionModel videoOptionModel1 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", -1);//清除 DNS 缓存。
        VideoOptionModel videoOptionModel2 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1);//控制是否开启数据包缓冲。
        VideoOptionModel videoOptionModel3 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1);//控制是否限制输入缓存数。
        VideoOptionModel videoOptionModel5 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzedmaxduration", 100);//分析码流时长。
        VideoOptionModel videoOptionModel6 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_flags", "prefer_tcp");//优先使用 TCP 协议进行 RTSP 传输
        VideoOptionModel videoOptionModel7 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 3);//设置最小帧数。
        VideoOptionModel videoOptionModel8 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 30);//设置丢帧率。
        VideoOptionModel videoOptionModel9 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1);//刷新数据包。
        VideoOptionModel videoOptionModel11 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fast", 1);//开启快速模式。
        VideoOptionModel videoOptionModel12 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 4096);//设置探测数据大小。
        VideoOptionModel videoOptionModel13 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);//准备好后自动开始播放。
        VideoOptionModel videoOptionModel14 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 30);//最大缓存时长
        VideoOptionModel videoOptionModel15 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);//跳过循环滤波器。
        VideoOptionModel videoOptionModel16 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);//开启硬解
        VideoOptionModel videoOptionModel17 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);//自动旋转硬解。
        VideoOptionModel videoOptionModel18 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);//处理分辨率变化。
        VideoOptionModel videoOptionModel19 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"reconnect",-1);// 播放重连次数
        VideoOptionModel videoOptionModel20 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 30);//设置最大帧率。
        VideoOptionModel videoOptionModel21 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1);//开启 HEVC 硬解。
        VideoOptionModel videoOptionModel22 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fps", 30);//设置帧率
        VideoOptionModel videoOptionModel23 = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);//开启精准定位。
        list.add(videoOptionModel1);//清除dns缓存
        list.add(videoOptionModel2);//是否开启缓冲
        list.add(videoOptionModel3);//是否限制输入缓存数
        list.add(videoOptionModel5);//分析码流时长。
        list.add(videoOptionModel6);//优先使用 TCP 协议进行 RTSP 传输
        list.add(videoOptionModel7);//设置最小帧数
//        list.add(videoOptionModel8);//设置丢帧率。
        list.add(videoOptionModel9);//刷新数据包
        list.add(videoOptionModel11);//开启快速模式。
//        list.add(videoOptionModel12);//设置探测数据大小。
        list.add(videoOptionModel13);//准备好后自动开始播放。
        list.add(videoOptionModel14);//最大缓存时长
//        list.add(videoOptionModel15);//跳过循环滤波器。
        list.add(videoOptionModel16);//开启硬解
        list.add(videoOptionModel17);//自动旋转硬解。
        list.add(videoOptionModel18);//处理分辨率变化。
        list.add(videoOptionModel19);// 播放重连次数
//        list.add(videoOptionModel20);//设置最大帧率。
        list.add(videoOptionModel21);//开启 HEVC 硬解。
//        list.add(videoOptionModel22);//设置帧率
//        list.add(videoOptionModel23);//开启精准定位。
        GSYVideoManager.instance().setOptionModelList(list);
        gsyVideoOption = new GSYVideoOptionBuilder();
        gsyVideoOption.setIsTouchWiget(true)//滑动界面改变进度
                .setNeedLockFull(true)//锁定屏幕功能
                .setUrl(url)
                .setCacheWithPlay(false)//是否边缓存
                .setVideoTitle("视频流播放")
                .setLooping(true)//设置循环
                .setRotateViewAuto(true)//自动旋转
                .setVideoAllCallBack(new GSYSampleCallBack() {
                    @Override
                    public void onPrepared(String url, Object... objects) {
                        super.onPrepared(url, objects);
                        //开始播放了才能旋转和全屏
                        //orientationUtils.setEnable(detailPlayer.isRotateWithSystem());
                        isPlay = true;
                    }
                    @Override
                    public void onQuitFullscreen(String url, Object... objects) {
                        super.onQuitFullscreen(url, objects);
                        // ------- ！！！如果不需要旋转屏幕，可以不调用！！！-------
                        // 不需要屏幕旋转，还需要设置 setNeedOrientationUtils(false)
//                        if (orientationUtils != null) {
//                            orientationUtils.backToProtVideo();
//                        }
                    }
                }).setLockClickListener(new LockClickListener() {
                    @Override
                    public void onClick(View view, boolean lock) {
//                        if (orientationUtils != null) {
//                            //配合下方的onConfigurationChanged
//                            orientationUtils.setEnable(!lock);
//                        }
                    }
                }).build(sampleVideo);
        sampleVideo.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleVideo.startWindowFullscreen(getContext(), true, true);
            }
        });
    }
    //当获取到截图
    @Override
    public void onScreenShot(Bitmap shotmap) {
        int w = shotmap.getWidth();
        int h = shotmap.getHeight();
        int[] pixels = new int[w * h];
        shotmap.getPixels(pixels, 0, w, 0, 0, w, h);
        resultUrl = yolov8ncnn.detectImage(pixels, w, h);
        Log.e("lmrui", "检测结束");
        resultImage = BitmapFactory.decodeFile(resultUrl);
        imageView.setImageBitmap(resultImage);
    }
    @Override
    public void onResume()
    {
        sampleVideo.getCurrentPlayer().onVideoResume(false);
        super.onResume();
        isPause = false;
        Log.e("lmrui", "onresume");
    }
    @Override
    public void onPause()
    {
        sampleVideo.getCurrentPlayer().onVideoPause();
        super.onPause();
        isPause = true;
        Log.e("lmrui", "onpause");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (isPlay) {
//            sampleVideo.getCurrentPlayer().release();
//        }
        Log.e("lmrui", "onpdestroy");
    }
}
