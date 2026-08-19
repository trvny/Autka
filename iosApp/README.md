# iOS host

Open `iosApp.xcodeproj` in Xcode. Simulator builds do not need signing.

For a physical iPhone, set `TEAM_ID` in `Configuration/Config.xcconfig` to your Apple Developer team ID before building. The Kotlin framework is built and embedded automatically by the Xcode build phase.
