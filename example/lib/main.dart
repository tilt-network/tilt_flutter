import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:tilt/tilt.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _tilt = Tilt("pk_3NqPrvpe6nDkdtyS1gJt4kX_4MQ");
  List<String> _logs = [];
  late final Stream<List<String>> _logsStream;


  @override
  void initState() {
    super.initState();
    initPlatformState();

    _fetchLogLines();
    // 2) escuta contínua
    _logsStream = EventChannel('tilt/logs')
        .receiveBroadcastStream()
        .map((e) => List<String>.from(e));
    _logsStream.listen((logs) {
      setState(() => _logs = logs);
    });
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion =
          await _tilt.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  Future<void> _fetchLogLines() async {
    final snapshot = await _tilt.getLogLines();
    if (!mounted) return;
    setState(() => _logs = snapshot ?? []);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Tilt Flutter')),
        body: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text('Running on: $_platformVersion'),
            ),
            const Divider(),
            const Text('Logs:'),
            Expanded(
              child: ListView.builder(
                itemCount: _logs.length,
                itemBuilder: (_, i) => ListTile(
                  title: Text(_logs[i]),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}