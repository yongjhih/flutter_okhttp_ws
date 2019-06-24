#import "FlutterOkhttpWsPlugin.h"
#import <flutter_okhttp_ws/flutter_okhttp_ws-Swift.h>

@implementation FlutterOkhttpWsPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterOkhttpWsPlugin registerWithRegistrar:registrar];
}
@end
