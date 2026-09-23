<a href="https://customerly.io" target="_blank">
    <img src="https://avatars1.githubusercontent.com/u/23583405?s=200&v=4" height="100" alt="Customerly logo">
</a>

# Customerly Android SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.customerly/customerlyandroidsdk)](https://search.maven.org/artifact/io.customerly/customerlyandroidsdk)
![GitHub License](https://img.shields.io/github/license/Customerly/CustomerlyAndroidSDK)

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
    implementation 'io.customerly:customerlyandroidsdk:1.2.0'
}
```

The SDK declares the permissions it needs (`INTERNET`, `POST_NOTIFICATIONS`, and the storage permissions used by the live-chat file-attachment flow) in its own manifest, so they are merged into your app automatically — no manifest changes are required.

> **Note:** the SDK declares `MANAGE_EXTERNAL_STORAGE`, which is subject to Google Play's [All files access policy](https://support.google.com/googleplay/android-developer/answer/10467955). If your app is distributed on Google Play you may need to justify this permission during review.

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

#### load

Initializes the Customerly SDK with the provided settings.

```kotlin
Customerly.load(context, CustomerlySettings(app_id = "YOUR_APP_ID"))
```

#### setContext

Updates the context used by the SDK. Call this when your application's context changes.

```kotlin
Customerly.setContext(context)
```

#### update

Updates the Customerly SDK settings.

```kotlin
Customerly.update(CustomerlySettings(app_id = "YOUR_APP_ID"))
```

#### requestNotificationPermissionIfNeeded

Requests notification permissions if not already granted.

```kotlin
Customerly.requestNotificationPermissionIfNeeded()
```

### Chat Interface

#### show

Shows the Customerly chat interface.

```kotlin
Customerly.show()
```

#### hide

Hides the Customerly chat interface.

```kotlin
Customerly.hide()
```

#### back

Navigates back in the chat interface.

```kotlin
Customerly.back()
```

### User Management

#### logout

Logs out the current user.

```kotlin
Customerly.logout()
```

#### registerLead

Registers a new lead with the provided email and optional attributes.

```kotlin
Customerly.registerLead(email = "test@customerly.io", attributes = mapOf("name" to "John Doe"))
```

### Messaging

#### showNewMessage

Shows the chat interface with a pre-filled message.

```kotlin
Customerly.showNewMessage(message = "Hello, how are you?")
```

#### sendNewMessage

Sends a new message and shows the chat interface.

```kotlin
Customerly.sendNewMessage(message = "Hello, how are you?")
```

#### navigateToConversation

Navigates to a specific conversation.

```kotlin
Customerly.navigateToConversation(conversationId = 123)
```

### Help Center

#### showArticle

Shows a specific help center article.

```kotlin
Customerly.showArticle(collectionSlug = "collection", articleSlug = "article")
```

### Analytics

#### event

Tracks a custom event.

```kotlin
Customerly.event(name = "event_name")
```

#### attribute

Sets a custom attribute for the current user.

```kotlin
Customerly.attribute(name = "attribute_name", value = "attribute_value")
```

### Message Counts

#### getUnreadMessagesCount

Gets the count of unread messages.

```kotlin
Customerly.getUnreadMessagesCount(resultCallback = { count ->
    Log.d("Customerly", "Unread messages count: $count")
})
```

#### getUnreadConversationsCount

Gets the count of unread conversations.

```kotlin
Customerly.getUnreadConversationsCount(resultCallback = { count ->
    Log.d("Customerly", "Unread conversations count: $count")
})
```

### Callbacks

The SDK provides various callback methods to handle different events:

```kotlin
fun setOnChatClosed(callback: () -> Unit)
fun setOnChatOpened(callback: () -> Unit)
fun setOnMessageRead(callback: (Int, Int) -> Unit)
fun setOnMessengerInitialized(callback: () -> Unit)
fun setOnMessengerLoadFailed(callback: (MessengerLoadFailure) -> Unit)
fun setOnNewMessageReceived(callback: (UnreadMessage) -> Unit)
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

## Examples

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

## Development

To release a new version of the SDK, you need to:

1. Update the version in the `build.gradle` file
2. Update the version in the `README.md` file
3. Push the changes to the `main` branch
4. Create a new tag with the version number (e.g. `1.0.0`)
5. The GitHub Actions workflow will build the SDK and release it to Maven Central

## License

Copyright 2025-2026 Customerly Ltd.

This SDK is licensed under the [Apache License 2.0](LICENSE) — you are free to use it in commercial and closed-source applications. See the `LICENSE` and `NOTICE` files for details.

> Versions up to and including `1.1.0` were published under the GNU GPLv3. The Apache 2.0 license applies from the next release onwards. If you are pinned to an older version, upgrade to pick up the more permissive terms.
