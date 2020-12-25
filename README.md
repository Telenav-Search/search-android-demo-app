# Telenav SDK Demo App

This is a sample app provided to show developers what Telenav SDKs looks like in action, and help them see a real example of integrating Telenav SDKs into their applications.

## About Telenav

Telenav (NASDAQ: TNAV) is a leading provider of connected car and location-based services, focused on transforming life on the go for people – before, during, and after every drive. Leveraging our location platform, global brands such as GM, Toyota, and AT&T deliver custom connected car and mobile experiences. Fortune 500 advertisers and local advertisers can now reach millions of users with our highly-targeted advertising platform. To learn more about how Telenav’s location platform powers personalized navigation, mapping, big data intelligence, social driving, and location-based ads, visit [our site](https://www.telenav.com/).

## About SDK

### Entity Service

**Entity Service, a subset of Telenav Vivid Nav SDK**, enables developers to build powerful search features for a variety of applications. In addition to powerful onebox search, the SDK features include query prediction (auto suggestion / word prediction), entity detail lookup, and discovery features. The combination of the code and methods provided with this SDK, plus access to the Telenav cloud services, enable developers to build powerful and engaging search based applications.

### Data Collector Service

**Data Collector Service, a subset of Telenav Vivid Nav SDK**, collects and stores many kinds of event data into Telenav’s data-warehouse (Cloud) so that Telenav can use such data for various analysis to enable personalization related features and functionality in addition to product developments & improvements. Such as:

* Home area detection
* Predictive Cards
* Personalization
* RoadSense
### OTA Service

**OTA (Over The Air) Service, a subset of Telenav Vivid Nav SDK**, keeps local data up-to-date for on-board navigation. It enables the developers to replace stale entities such as POIs, addresses, brands, and other entity types with the latest data published by various data vendors taking advantage of the Telenav cloud. To optimize bandwidth usage for data transfer, data replacement does not cover the whole map. Instead, each update only targets a specific area (a.k.a. OTA area) determined by the Telenav technology based on a combination of factors including home location, workplace, user profile, etc.

## Installation
Clone this repository and import into **Android Studio**
```bash
git clone git@github.com:Telenav-Search/telenav-sdk-demo-app.git
```

## Configuration

### Secrets:
It your `gradle.properties` add the following fields providet to you by Telenav:
```gradle
telenav_user_id=...
telenav_api_key=...
telenav_api_secret=...
telenav_cloud_endpoint=...
```

You also need to add credentials for `telenav.jfrog` artifactory in order to download dependencies
```gradle
telenav_jfrog_user=...
telenav_jfrog_password=...
```

### Device preparations:
In your `gradle.properties` add the following field:
```gradle
telenav_data_dir=...
```
Make sure to download SDK data released by Telenav, unzip it and copy it to path of `telenav_data_dir` on the device.
You also can unzip it anywhere on the device and chose it as data folder on app startup.

### Device Requirements:

PLease note that Telenav SDKs only works with `armeabi-v7a`, `arm64-v8a` and `and x86_64` devices

## Support
Please feel free to submit issues with any bugs or other unforeseen issues you experience. We work diligently to ensure that the `master` branch is always bug-free and easy to clone and run from Android Studio. If you experience problems, open an issue describing the problem and how to reproduce it, and we'll be sure to take a look at it.
