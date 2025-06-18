
import 'tilt_platform_interface.dart';

class Tilt {
  final String publicKey;
  Tilt(this.publicKey);

  Future<String?> getPlatformVersion() {
    return TiltPlatform.instance.getPlatformVersion();
  }
  Future<List<String>?> getLogLines() {
    return TiltPlatform.instance.getLogLines();
  }
}
