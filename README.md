<p align="center">
	<img src="https://ds9bjnn93rsnp.cloudfront.net/assets/logo/logotype_black_on_transparent_782x256-7256a7ab03e9652908f43be94681bc4ebeff6d729c36c946c346a80a4f8ca245.png" width=300 />
</p>

[![Documentation](https://img.shields.io/badge/documentation-latest-blue.svg)](http://www.buglife.com/docs/android)
[![Twitter](https://img.shields.io/badge/twitter-@BuglifeApp-blue.svg)](https://twitter.com/buglifeapp)

Buglife is an awesome bug reporting SDK & web platform for Android apps. Here's how it works:

1. User takes a screenshot, or stops screen recording
2. User annotates their screenshot & writes feedback
3. Bug reports are pushed to your team's email/Jira/Slack/Asana/wherever you track bugs.

You can also find Buglife for iOS [here](https://github.com/buglife/buglife-ios).

<p align="center" style="margin-top: 20px; margin-bottom: 20px;">
	<img src="https://i.imgur.com/73pDl6Q.png" />
</p>

---

|   | Main Features |
|---|---------------|
| 👤 | Free + no account required |
| 📖 | Open source |
| 🏃🏽‍♀️ | Fast & lightweight |
| 🎨 | Themeable |
| 📩 | Automatic caching & retry |
| 🎥 | Screen recording |
| 📜 | Custom form fields, with pickers & multiline text fields  |
| ℹ️ | Advanced logging, with debug / info / warning levels |
| 📎 | Custom attachments, including JSON & SQLite support |

## Installation

1. Add `buglife-android` as a dependency to your app's `build.gradle`. You'll also need to add [Android Volley](https://developer.android.com/training/volley/) if you haven't already.

	```groovy
	dependencies {
		implementation 'com.buglife.sdk:buglife-android:1.4.1'
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

### Invocation

There are 2 different ways to invoke the bug reporter, enumerated by Buglife.InvocationMethod:

1. `SHAKE`: Invokes when the user shakes their device.
	
	```java
	Buglife.setInvocationMethod(InvocationMethod.SHAKE);
	```
2. `SCREENSHOT`: Invokes when the user manually takes a screenshot with their device.
	
	```java
	Buglife.setInvocationMethod(InvocationMethod.SCREENSHOT);
	```
3. `NONE`: Disables all except manual invocations.
	
	```java
	Buglife.setInvocationMethod(InvocationMethod.NONE);
	```
	This may be preferable for production builds. In this case, we recommend embedding a hidden button somewhere in your app which manually presents the bug reporter, so that you can still collect logs & other information.

For both automatic invocations (`SHAKE` and `SCREENSHOT`), a screenshot of the current activity is captured & attached to the bug report draft.

We recommend using the screenshot invocation for most apps. Taking screenshots to report feedback comes naturally to most users, and screenshots are still saved to the user's device as expected.

#### Manual & Custom Invocations

You can manually present the bug reporter activity using:

```java
Buglife.showReporter();
```

Unlike the `SHAKE` & `SCREENSHOT` invocations, manually presenting the bug reporter activity does not automatically capture & attach a screenshot to the bug report draft.

This is useful for situations where attaching a screenshot of the immediate application state doesn't make sense (for example, if you have a "Report Bug" option in your app's settings screen).

##### Manually capture & attach screenshots

You can capture a screenshot of your app at any moment using `Buglife.getScreenshotBitmap()`. You can then create an Attachment object from the generated bitmap, and add this to your bug report draft using the `Buglife.addAttachment()` method. For example:

```java
// Capture a screenshot of the current activity
Bitmap screenshot = Buglife.getScreenshotBitmap();
// Create an Attachment object with the generated screenshot
Attachment attachment = new Attachment
		.Builder("Screenshot.png", Attachment.TYPE_PNG)
		.build(screenshot);
// Queue the Attachment for the next bug report draft
Buglife.addAttachment(attachment);
// Show the bug reporter activity. This will include the queued attachment
Buglife.showReporter();
```

### User Identification

If your application stores a user’s email address, then typically you should simply set the user’s email address shortly after initializing Buglife:

```java
String email = /** Current user's email */
Buglife.setUserEmail(email);
```

Alternatively, you may set a string representing the user’s name, database ID or other identifier:

```java
String username = /** Current username */
Buglife.setUserIdentifier(username);
```

### Custom Attributes

#### Adding custom attributes

You can include custom attributes (i.e. key-value pairs) to your bug reports, as such:

```java
Buglife.putAttribute("Artist", "2Pac");
Buglife.putAttribute("Song", "California Love");
```

#### Removing attributes

To clear an attribute, set its value to null.

```java
Buglife.putAttribute("Artist", null);
```

### Screen Recording

Buglife can be used to record a user's screen, and attach the recording to a bug report. Screen recording is initiated programmatically, like such:

```java
Buglife.startScreenRecording();
Here's how the complete screen recording flow works:
```

1. The screen recording flow is initiated using `Buglife.startScreenRecording();`
2. The user is shown an OS-level prompt requesting permission to record the screen.
3. Once granted, Buglife immediately begins recording the screen.
4. Screen recording will progress for up to 30 seconds. If the user wishes to stop screen recording earlier, they may do so by tapping the floating Record button.
5. The Buglife reporter UI is automatically shown with the recording attached.

### Learn more

Check out the [Buglife Android guide](http://www.buglife.com/docs/android) for configuration & customization options.

## License

```
Copyright (C) 2018 Buglife, Inc.

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

