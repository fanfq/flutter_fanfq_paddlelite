import 'package:flutter/material.dart';

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

  _loadImage() async {

    _image = Image.asset("images/image.jpg");
    //调用模型

    setState(() {

    });
  }

  @override
  initState() {
    super.initState();
    _loadImage();
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
              print("1");
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
