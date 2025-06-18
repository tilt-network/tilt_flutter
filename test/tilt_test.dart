import 'package:flutter_test/flutter_test.dart';
import 'package:tilt/tilt.dart';
import 'package:tilt/tilt_platform_interface.dart';
import 'package:tilt/tilt_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockTiltPlatform
    with MockPlatformInterfaceMixin
    implements TiltPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<List<String>?> getLogLines() => Future.value(['42']);

  @override
  Future<void> initialize(String publicKey, String environment) {
    // TODO: implement initialize
    throw UnimplementedError();
  }
}

void main() {
  final TiltPlatform initialPlatform = TiltPlatform.instance;

  test('$MethodChannelTilt is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelTilt>());
  });

  test('getPlatformVersion', () async {
    Tilt tiltPlugin = Tilt("pk_3NqPrvpe6nDkdtyS1gJt4kX_4MQ");
    MockTiltPlatform fakePlatform = MockTiltPlatform();
    TiltPlatform.instance = fakePlatform;

    expect(await tiltPlugin.getPlatformVersion(), '42');
  });
}
