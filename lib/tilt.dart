
import 'tilt_platform_interface.dart';

class Tilt {
    final String publicKey;
    final String environment;
    final Future<void> _initialized;

    Tilt(this.publicKey, {this.environment = 'production'})
        : _initialized = TiltPlatform.instance.initialize(publicKey, environment);

    Future<void> initialize() => TiltPlatform.instance.initialize(publicKey, environment);

    Future<String?> getPlatformVersion() {
        return TiltPlatform.instance.getPlatformVersion();
    }
    Future<List<String>?> getLogLines() {
        return TiltPlatform.instance.getLogLines();
    }
}
