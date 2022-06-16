package com.example.flutter_fanfq_paddlelite;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.renderscript.RenderScript;
import android.util.Log;
import android.widget.Toast;

import com.example.flutter_fanfq_paddlelite.config.Config;
import com.example.flutter_fanfq_paddlelite.preprocess.Preprocess;
import com.example.flutter_fanfq_paddlelite.visual.Visualize;


import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {

    final static String TAG = "MainActivity";

    // 模型配置
    Config config = new Config();
    protected Predictor predictor = new Predictor();
    Preprocess preprocess = new Preprocess();
    Visualize visualize = new Visualize();

    private static final String CHANNEL = "paddlelite";
    private static boolean modalLoaded = false;
    private RenderScript rs;

    //定义图像来源
    public static final int OPEN_GALLERY_REQUEST_CODE = 0;//本地相册
    public static final int TAKE_PHOTO_REQUEST_CODE = 1;//摄像头拍摄

    //定义模型推理相关变量
    public static final int REQUEST_LOAD_MODEL = 0;
    public static final int REQUEST_RUN_MODEL = 1;
    public static final int RESPONSE_LOAD_MODEL_SUCCESSED = 0;
    public static final int RESPONSE_LOAD_MODEL_FAILED = 1;
    public static final int RESPONSE_RUN_MODEL_SUCCESSED = 2;
    public static final int RESPONSE_RUN_MODEL_FAILED = 3;

    static final String METHOD_REQUEST_LOAD_MODEL = "METHOD_REQUEST_LOAD_MODEL";
    static final String METHOD_REQUEST_RUN_MODEL = "METHOD_REQUEST_RUN_MODEL";
    static final String METHOD_RESPONSE_LOAD_MODEL_SUCCESSED = "METHOD_RESPONSE_LOAD_MODEL_SUCCESSED";
    static final String METHOD_RESPONSE_LOAD_MODEL_FAILED = "METHOD_RESPONSE_LOAD_MODEL_FAILED";
    static final String METHOD_RESPONSE_RUN_MODEL_SUCCESSED = "METHOD_RESPONSE_RUN_MODEL_SUCCESSED";
    static final String METHOD_RESPONSE_RUN_MODEL_FAILED = "METHOD_RESPONSE_RUN_MODEL_FAILED";

    //定义操作流程线程句柄
    protected HandlerThread worker = null; // 工作线程（加载和运行模型）
    protected Handler receiver = null; // 接收来自工作线程的消息
    protected Handler sender = null; // 发送消息给工作线程


    /**
     * Native 返回字节数组，flutter侧使用 Uint8List
     * @param imageName
     * @return
     */
    private byte[] getImageAsBytes(String imageName){

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try{
            InputStream imageStream = getAssets().open("image_matting/images/human.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            bitmap.compress(Bitmap.CompressFormat.PNG,80,outputStream);
        }catch (Exception e){
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    MethodChannel channel;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        BinaryMessenger messenger = flutterEngine.getDartExecutor().getBinaryMessenger();
        channel = new MethodChannel(messenger,"flutter/channel");

        channel.setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {

                Log.i(TAG,call.method);

                switch (call.method){
                    case METHOD_REQUEST_RUN_MODEL:

                        try{
                            byte[] bytes = call.argument("file");

                            Bitmap image = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                            Bitmap bg = null;

                            //加载背景图像
                            if (!config.bgPath.substring(0, 1).equals("/")) {
                                InputStream imageStream = getAssets().open(config.bgPath);
                                bg = BitmapFactory.decodeStream(imageStream);
                            } else {
                                if (!new File(config.bgPath).exists()) {
                                    return;
                                }
                                bg = BitmapFactory.decodeFile(config.bgPath);
                            }

                            if (image != null && bg != null && predictor.isLoaded()) {
                                predictor.setInputImage(image,bg);
                                runModel();//开始推理
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }


                        break;
                }

//                String filepath = call.argument("file");
//
//                Log.i(TAG,"filepath:"+filepath);
//
//                byte[] bytes = getImageAsBytes(filepath);
//                if(bytes!=null && bytes.length>0){
//                    result.success(bytes);
//                }else{
//                    result.error("-1","图片加载失败",null);
//                }
            }
        });

//        rs = RenderScript.create(this);
//
//        Log.e(TAG,"configureFlutterEngine");
//
//        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
//                .setMethodCallHandler(
//                        (call, result) -> {
//                            if (call.method.equals("loadModel")) {
//                                loadModel(result);
//                            } else if(call.method.equals("detectObject")){
//                                HashMap image = call.arguments();
//                                detectObject(image,result);
//                            } else {
//                                result.notImplemented();
//                            }
//                        }
//                );
    }

    @Override
    protected void onResume() {
        Log.i(TAG,"begin onResume");
        super.onResume();

        boolean settingsChanged = false;
        String model_path = "image_matting/models/modnet";
        String label_path = "image_matting/labels/label_list";
        String image_path = "image_matting/images/human.jpg";
        String bg_path = "image_matting/images/bg.jpg";
        settingsChanged |= !model_path.equalsIgnoreCase(config.modelPath);
        settingsChanged |= !label_path.equalsIgnoreCase(config.labelPath);
        settingsChanged |= !image_path.equalsIgnoreCase(config.imagePath);
        settingsChanged |= !bg_path.equalsIgnoreCase(config.bgPath);
        int cpu_thread_num = 1;
        settingsChanged |= cpu_thread_num != config.cpuThreadNum;
        String cpu_power_mode = "LITE_POWER_HIGH";
        settingsChanged |= !cpu_power_mode.equalsIgnoreCase(config.cpuPowerMode);
        String input_color_format = "RGB";
        settingsChanged |= !input_color_format.equalsIgnoreCase(config.inputColorFormat);
        long[] input_shape = new long[]{1,3,256,256};

        settingsChanged |= input_shape.length != config.inputShape.length;

        if (!settingsChanged) {
            for (int i = 0; i < input_shape.length; i++) {
                settingsChanged |= input_shape[i] != config.inputShape[i];
            }
        }

        if (settingsChanged) {
            config.init(model_path,label_path,image_path,bg_path,cpu_thread_num,cpu_power_mode,
                    input_color_format,input_shape);
            preprocess.init(config);
            // 更新UI
            //tvInputSetting.setText("算法模型: " + config.modelPath.substring(config.modelPath.lastIndexOf("/") + 1));
            //tvInputSetting.scrollTo(0, 0);
            // 如果配置发生改变则重新加载模型并预测
            loadModel();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG,"begin onDestroy");
        if (predictor != null) {
            predictor.releaseModel();
        }
        worker.quit();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"begin onCreate");

        //定义消息接收线程
        receiver = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case RESPONSE_LOAD_MODEL_SUCCESSED:
                        //pbLoadModel.dismiss();
                        channel.invokeMethod(METHOD_RESPONSE_LOAD_MODEL_SUCCESSED,"Load model successed!");
                        onLoadModelSuccessed();
                        break;
                    case RESPONSE_LOAD_MODEL_FAILED:
                        //pbLoadModel.dismiss();
                        channel.invokeMethod(METHOD_RESPONSE_LOAD_MODEL_FAILED,"Load model failed!");
                        //Toast.makeText(MainActivity.this, "Load model failed!", Toast.LENGTH_SHORT).show();
                        onLoadModelFailed();
                        break;
                    case RESPONSE_RUN_MODEL_SUCCESSED:
                        //pbRunModel.dismiss();
                        channel.invokeMethod(METHOD_RESPONSE_RUN_MODEL_SUCCESSED,"Run model successed!");
                        onRunModelSuccessed();
                        break;
                    case RESPONSE_RUN_MODEL_FAILED:
                        //pbRunModel.dismiss();
                        //通知 flutter 跟新
                        channel.invokeMethod(METHOD_RESPONSE_RUN_MODEL_FAILED,"Run model failed!");
                        //Toast.makeText(MainActivity.this, "Run model failed!", Toast.LENGTH_SHORT).show();
                        onRunModelFailed();
                        break;
                    default:
                        break;
                }
            }
        };

        //定义工作线程
        worker = new HandlerThread("Predictor Worker");
        worker.start();

        //定义发送消息线程
        sender = new Handler(worker.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case REQUEST_LOAD_MODEL:
                        // load model and reload test image
                        if (onLoadModel()) {
                            receiver.sendEmptyMessage(RESPONSE_LOAD_MODEL_SUCCESSED);
                        } else {
                            receiver.sendEmptyMessage(RESPONSE_LOAD_MODEL_FAILED);
                        }
                        break;
                    case REQUEST_RUN_MODEL:
                        // run model if model is loaded
                        if (onRunModel()) {
                            receiver.sendEmptyMessage(RESPONSE_RUN_MODEL_SUCCESSED);
                        } else {
                            receiver.sendEmptyMessage(RESPONSE_RUN_MODEL_FAILED);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }


    public boolean onLoadModel() {
        return predictor.init(MainActivity.this, config);
    }

    public boolean onRunModel() {
        return predictor.isLoaded() && predictor.runModel(preprocess,visualize);
    }

    public void onLoadModelFailed() {
    }
    public void onRunModelFailed() {
    }

    public void loadModel() {
        //pbLoadModel = ProgressDialog.show(this, "", "加载模型中...", false, false);

        //通知 flutter 跟新
        channel.invokeMethod(METHOD_REQUEST_LOAD_MODEL,"loading AI model");

        Log.i(TAG,"加载模型中...");
        sender.sendEmptyMessage(REQUEST_LOAD_MODEL);
    }

    public void runModel() {
        //pbRunModel = ProgressDialog.show(this, "", "推理中...", false, false);
        Log.i(TAG,"推理中...");
        Log.i(TAG,"ts-startRunModel:"+System.currentTimeMillis());
        //通知 flutter 跟新
        channel.invokeMethod(METHOD_REQUEST_RUN_MODEL,"running AI model");

        sender.sendEmptyMessage(REQUEST_RUN_MODEL);
    }

    public void onLoadModelSuccessed() {
        // load test image from file_paths and run model
//        try {
//            if (config.imagePath.isEmpty()||config.bgPath.isEmpty()) {
//                return;
//            }
//            Bitmap image = null;
//            Bitmap bg = null;
//
//            //加载待抠图像（如果是拍照或者本地相册读取，则第一个字符为“/”。否则就是从默认路径下读取图片）
//            if (!config.imagePath.substring(0, 1).equals("/")) {
//                InputStream imageStream = getAssets().open(config.imagePath);
//                image = BitmapFactory.decodeStream(imageStream);
//            } else {
//                if (!new File(config.imagePath).exists()) {
//                    return;
//                }
//                image = BitmapFactory.decodeFile(config.imagePath);
//            }
//
//            //加载背景图像
//            if (!config.bgPath.substring(0, 1).equals("/")) {
//                InputStream imageStream = getAssets().open(config.bgPath);
//                bg = BitmapFactory.decodeStream(imageStream);
//            } else {
//                if (!new File(config.bgPath).exists()) {
//                    return;
//                }
//                bg = BitmapFactory.decodeFile(config.bgPath);
//            }
//
//            if (image != null && bg != null && predictor.isLoaded()) {
//                predictor.setInputImage(image,bg);
//                runModel();//开始推理
//            }
//        } catch (IOException e) {
//            //Toast.makeText(MainActivity.this, "Load image failed!", Toast.LENGTH_SHORT).show();
//            Log.e(TAG,"Load image failed!");
//            e.printStackTrace();
//        }
    }

    public void onRunModelSuccessed() {
        // 获取抠图结果并更新UI
        //tvInferenceTime.setText("推理耗时: " + predictor.inferenceTime() + " ms");
        Log.i(TAG,"推理耗时: " + predictor.inferenceTime() + " ms");
        Log.i(TAG,"ts-endRunModel:"+System.currentTimeMillis());
        Bitmap outputImage = predictor.outputImage();
        if (outputImage != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            long ts = System.currentTimeMillis();
            //outputImage.compress，此步骤比较耗时，png为无损压缩，quality字段无效。
            // 主要原因是原图较大，推理后的新图也比较大
            //todo 在原生沙盒生成图片文件，传递文件名 20220616
            outputImage.compress(Bitmap.CompressFormat.PNG,60,outputStream);
            Log.i(TAG,"ts-compress cost:"+(System.currentTimeMillis() - ts));
            //通知 flutter 跟新
            Map<String,Object> map = new HashMap<>();
            map.put("bytes",outputStream.toByteArray());
            Log.i(TAG,"ts-callflutter:"+System.currentTimeMillis());
            channel.invokeMethod(METHOD_RESPONSE_RUN_MODEL_SUCCESSED,map);
        }

    }

}
