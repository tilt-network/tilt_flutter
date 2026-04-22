
import 'tilt_platform_interface.dart';

class Tilt {
    final String publicKey;
    final String environment;

    Tilt(this.publicKey, {this.environment = 'production'}) {
        TiltPlatform.instance.initialize(publicKey, environment);
    }

    Future<void> initialize() => TiltPlatform.instance.initialize(publicKey, environment);

    Future<String?> getPlatformVersion() {
        return TiltPlatform.instance.getPlatformVersion();
    }
    Future<List<String>?> getLogLines() {
        return TiltPlatform.instance.getLogLines();
    }
}
