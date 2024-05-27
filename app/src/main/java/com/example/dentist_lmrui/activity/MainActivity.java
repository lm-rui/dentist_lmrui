package com.example.dentist_lmrui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;

import com.example.dentist_lmrui.R;
import com.example.dentist_lmrui.fragment.PictureFragment;
import com.example.dentist_lmrui.fragment.CameraFragment;
import com.example.dentist_lmrui.fragment.RtspFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 * MainActivity 是一个演示动态管理 Fragment 的示例，已根据个人需求进行了修改。
 *
 * 主要功能：
 * - 底部导航栏切换不同的 Fragment
 * - 使用 HashMap 存储菜单项和对应的 Fragment
 * - 实现灵活的动态管理功能
 *
 * 使用说明：
 * - 添加新的 Fragment 需在 List 中进行修改
 * - 添加新的菜单项和对应的 Fragment 只需在 HashMap 中进行修改
 * - 修改界面布局或其他功能可以在对应的方法中进行
 *
 * 注意事项：
 * - 确保布局文件中包含了底部导航栏（BottomNavigationView）的定义
 *
 * 修改说明：
 * - 2024-04-16: 修改了底部导航栏切换 Fragment 的逻辑，使用 HashMap 管理 Fragment，实现更灵活和可扩展的动态管理
 */
public class MainActivity extends AppCompatActivity{

    List<Fragment> list;
    HashMap<Integer, Fragment> map;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        list = new ArrayList<>();
        list.add(new PictureFragment());
        list.add(new CameraFragment());
        list.add(new RtspFragment());

        // 使用HashMap存储菜单项和Fragment的对应关系
        map = new HashMap<>();
        map.put(R.id.menu_picture, list.get(0));
        map.put(R.id.menu_camera, list.get(1));
        map.put(R.id.menu_rtsp, list.get(2));

        //showFragment(list.get(0));
        showFragment(map.get(R.id.menu_picture));

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            //使用一个 HashMap 来存储每个菜单项对应的 Fragment，然后根据选中的菜单项来动态获取对应的 Fragment。
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                Fragment fragment = map.get(itemId);
                if(fragment != null) {
                    showFragment(fragment);
                    return true;
                }
                return false;
            }
        });

    }

    private void showFragment(Fragment fragment){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_main,fragment);
        ft.commit();

    }
}

