package com.example.dentist_lmrui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.dentist_lmrui.R;
import com.example.tencent.yolov8ncnn.Yolov8Ncnn;

import java.io.InputStream;

public class PictureFragment extends Fragment
{
    private static final int PERMISSION_REQUEST_CODE = 123;
    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();
    private Spinner modelSpinner;
    private Spinner cpuGpuSpinner;
    private Button inputPictureButton;
    private Button detectButton;
    private int current_model = 0;
    private int current_cpugpu = 0;
    private ImageView imageView;
    private ImageView resultView;
    private String imageUrl;
    private String resultUrl;
    private Bitmap sourceImage;
    private Bitmap resultImage;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_picture, container, false);
        //屏幕常亮
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //图片视图初始化
        imageView = view.findViewById(R.id.imageView);
        resultView = view.findViewById(R.id.resultView);
        //按钮初始化
        //导入图片按钮
        inputPictureButton = view.findViewById(R.id.inputPicture);
        inputPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                pickImageLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
            }
        });
        //开始检测按钮
        detectButton = view.findViewById(R.id.detectPicture);
        detectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 如果没有权限，则请求权限
                    Log.e("lmrui", "申请权限");
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                }
                // 已经有权限，加载图片
                if(imageUrl == null){
                    showAlertDialog();
                }
                else {
                    //在java打开图片，并传入c，在c中保存后即可读取（直接在c中读取没有权限，手机授权仅存在于java代码中）。
                    int w = sourceImage.getWidth();
                    int h = sourceImage.getHeight();
                    int[] pixels = new int[w * h];
                    sourceImage.getPixels(pixels, 0, w, 0, 0, w, h);
                    resultUrl = yolov8ncnn.detectImage(pixels, w, h);
                    Log.e("lmrui", "检测结束");
                    resultImage = BitmapFactory.decodeFile(resultUrl);
                    resultView.setImageBitmap(resultImage);
                }
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 初始化图片接收器
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == getActivity().RESULT_OK) {
                            Intent data = result.getData();
                            Uri selectedImageUri = data.getData();
                            if (selectedImageUri != null) {
                                try {
                                    // 使用ContentResolver打开InputStream来加载图片
                                    sourceImage = BitmapFactory.decodeStream(requireActivity().getContentResolver().openInputStream(selectedImageUri));
                                    // 将Uri转换为文件路径
                                    imageUrl = getImagePathFromUri(selectedImageUri);
                                    // 检查是否获取到了文件路径
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        imageView.setImageBitmap(sourceImage);//设置图片视图
                                    } else {
                                        Toast.makeText(requireActivity(), "Failed to get image path.", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(requireActivity(), "Failed to load the selected image.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(requireActivity(), "No image selected.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }
    //图片转URL
    private String getImagePathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = requireActivity().getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);
            }
        }
        return null;
    }
    //模型加载
    private void reload() {
        boolean ret_init = yolov8ncnn.loadModel(getActivity().getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov8ncnn loadModel failed");
        }
    }
    @Override
    public void onResume()
    {
        super.onResume();
        imageView.setImageBitmap(sourceImage);
        resultView.setImageBitmap(resultImage);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }
    //弹窗
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle("无法检测");
        builder.setMessage("请先打开图片");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 点击OK按钮后的操作，可以添加你的逻辑
                dialog.dismiss(); // 关闭弹窗
                pickImageLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));//启动图片选择器
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 点击Cancel按钮后的操作，可以添加你的逻辑
                dialog.dismiss(); // 关闭弹窗
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
