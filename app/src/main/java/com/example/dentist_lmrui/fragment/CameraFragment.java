package com.example.dentist_lmrui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.dentist_lmrui.R;
import com.example.tencent.yolov8ncnn.Yolov8Ncnn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraFragment extends Fragment implements SurfaceHolder.Callback
{
    public static final int REQUEST_CAMERA = 100;
    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();
    private int facing = 0;
    private Spinner modelSpinner;
    private Spinner cpuGpuSpinner;
    private Button switchCameraButton;
    private int current_model = 4;
    private int current_cpugpu = 4;
    private SurfaceView cameraView;
    SurfaceHolder cameraViewHolder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        //屏幕常亮
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //相机空间初始化
        cameraView = view.findViewById(R.id.cameraview);
        cameraViewHolder = cameraView.getHolder();
        cameraViewHolder.setFormat(PixelFormat.RGBA_8888);
        cameraViewHolder.addCallback(this);
        //摄像头按钮初始化
        switchCameraButton = view.findViewById(R.id.buttonSwitchCamera);
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                int new_facing = 1 - facing;

                yolov8ncnn.closeCamera();

                yolov8ncnn.openCamera(new_facing);

                facing = new_facing;
            }
        });
        //模型列表选项初始化
        modelSpinner = view.findViewById(R.id.spinnerModel);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.model_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        // 设置默认值
        int defaultPosition = adapter.getPosition("n-teethv8");
        modelSpinner.setSelection(defaultPosition);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                Log.e("lmrui","11");
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
    //相机画面更新时，更新控件显示
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        yolov8ncnn.setOutputWindow(holder.getSurface());
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }
    @Override
    public void onResume()
    {
        super.onResume();
        //相机权限申请
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
        yolov8ncnn.openCamera(facing);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        //关闭相机
        yolov8ncnn.closeCamera();
    }
}
