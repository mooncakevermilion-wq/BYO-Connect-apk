# BYO Connect - Android WebView App

A native Android WebView wrapper for **BYO Connect** (`https://mooncakevermilion-wq.github.io/BYO-Connect-app/`), engineered with a native splash experience, camera and gallery file upload capabilities, WhatsApp and dialer integration, smooth cyan pull-to-refresh, an offline error recovery screen, and automated GitHub Actions APK builds.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been confirmed and incorporated into the design:

- **Image & File Uploads**: Hybrid file picker enabling direct camera photo capture or photo gallery selection for creating and uploading posts on BYO Connect.
- **Offline / Error Handling**: Custom dark (`#000000`) and cyan (`#00F2EA`) error screen with an animated retry button and connection status check.
- **CI/CD Automation**: Inclusion of `.github/workflows/build-apk.yml` configured to build and upload both debug and release APK artifacts automatically on GitHub.
- **Package & Identity**: App ID configured as `com.byoconnect.bulawayo` with user-facing app name `BYO Connect`.

---

## 1. Overview & Core Concept

- **What It Does**: Wraps the BYO Connect progressive web platform into an immersive, native-feeling Android application without browser chrome (no URL bar or navigation buttons). It bridges device hardware features—including camera capture, photo library access, hardware back navigation, telephone calling, and direct WhatsApp chatting.
- **Target Audience & Context**: Citizens, entrepreneurs, and visitors in Bulawayo utilizing the BYO Connect community platform to find local services, connect with businesses, and share posts/media.
- **Key Value**: Provides an instant, app-store-ready native experience with fast launch times, offline resilience, camera access for image posts, and native intent handling for local communication channels.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **App Launch & Splash Screen**:
   - The user taps the BYO Connect icon.
   - An instant splash screen appears featuring the BYO Connect stylized badge and typography on pure pitch black (`#000000`) with vibrant cyan (`#00F2EA`) accents.
   - While the splash screen is visible, the WebView pre-warms and initiates loading the web app.
   - Once the DOM is ready or first layout renders, the splash smoothly transitions into the web app view.

2. **Web Browsing & Native Integration**:
   - The user browses BYO Connect in full-bleed edge-to-edge mode.
   - While navigating between pages, a slim 3dp cyan progress bar animates across the top edge.
   - Pulling down from the top triggers a smooth cyan pull-to-refresh spinner to reload the page.

3. **External Action Links**:
   - Clicking any `wa.me` or `whatsapp://` link directly launches the official WhatsApp app with the target chat and pre-filled text.
   - Clicking any `tel:` link opens the Android phone dialer with the phone number pre-filled.
   - Clicking `mailto:` launches the system email client.

4. **Media Uploading**:
   - When tapping "Upload Photo" or creating a post, an Android bottom sheet/chooser offers:
     - **Camera**: Take a new photo instantly via secure `FileProvider`.
     - **Gallery**: Choose an existing photo from the device.
   - Selected images are returned cleanly to the web input element.

5. **Hardware Back Button Handling**:
   - Tapping Back navigates backward through the WebView's browsing history.
   - If on the root page or history is empty, the app closes cleanly without hanging.

6. **Offline State & Recovery**:
   - If internet connectivity drops or DNS fails, a styled dark screen displays a clear illustration, explanatory text ("You're offline"), and a "Retry Connection" button in cyan.
   - Once connection is restored or retry is tapped, the web app reloads immediately.

### Visual Identity & Theme Tokens

| Token | Hex Value | Role |
|---|---|---|
| Background | `#000000` | Deep pitch black native background |
| Surface Dark | `#0D1117` | Card and modal surfaces |
| Primary Accent | `#00F2EA` | Electric cyan for progress bar, buttons, highlights |
| Primary Glow | `#00A8A2` | Darker cyan for borders and pressed states |
| Text Primary | `#FFFFFF` | Crisp white typography |
| Text Secondary | `#8B949E` | Muted silver gray for subheaders and status |

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Edge-to-Edge WebView with System Insets Protection**
  - *Chosen Approach*: Use `enableEdgeToEdge()` with custom padding to ensure status bar indicators remain legible while avoiding notch clipping and navigation gesture interference.
  - *Why*: Delivers an authentic native mobile look and feel, eliminating browser URL bars and toolbars.

