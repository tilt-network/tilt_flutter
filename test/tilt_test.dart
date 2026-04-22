import 'package:flutter_test/flutter_test.dart';
import 'package:tilt_network/tilt.dart';
import 'package:tilt_network/tilt_platform_interface.dart';
import 'package:tilt_network/tilt_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockTiltPlatform with MockPlatformInterfaceMixin implements TiltPlatform {
    String? lastPublicKey;
    String? lastEnvironment;
    bool initializeCalled = false;
    bool throwOnInitialize = false;

    @override
    Future<void> initialize(String publicKey, String environment) async {
        if (throwOnInitialize) throw Exception('init error');
        initializeCalled = true;
        lastPublicKey = publicKey;
        lastEnvironment = environment;
    }

    @override
    Future<String?> getPlatformVersion() async => '42';

    @override
    Future<List<String>?> getLogLines() async => ['line1', 'line2'];
}

class NullLogsMockPlatform with MockPlatformInterfaceMixin implements TiltPlatform {
    @override
    Future<void> initialize(String publicKey, String environment) async {}

    @override
    Future<String?> getPlatformVersion() async => null;

    @override
    Future<List<String>?> getLogLines() async => null;
}

void main() {
    group('Tilt default instance', () {
        test('MethodChannelTilt is the default platform instance', () {
            expect(TiltPlatform.instance, isInstanceOf<MethodChannelTilt>());
        });
    });

    group('Tilt constructor', () {
        late MockTiltPlatform mock;

        setUp(() {
            mock = MockTiltPlatform();
            TiltPlatform.instance = mock;
        });

        test('calls initialize on construction with correct publicKey', () async {
            Tilt('pk_test_key');
            await Future.microtask(() {});
            expect(mock.initializeCalled, isTrue);
            expect(mock.lastPublicKey, 'pk_test_key');
        });

        test('uses production as default environment', () async {
            Tilt('pk_test_key');
            await Future.microtask(() {});
            expect(mock.lastEnvironment, 'production');
        });

        test('passes custom environment to platform', () async {
            Tilt('pk_test_key', environment: 'staging');
            await Future.microtask(() {});
            expect(mock.lastEnvironment, 'staging');
        });
    });

    group('Tilt.initialize()', () {
        late MockTiltPlatform mock;
        late Tilt tilt;

        setUp(() {
            mock = MockTiltPlatform();
            TiltPlatform.instance = mock;
            tilt = Tilt('pk_test');
        });

        test('delegates to platform with stored publicKey and environment', () async {
            await tilt.initialize();
            expect(mock.lastPublicKey, 'pk_test');
            expect(mock.lastEnvironment, 'production');
        });

        test('can be called multiple times without error', () async {
            await tilt.initialize();
            await tilt.initialize();
            expect(mock.initializeCalled, isTrue);
        });
    });

    group('Tilt.getLogLines()', () {
        late MockTiltPlatform mock;
        late Tilt tilt;

        setUp(() {
            mock = MockTiltPlatform();
            TiltPlatform.instance = mock;
            tilt = Tilt('pk_test');
        });

        test('returns lines from platform', () async {
            final lines = await tilt.getLogLines();
            expect(lines, ['line1', 'line2']);
        });

        test('returns null when platform returns null', () async {
            TiltPlatform.instance = NullLogsMockPlatform();
            final t = Tilt('pk_test');
            final lines = await t.getLogLines();
            expect(lines, isNull);
        });
    });

    group('Tilt.getPlatformVersion()', () {
        test('returns platform version string', () async {
            final mock = MockTiltPlatform();
            TiltPlatform.instance = mock;
            final t = Tilt('pk_test');
            expect(await t.getPlatformVersion(), '42');
        });

        test('returns null when platform returns null', () async {
            TiltPlatform.instance = NullLogsMockPlatform();
            final t = Tilt('pk_test');
            expect(await t.getPlatformVersion(), isNull);
        });
    });
}
