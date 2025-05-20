# Customerly Android SDK

Customerly is a customer service platform that helps businesses provide better support to their customers. The Android SDK allows you to integrate Customerly's features directly into your Android application, including:

- Live chat support
- Help center articles
- User profiling
- Event tracking
- Lead generation
- Surveys
- Real-time video calls

## Installation

1. Add the following dependency to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'io.customerly:androidsdk:1.0.0'
}
```

2. Add the following permissions to your `AndroidManifest.xml` to enable file attachments in the live chat:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
```

These permissions are required to allow users to attach files in the live chat. The `WRITE_EXTERNAL_STORAGE` permission is limited to Android 10 (API level 28) and below, while `MANAGE_EXTERNAL_STORAGE` is used for Android 11 (API level 30) and above.

## Basic Usage

1. Initialize the SDK in your Application class or MainActivity:

```kotlin
Customerly.load(context, CustomerlySettings(app_id = "YOUR_APP_ID"))
```

2. Show the chat interface:

```kotlin
Customerly.show()
```

## APIs

### Initialization

#### `load(context: Context, settings: CustomerlySettings)`
Initializes the Customerly SDK with the provided settings.

#### `setContext(context: Context)`
Updates the context used by the SDK. Call this when your application's context changes.

#### `update(settings: CustomerlySettings)`
Updates the Customerly SDK settings.

#### `requestNotificationPermissionIfNeeded()`
Requests notification permissions if not already granted.

### Chat Interface

#### `show(withoutNavigation: Boolean = false, safe: Boolean = false)`
Shows the Customerly chat interface.

#### `hide()`
Hides the Customerly chat interface.

#### `back()`
Navigates back in the chat interface.

### User Management

#### `logout()`
Logs out the current user.

#### `registerLead(email: String, attributes: Map<String, String>? = null)`
Registers a new lead with the provided email and optional attributes.

### Messaging

#### `showNewMessage(message: String)`
Shows the chat interface with a pre-filled message.

#### `sendNewMessage(message: String)`
Sends a new message and shows the chat interface.

#### `navigateToConversation(conversationId: Int)`
Navigates to a specific conversation.

### Help Center

#### `showArticle(collectionSlug: String, articleSlug: String)`
Shows a specific help center article.

### Analytics

#### `event(name: String)`
Tracks a custom event.

#### `attribute(name: String, value: Any)`
Sets a custom attribute for the current user.

### Message Counts

#### `getUnreadMessagesCount(resultCallback: ValueCallback<Int>)`
Gets the count of unread messages.

#### `getUnreadConversationsCount(resultCallback: ValueCallback<Int>)`
Gets the count of unread conversations.

### Callbacks

The SDK provides various callback methods to handle different events:

```kotlin
fun setOnChatClosed(callback: () -> Unit)
fun setOnChatOpened(callback: () -> Unit)
fun setOnMessengerInitialized(callback: () -> Unit)
fun setOnNewMessageReceived(callback: (Int, String, Long, Int, Int) -> Unit)
fun setOnNewConversation(callback: (String, List<AttachmentPayload>) -> Unit)
fun setOnNewConversationReceived(callback: (Int) -> Unit)
fun setOnHelpCenterArticleOpened(callback: (HelpCenterArticle) -> Unit)
fun setOnLeadGenerated(callback: (String?) -> Unit)
fun setOnProfilingQuestionAnswered(callback: (String, String) -> Unit)
fun setOnProfilingQuestionAsked(callback: (String) -> Unit)
fun setOnRealtimeVideoAnswered(callback: (RealtimeCall) -> Unit)
fun setOnRealtimeVideoCanceled(callback: () -> Unit)
fun setOnRealtimeVideoReceived(callback: (RealtimeCall) -> Unit)
fun setOnRealtimeVideoRejected(callback: () -> Unit)
fun setOnSurveyAnswered(callback: () -> Unit)
fun setOnSurveyPresented(callback: (Survey) -> Unit)
fun setOnSurveyRejected(callback: () -> Unit)
```

Each callback has a corresponding remove method:

```kotlin
fun removeOnChatClosed()
fun removeOnChatOpened()
// ... and so on for all callbacks
```

You can also remove all callbacks at once:

```kotlin
fun removeAllCallbacks()
```

## Example Project

The SDK includes a sample app project located in the `sampleapp` directory that demonstrates how to integrate and use the Customerly SDK. The sample app showcases various features including:

- Basic SDK initialization
- Messenger presentation
- User management
- Event tracking
- Message handling
- Notification handling
- Callback usage

To run the sample app:

1. Open the project in Android Studio
2. Navigate to the `sampleapp` module
3. Replace the `app_id` in `MainActivity.kt` with your Customerly app ID
4. Run the app on your device or emulator

The sample app provides a complete reference implementation of all SDK features and can be used as a starting point for your integration.

## License

This SDK is licensed under the GNU GPLv3 License. See the LICENSE file for more details.
