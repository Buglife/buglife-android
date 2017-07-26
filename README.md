![Buglife: Awesome Bug Reporting](https://ds9bjnn93rsnp.cloudfront.net/assets/logo/logotype_black_on_transparent_782x256-7256a7ab03e9652908f43be94681bc4ebeff6d729c36c946c346a80a4f8ca245.png)

[![Documentation](https://img.shields.io/badge/documentation-latest-blue.svg)](http://www.buglife.com/docs/android)
[![Twitter](https://img.shields.io/badge/twitter-@BuglifeApp-blue.svg)](https://twitter.com/buglifeapp)

Buglife for Android is an awesome library for reporting bugs, getting feedback, annotating screenshots, collecting diagnostic information, and more.

## Installation

1. Add `buglife-android` as a dependency to your `build.gradle`.

	```groovy
	dependencies {
		compile 'com.buglife.sdk:buglife-android:0.9.12'
	}
	```

2. Create an `Application` subclass in your project if you don't already have one. (And don't forget to add it to your app's `AndroidManifest.xml`.)

3. Add the following lines to the end of the `onCreate()` method in your app's `Application` subclass:

	```java
	Buglife.initWithEmail(this, "you@yourdomain.com");
	Buglife.setInvocationMethod(InvocationMethod.SCREENSHOT);
	```
	Be sure to replace `you@yourdomain.com` with your own email address; this is where bug reports will be sent to.
	
	##### Got an account?
	
	If you have a Buglife account already, you should initialize Buglife with your team's API key instead of your email:
	
	```java
	Buglife.initWithApiKey(this, "YOUR_API_KEY");
	Buglife.setInvocationMethod(InvocationMethod.SCREENSHOT);
	```

4. Build & run on a device; take a screenshot to report a bug!

5. Submit a bug report from your device, then check your email for a link to the Buglife web dashboard!

## Usage

Check out the [Buglife Android guide](http://www.buglife.com/docs/android) for configuration & customization options.

## License

```
Copyright (C) 2017 Buglife, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

