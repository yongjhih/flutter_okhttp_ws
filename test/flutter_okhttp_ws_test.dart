import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_okhttp_ws/flutter_okhttp_ws.dart';

void main() {
  const MethodChannel channel = MethodChannel('flutter_okhttp_ws');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await FlutterOkhttpWs.platformVersion, '42');
  });
}
