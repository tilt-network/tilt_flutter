import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'tilt_method_channel.dart';

abstract class TiltPlatform extends PlatformInterface {
    /// Constructs a TiltPlatform.
    TiltPlatform() : super(token: _token);

    static final Object _token = Object();

    static TiltPlatform _instance = MethodChannelTilt();

    /// The default instance of [TiltPlatform] to use.
    ///
    /// Defaults to [MethodChannelTilt].
    static TiltPlatform get instance => _instance;

    /// Platform-specific implementations should set this with their own
    /// platform-specific class that extends [TiltPlatform] when
    /// they register themselves.
    static set instance(TiltPlatform instance) {
        PlatformInterface.verifyToken(instance, _token);
        _instance = instance;
    }

    Future<void> initialize(String publicKey, String environment) {
        throw UnimplementedError('initialize() has not been implemented.');
    }

    Future<String?> getPlatformVersion() {
        throw UnimplementedError('platformVersion() has not been implemented.');
    }

    Future<List<String>?> getLogLines() {
        throw UnimplementedError('logLines() has not been implemented.');
    }
}
