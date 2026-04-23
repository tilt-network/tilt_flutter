import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:tilt_network/tilt.dart';

void main() => runApp(const TiltExampleApp());

class TiltExampleApp extends StatelessWidget {
    const TiltExampleApp({super.key});

    @override
    Widget build(BuildContext context) {
        return MaterialApp(
            title: 'Tilt SDK Demo',
            theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
            home: const TiltDemoPage(),
        );
    }
}

class TiltDemoPage extends StatefulWidget {
    const TiltDemoPage({super.key});

    @override
    State<TiltDemoPage> createState() => _TiltDemoPageState();
}

class _TiltDemoPageState extends State<TiltDemoPage> {
    final _pkController = TextEditingController(
        text: 'pk_3NqPrvpe6nDkdtyS1gJt4kX_4MQ',
    );
    final _envController = TextEditingController(text: 'staging');
    final _scrollController = ScrollController();

    Tilt? _tilt;
    List<String> _logs = [];
    bool _running = false;
    String _status = 'Idle';

    void _start() {
        final pk = _pkController.text.trim();
        final env = _envController.text.trim();
        if (pk.isEmpty) return;

        final tilt = Tilt(pk, environment: env);

        // Stream logs from the native runtime via EventChannel
        EventChannel('tilt/logs')
            .receiveBroadcastStream()
            .map((e) => List<String>.from(e as List))
            .listen(
                (lines) {
                    if (!mounted) return;
                    setState(() => _logs = lines);
                    _scrollToBottom();
                },
                onError: (e) {
                    if (!mounted) return;
                    setState(() => _status = 'Error: $e');
                },
            );

        setState(() {
            _tilt = tilt;
            _running = true;
            _status = 'Running ($env)';
            _logs = [];
        });
    }

    void _scrollToBottom() {
        WidgetsBinding.instance.addPostFrameCallback((_) {
            if (_scrollController.hasClients) {
                _scrollController.animateTo(
                    _scrollController.position.maxScrollExtent,
                    duration: const Duration(milliseconds: 150),
                    curve: Curves.easeOut,
                );
            }
        });
    }

    @override
    void dispose() {
        _pkController.dispose();
        _envController.dispose();
        _scrollController.dispose();
        super.dispose();
    }

    @override
    Widget build(BuildContext context) {
        return Scaffold(
            appBar: AppBar(
                title: const Text('Tilt SDK Demo'),
                actions: [
                    Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        child: Chip(
                            label: Text(_status),
                            backgroundColor: _running
                                ? Colors.green.shade100
                                : Colors.grey.shade200,
                        ),
                    ),
                ],
            ),
            body: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                        TextField(
                            controller: _pkController,
                            enabled: !_running,
                            decoration: const InputDecoration(
                                labelText: 'Public Key',
                                border: OutlineInputBorder(),
                            ),
                        ),
                        const SizedBox(height: 8),
                        TextField(
                            controller: _envController,
                            enabled: !_running,
                            decoration: const InputDecoration(
                                labelText: 'Environment',
                                border: OutlineInputBorder(),
                                hintText: 'staging | production',
                            ),
                        ),
                        const SizedBox(height: 12),
                        ElevatedButton.icon(
                            onPressed: _running ? null : _start,
                            icon: const Icon(Icons.play_arrow),
                            label: const Text('Start'),
                        ),
                        const SizedBox(height: 16),
                        const Text(
                            'Runtime logs',
                            style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 8),
                        Expanded(
                            child: Container(
                                decoration: BoxDecoration(
                                    color: Colors.black87,
                                    borderRadius: BorderRadius.circular(8),
                                ),
                                padding: const EdgeInsets.all(8),
                                child: _logs.isEmpty
                                    ? const Center(
                                        child: Text(
                                            'No logs yet',
                                            style: TextStyle(color: Colors.white38),
                                        ),
                                    )
                                    : ListView.builder(
                                        controller: _scrollController,
                                        itemCount: _logs.length,
                                        itemBuilder: (_, i) => Text(
                                            _logs[i],
                                            style: TextStyle(
                                                color: _logColor(_logs[i]),
                                                fontFamily: 'monospace',
                                                fontSize: 12,
                                            ),
                                        ),
                                    ),
                            ),
                        ),
                    ],
                ),
            ),
        );
    }

    Color _logColor(String line) {
        if (line.contains('[ERROR]')) return Colors.red.shade300;
        if (line.contains('[WARN]')) return Colors.orange.shade300;
        return Colors.green.shade300;
    }
}