- **Decision 2: Dual File Provider + Chooser for Web File Inputs**
  - *Chosen Approach*: Implement `WebChromeClient.onShowFileChooser` combining an `ACTION_IMAGE_CAPTURE` camera intent (backed by an internal `androidx.core.content.FileProvider`) with `ACTION_GET_CONTENT` gallery intent in an `Intent.createChooser`.
  - *Why*: Web applications often need either an instant camera snapshot or an existing file. Provides complete parity with native social apps while handling permission and cancellation edge cases.

- **Decision 3: Fallback Redirection for External Intent Schemes**
  - *Chosen Approach*: Intercept `shouldOverrideUrlLoading` in `WebViewClient`. Match `wa.me`, `api.whatsapp.com`, `whatsapp://`, `tel:`, and `mailto:`. If WhatsApp is not installed on the device, seamlessly route to the web version `https://api.whatsapp.com/...`.
  - *Why*: Prevents `ERR_UNKNOWN_URL_SCHEME` crashes and guarantees user communication always succeeds.

- **Decision 4: CI/CD GitHub Actions Workflow**
  - *Chosen Approach*: Add `.github/workflows/build-apk.yml` with Java 17 setup, Gradle caching, debug APK assembly, and release APK packaging with artifact uploading.
  - *Why*: Allows the repository owner to generate downloadable APK files automatically on every push or manual dispatch.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                 BYO Connect Native Container                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ Top Cyan Loading Bar (Visibility tied to Progress)  │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │           SwipeRefreshLayout (Cyan Accent)          │   │
│   │                                                     │   │
│   │   ┌─────────────────────────────────────────────┐   │   │
│   │   │               WebView Core                  │   │   │
│   │   │   - JavaScript & DOM Storage Enabled        │   │   │
│   │   │   - Mixed Content & Hardware Acceleration   │   │   │
│   │   └─────────────────────┬───────────────────────┘   │   │
│   └─────────────────────────┼───────────────────────────┘   │
│                             │                               │
│              ┌──────────────┴──────────────┐                │
│              │ Intercept Navigation Events │                │
│              └──────┬───────────────┬──────┘                │
│                     │               │                       │
│           wa.me / tel / mailto   file upload dialog         │
│                     │               │                       │
│                     ▼               ▼                       │
│            ┌─────────────────┐ ┌─────────────────────────┐  │
│            │ External Intent │ │ Camera / Gallery Chooser│  │
│            │ (WhatsApp /     │ │ + FileProvider          │  │
│            │  Phone Dialer)  │ └─────────────────────────┘  │
│            └─────────────────┘                              │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ Error & Offline Overlay (Shown on network failure)  │   │
│   │   - Retry button + Network State Listener           │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Component Breakdown

1. **`MainActivity.kt`**:
   - Manages the lifecycle, `OnBackPressedCallback`, permission launcher, and Activity Result callbacks.
   - Configures `WebView`, `WebChromeClient`, and `WebViewClient`.
   - Intercepts WhatsApp, `tel:`, and external links.
   - Implements `onShowFileChooser` with `FileProvider` URI generation.
   - Manages pull-to-refresh and network state monitoring.

2. **`AndroidManifest.xml`**:
   - Declares `INTERNET`, `ACCESS_NETWORK_STATE`, and optional `CAMERA` with `uses-feature` set to not strictly required.
   - Configures `<provider>` for `FileProvider` to support camera capture without security exceptions.
   - Configures `<queries>` intent declarations for `whatsapp`, `tel`, and `mailto` to comply with Android 11+ package visibility requirements.

3. **`res/xml/file_paths.xml`**:
   - Defines cache directory file paths for temporary camera captures.

4. **`res/values/colors.xml` & `strings.xml`**:
   - Cyan `#00F2EA`, deep black `#000000`, surface `#0D1117`.
   - String resources for app title, offline messages, and file picker options.

5. **`res/drawable/` & Splash Screen**:
   - Vector logo and branding for BYO Connect.
   - Custom splash theme in `themes.xml` for seamless zero-flicker startup.

6. **`.github/workflows/build-apk.yml`**:
   - Automated workflow to checkout code, setup JDK 17, grant Gradle permissions, build APKs, and upload them as GitHub Actions artifacts.
