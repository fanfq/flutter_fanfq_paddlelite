package com.example.flutter_fanfq_paddlelite;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.renderscript.RenderScript;
import android.util.Log;

import com.example.flutter_fanfq_paddlelite.config.Config;
import com.example.flutter_fanfq_paddlelite.preprocess.Preprocess;
import com.example.flutter_fanfq_paddlelite.visual.Visualize;


import androidx.annotation.NonNull;

import java.util.HashMap;

import io.flutter.embedding.android.FlutterActivity;

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

//    @Override
//    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
//        super.configureFlutterEngine(flutterEngine);
//
//        rs = RenderScript.create(this);
//
//        Log.e(TAG,"configureFlutterEngine");

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
//    }

//    protected void onResume() {
//        Log.i(TAG,"begin onResume");
//        super.onResume();
//
//        boolean settingsChanged = false;
//        String model_path = "image_matting/models/modnet";
//        String label_path = "image_matting/labels/label_list";
//        String image_path = "image_matting/images/human.jpg";
//        String bg_path = "image_matting/images/bg.jpg";
//        settingsChanged |= !model_path.equalsIgnoreCase(config.modelPath);
//        settingsChanged |= !label_path.equalsIgnoreCase(config.labelPath);
//        settingsChanged |= !image_path.equalsIgnoreCase(config.imagePath);
//        settingsChanged |= !bg_path.equalsIgnoreCase(config.bgPath);
//        int cpu_thread_num = 1;
//        settingsChanged |= cpu_thread_num != config.cpuThreadNum;
//        String cpu_power_mode = "LITE_POWER_HIGH";
//        settingsChanged |= !cpu_power_mode.equalsIgnoreCase(config.cpuPowerMode);
//        String input_color_format = "RGB";
//        settingsChanged |= !input_color_format.equalsIgnoreCase(config.inputColorFormat);
//        long[] input_shape = new long[]{1,3,256,256};
//
//        settingsChanged |= input_shape.length != config.inputShape.length;
//
//        if (!settingsChanged) {
//            for (int i = 0; i < input_shape.length; i++) {
//                settingsChanged |= input_shape[i] != config.inputShape[i];
//            }
//        }
//
//        if (settingsChanged) {
//            config.init(model_path,label_path,image_path,bg_path,cpu_thread_num,cpu_power_mode,
//                    input_color_format,input_shape);
//            preprocess.init(config);
//            // 更新UI
//            //tvInputSetting.setText("算法模型: " + config.modelPath.substring(config.modelPath.lastIndexOf("/") + 1));
//            //tvInputSetting.scrollTo(0, 0);
//            // 如果配置发生改变则重新加载模型并预测
//            //loadModel();
//        }
//    }


}
