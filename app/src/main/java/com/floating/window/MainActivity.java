package com.floating.window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText mEditText;
    private SharedPreferences sharedPreferences;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("default", MODE_PRIVATE);
        ((MyApplication)getApplication()).setHandler(mHandler);

        findViewById(R.id.mainButton).setOnClickListener(clickListener);
        mEditText = findViewById(R.id.mainEditText);
        mEditText.setText(sharedPreferences.getString("pathname", ""));

    }

    View.OnClickListener clickListener = (v) -> {

            String pathname = mEditText.getText().toString();
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;

            if (checkOverlays() && hasPermission(permission) && !pathname.isEmpty()){
                sharedPreferences.edit().putString("pathname", pathname).commit();
                Intent intent = new Intent(MainActivity.this, TextureVideoActivity.class);
                intent.putExtra("pathname", pathname);
                startActivity(intent);
            }else {
                requestPermission(permission);
            }
    };

    public boolean checkOverlays(){
        boolean isCanOverlays = false;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(Settings.canDrawOverlays(this))
                isCanOverlays = true;
            else
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }else{
            isCanOverlays = true;
        }
        return isCanOverlays;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this))
                    Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "failure", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean hasPermission(String permission){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        else
            return true;
    }

    public void requestPermission(String permission){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(permission)){
                Toast.makeText(this, "request read sdcard permmission", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{permission},0);
        }
    }
}
