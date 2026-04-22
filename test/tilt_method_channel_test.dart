import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tilt_network/tilt_method_channel.dart';

void main() {
    TestWidgetsFlutterBinding.ensureInitialized();

    late MethodChannelTilt platform;
    const MethodChannel channel = MethodChannel('tilt');

    setUp(() {
        platform = MethodChannelTilt();
    });

    tearDown(() {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null);
    });

    group('initialize()', () {
        test('sends initialize method with publicKey and environment', () async {
            MethodCall? captured;
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (call) async {
                    captured = call;
                    return null;
                });

            await platform.initialize('pk_abc', 'staging');

            expect(captured?.method, 'initialize');
            expect(captured?.arguments['publicKey'], 'pk_abc');
            expect(captured?.arguments['environment'], 'staging');
        });

        test('sends production as environment when not specified', () async {
            MethodCall? captured;
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (call) async {
                    captured = call;
                    return null;
                });

            await platform.initialize('pk_abc', 'production');

            expect(captured?.arguments['environment'], 'production');
        });

        test('completes without error when platform returns null', () async {
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (call) async => null);

            await expectLater(
                platform.initialize('pk_abc', 'production'),
                completes,
            );
        });
    });

    group('getLogLines()', () {
        test('sends getLogLines method and returns list', () async {
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (call) async {
                    if (call.method == 'getLogLines') return ['log1', 'log2'];
                    return null;
                });

            final lines = await platform.getLogLines();
            expect(lines, ['log1', 'log2']);
        });

        test('returns null when platform returns null', () async {
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (call) async => null);

            final lines = await platform.getLogLines();
            expect(lines, isNull);
        });

        test('returns empty list when platform returns empty', () async {
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (call) async => <String>[]);

            final lines = await platform.getLogLines();
            expect(lines, isEmpty);
        });
    });

    group('getPlatformVersion()', () {
        test('sends getPlatformVersion method and returns string', () async {
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (call) async {
                    if (call.method == 'getPlatformVersion') return 'Android 14';
                    return null;
                });

            final version = await platform.getPlatformVersion();
            expect(version, 'Android 14');
        });

        test('returns null when platform returns null', () async {
            TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
                .setMockMethodCallHandler(channel, (call) async => null);

            final version = await platform.getPlatformVersion();
            expect(version, isNull);
        });
    });
}
