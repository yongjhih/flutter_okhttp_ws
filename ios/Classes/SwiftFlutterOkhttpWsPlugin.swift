import Flutter
import UIKit

public class SwiftFlutterOkhttpWsPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_okhttp_ws", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterOkhttpWsPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
