import 'dart:typed_data';
import 'dart:ui';

import 'dart:io';
import 'dart:convert' as convert;

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'PaddleLite for Flutter'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {

  var _image;

  final _picker = ImagePicker();

  File? _imageFile; //原图
  Uint8List? _imageUint8List;//原图

  ///
  /// 从相册选择图片
  ///
  Future<void> _pickImageFromGallery() async {
    final pickedFile = await _picker.getImage(source: ImageSource.gallery);
    if (pickedFile != null) {
      _imageFile = File(pickedFile.path);
      _imageUint8List =  await _imageFile!.readAsBytes();

      startTime = currentTimeMillis();
      print("ts-start:"+startTime.toString());
      await channel.invokeMethod(METHOD_REQUEST_RUN_MODEL, {"file": _imageUint8List} );
    }
  }

  _loadImage() async {

    //_image = Image.asset("images/image.jpg");
    //调用模型

    //ByteData? pngActorBytes = await _image.toByteData(format: ImageByteFormat.png);
    //Uint8List pngSaveBytes = pngActorBytes!.buffer.asUint8List();

    ByteData data = await rootBundle.load("images/image.jpg");
    Uint8List pngSaveBytes = data.buffer.asUint8List();

    await channel.invokeMethod(METHOD_REQUEST_RUN_MODEL, {"file": pngSaveBytes} );


    // setState(() {
    //
    // });
  }



  // 创建渠道
  //static const MethodChannel channel =  MethodChannel("readimg");

  static const MethodChannel channel = MethodChannel("flutter/channel");

  static const String METHOD_REQUEST_LOAD_MODEL = "METHOD_REQUEST_LOAD_MODEL";
  static const String METHOD_REQUEST_RUN_MODEL = "METHOD_REQUEST_RUN_MODEL";
  static const String METHOD_RESPONSE_LOAD_MODEL_SUCCESSED = "METHOD_RESPONSE_LOAD_MODEL_SUCCESSED";
  static const String METHOD_RESPONSE_LOAD_MODEL_FAILED = "METHOD_RESPONSE_LOAD_MODEL_FAILED";
  static const String METHOD_RESPONSE_RUN_MODEL_SUCCESSED = "METHOD_RESPONSE_RUN_MODEL_SUCCESSED";
  static const String METHOD_RESPONSE_RUN_MODEL_FAILED = "METHOD_RESPONSE_RUN_MODEL_FAILED";

  var startTime;

  int currentTimeMillis() {
    return new DateTime.now().millisecondsSinceEpoch;
  }

  void _initChannel() async{
    channel.setMethodCallHandler((call) => Future<void>((){
      switch(call.method){
        case "METHOD_REQUEST_LOAD_MODEL":

          break;
        case "METHOD_REQUEST_RUN_MODEL":

          break;
        case "METHOD_RESPONSE_LOAD_MODEL_SUCCESSED":

          break;
        case "METHOD_RESPONSE_LOAD_MODEL_FAILED":

          break;
        case METHOD_RESPONSE_RUN_MODEL_SUCCESSED:

          Uint8List bytes = call.arguments['bytes'];

          var cost = currentTimeMillis()-startTime;
          print("ts-end:"+currentTimeMillis().toString());
          print(METHOD_RESPONSE_RUN_MODEL_SUCCESSED + ",cost:"+cost.toString());

          setState(() {
            _image = Image.memory(bytes);
          });

          break;
        case "METHOD_RESPONSE_RUN_MODEL_FAILED":

          break;
        default:
          print("METHOD can not catch:"+call.method);
          break;
      }
    }));
  }

  // Future<void> _callNativeMethod(String filepath) async {
  //   try {
  //     // 通过渠道，调用原生代码代码的方法
  //     Uint8List bytes = await channel.invokeMethod("readimg", {"filepath": filepath} );
  //     // 打印执行的结果
  //
  //     _image = Image.memory(bytes);
  //
  //     setState(() {
  //     });
  //
  //   } on PlatformException catch(e) {
  //     print(e.toString());
  //   }
  // }

  @override
  initState() {
    super.initState();
    //_loadImage();

    _initChannel();
    //_callNativeMethod("filepath_123");
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        actions: [
          IconButton(
            icon: const Icon(Icons.crop),
            onPressed: (){
              print("3");
              _loadImage();
            },
          ),

          IconButton(
            icon: const Icon(Icons.image_search_sharp),
            onPressed: (){
              //print("1");
              _pickImageFromGallery();
            },
          ),

          IconButton(
            icon: const Icon(Icons.menu),
            onPressed: (){
              print("2");
            },
          ),
        ],
      ),
      body: Center(

        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            _image == null ? Text("2") : _image,
            //Image.asset("images/image.jpg"),
          ],
        ),
      ),
    );
  }
}
