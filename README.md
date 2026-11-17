<p align="center">
  <img alt="DD GUI icon" width="192" height="192" src="web_hi_res_512.png"/>
</p>

# DD GUI

DD GUI is a graphical interface for running the `dd` command on Android. Select input and output paths, choose supported conversions, and review each write before execution.

**Root access is required.** The app cannot guarantee that a selected path is accessible on every device.
Supports Android 7.0 (API 24) and later.

[![Top language](https://img.shields.io/github/languages/top/Lennoard/DDGUI)](https://github.com/Lennoard/DDGUI)
[![License](https://img.shields.io/github/license/Lennoard/DDGUI)](LICENSE.md)
![GitHub downloads](https://img.shields.io/github/downloads/Lennoard/DDGUI/total)
![GitHub contributors](https://img.shields.io/github/contributors/Lennoard/DDGUI)

## Features

- Copy data between selected files and explicitly selected block-device paths, including image backup and write workflows, subject to device access restrictions.
- Choose input and output paths with the root-aware file picker; browse directories and create files or folders.
- Select `dd` conversions: `notrunc`, `noerror`, `sync`, `fsync`, and `swab`.
- Review the exact source, destination, and selected conversions before a write. The Settings option can hide repeat warning dialogs, but a review and separate final confirmation are always required.
- Select a special or unrecognized filesystem entry, such as a possible boot or recovery block device, after an explicit warning. This does not test whether Android or the device permits access.
- Use adaptive Compose layouts for compact and wider windows.

## Safety

Writing to a block device can permanently destroy data or make a device unbootable. 
Check the source and destination carefully before confirming.
DD GUI does not discover or verify partition layouts, determine whether an image is compatible with a device, or bypass Android root and security restrictions.

## Technologies

- Kotlin and Android Gradle Plugin with Gradle Kotlin DSL
- Jetpack Compose, Material 3, and Navigation 3
- Kotlin Coroutines and `StateFlow`
- libsu for root shell and filesystem access
- Koin for dependency injection

## Screenshots

<div align="center">

|                                                             Main operation                                                             |                                                              Root file picker                                                               |                                                 Completed operation                                                 |
|:--------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------------------:|
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" alt="DD GUI input, output, and conversion form" width="200"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" alt="Root file picker browsing block-device entries" width="200"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" alt="Completed dd operation" width="200"> |

</div>

## Download

<a href="https://apt.izzysoft.de/fdroid/index/apk/com.androidvip.ddgui"><img alt="Get it on IzzyOnDroid" height="64" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"/></a>
<a href="https://xdaforums.com/t/app-7-0-dd-gui-a-graphical-interface-for-the-dd-command.4054763/"><img alt="Get it on XDA Labs" height="64" src="https://i.imgur.com/Pz29bXH.png"/></a>

## Build

The project uses Gradle 9.6, Android Gradle Plugin 9.5, Android SDK 37, and Java 11 bytecode. Use a JDK supported by the pinned Gradle and Android Gradle Plugin versions, and install Android SDK Platform 37.

```sh
./gradlew :app:assembleDebug
./gradlew test
```

## License

This project is licensed under the MIT License. See [LICENSE.md](LICENSE.md).
