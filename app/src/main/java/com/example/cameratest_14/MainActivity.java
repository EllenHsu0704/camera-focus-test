package com.example.cameratest_14;
/*
1.4.1由原先的基礎上加入閃光燈及TextView，是參考CameraX_2.0完成
原本是要跟2.0版本有所區隔，但最終發現由此程式測試拍照速度，會比2.0版本快上100~200左右毫秒
所以為了求基準一致起見，便將2.0的新增項目及功能搬過來
兩者拍照速度上的差異原因，目前還不清楚
2.0使用計算時間的方法是System.nanoTime(); //納秒的計算，精度更高，但在2.0之中2種算法經測試差距不大，所以這個版本就保留了原本的System.currentTimeMillis()
此外，兩者都是使用CameraX的寫法，為了配合之前測試結果，所以統一還是以此方式計時
 */
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cameratest_14.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.camera.core.CameraX;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ViewPort;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding viewBinding;
    private ImageCapture imageCapture = null;
    //private VideoCapture<Recorder> videoCapture = null;
    //private Recording recording = null;
    private ExecutorService cameraExecutor;
    private String fileName;
    long startTime;
    Executor executor;
    TextView textview;
    private int flashLamp = 0;
    ImageButton clearButton,captureButton,btn_flash;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(Configuration.TAG, "[]+設立介面" );
        setTitle("CameraTest_1.4.1");
        //viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        //setContentView(viewBinding.getRoot());

       
        // 請求相機權限
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, Configuration.REQUIRED_PERMISSIONS,
                    Configuration.REQUEST_CODE_PERMISSIONS);
        }

        // 設置拍照按钮監聽
        textview = findViewById(R.id.textview);
        clearButton = findViewById(R.id.clearButton);
        btn_flash = findViewById(R.id.btn_flash);
        captureButton = findViewById(R.id.imageCaptureButton);
        //viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());
        //viewBinding.videoCaptureButton.setOnClickListener(v -> captureVideo());
        captureButton.setOnClickListener(v -> takePhoto());
        btn_flash.setOnClickListener(v -> takeFlash());

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 清除TextView的文字
                textview.setText("");
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();

    }
    @SuppressLint("CheckResult")


    private void takePhoto() {
        // 確保imageCapture 已經被實例化, 否则程序將可能崩溃
        // Calculate the time taken for the photo
        startTime = System.currentTimeMillis();
        Log.d(Configuration.TAG, "[]+成像實例化+放置計時器" );
        // Create output file to hold the image
        fileName = System.currentTimeMillis() + ".jpg";
        if (imageCapture != null) {
            // 創建帶時間戳的輸出文件以保存圖片，带時間戳是為了保證文件名唯一
            String name = new SimpleDateFormat(Configuration.FILENAME_FORMAT,
                    Locale.SIMPLIFIED_CHINESE).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
                Log.d(Configuration.TAG, "[]+建立儲存位置" );
            }

            // 創建 output option 對象，用以指定照片的输出方式。
            // 在這個對象中指定有關我們希望輸出如何的方式。我們希望將輸出保存在 MediaStore 中，以便其他應用可以顯示它
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                    .Builder(getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues)
                    .build();
            Log.d(Configuration.TAG, "[]+建立輸出" );


            // 設置拍照监听，用以在照片拍摄後執行takePicture（拍照）方法
            imageCapture.takePicture(outputFileOptions,
                    ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {// 保存照片时的回调
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            long timeTaken = System.currentTimeMillis() - startTime;//設置結束時間
                            Log.d(Configuration.TAG, "[]+結束計時" );
                            String msg = "照片拍攝成功! " + "----" + timeTaken + "ms";//outputFileResults.getSavedUri();
                            String msg3 = timeTaken + "ms";
                            Toast.makeText(getBaseContext(), msg , Toast.LENGTH_SHORT).show();

                            // 在UI線程上更新TextView
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String currentText = textview.getText().toString();
                                    String newText = currentText + "\n" + "計算結果： " + msg3;
                                    textview.setText(newText);
                                }
                            });

                            Log.d(Configuration.TAG, msg );

                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(Configuration.TAG, "Photo capture failed: " + exception.getMessage());
                        }
                    });
        }
    }

    private void takeFlash() {
        if (flashLamp == 0)
        {
            flashLamp = 1;
            imageCapture.setFlashMode(ImageCapture.FLASH_MODE_AUTO);
            Toast.makeText(getBaseContext(), "Flash AUTO", Toast.LENGTH_SHORT).show();
        }
        else if(flashLamp == 1)
        {
            flashLamp = 0;
            imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);
            Toast.makeText(getBaseContext(), "Flash Enable", Toast.LENGTH_SHORT).show();
        }
    }


    private void startCamera() {
        // 将Camera的生命周期和Activity绑定在一起（設定生命周期所有者），這樣就不用手動控制相機的啟動和關閉。
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // 将你的相機和當前生命周期的所有者绑定所需的對象
                ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();

                // 創建一个Preview 實例，並設置該實例的 surface 提供者（provider）。
                PreviewView viewFinder = (PreviewView)findViewById(R.id.viewFinder);
                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                Log.d(Configuration.TAG, "[]+創建Preview" );

                // 選擇後置鏡頭為默認鏡頭
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 創建拍照所需的實例
                //imageCapture = new ImageCapture.Builder().build();//此段是原本android範例程式碼，所需功能需在此添加
                imageCapture = new ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)//設置拍照高畫質模式
                                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)//設置低光源自動閃光燈模式
                                .build();
                Log.d(Configuration.TAG, "imageCapture" );


                // 設置預覽幀分析....下面3行是android範例程式碼，
                /*ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, new MyAnalyzer());
                 */

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(4608, 3456))  //設置相片尺寸
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
                Log.d(Configuration.TAG, "[]+設置預覽幀分析" );

                imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

                        imageProxy.close();
                    }
                });


                // 重新绑定用例前先解绑
                processCameraProvider.unbindAll();

                processCameraProvider.bindToLifecycle(MainActivity.this, cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                        //videoCapture
                );

            } catch (Exception e) {
                Log.e(Configuration.TAG, "绑定失敗！" + e);
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private boolean allPermissionsGranted() {
        for (String permission : Configuration.REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(Configuration.TAG, "[]+動態申請權限" );
        if (requestCode == Configuration.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {// 申请權限通過
                startCamera();
            } else {// 申请權限失敗
                Toast.makeText(this, "用戶拒绝授權！", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == Configuration.REQUEST_AUDIO_CODE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this,
                    "Manifest.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "用戶未授權！", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    static class Configuration {
        public static final String TAG = "CameraxBasic";
        public static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
        public static final int REQUEST_CODE_PERMISSIONS = 10;
        public static final int REQUEST_AUDIO_CODE_PERMISSIONS = 12;
        public static final String[] REQUIRED_PERMISSIONS =
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ?
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE} :
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO};
    }

    private static class MyAnalyzer implements ImageAnalysis.Analyzer{

        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void analyze(@NonNull ImageProxy image) {
            Log.d(Configuration.TAG, "Image's stamp is " + Objects.requireNonNull(image.getImage()).getTimestamp());
            image.close();
        }
    }

}