# DRIVER-Android
Android client for adding DRIVER records


## Build Status
[![Build Status](https://travis-ci.org/WorldBank-Transport/DRIVER-Android.svg?branch=develop)](https://travis-ci.org/WorldBank-Transport/DRIVER-Android)

## Setup
Before running app from Android Studio, copy over the two tempates:
```
cp templates/configurables.xml.template app/src/main/res/values/configurables.xml
```

Set the URL for your DRIVER API server in `configurables.xml`.

```
cp templates/release.properties.template app/release.properties
```

## Building for Release
To build a signed version of the app that will reload model files when there is a schema update:

  - Copy the keystore file also used to sign the updated model files into the `app` directory
  - Edit the `release.properties` file in the `app` directory to set the properties for the keystore

## Updating base models
A library jar lives at `app/src/main/assets/models.jar` that is used as the initial model class definition,
the backup model classes in case of update failure, and also for running tests. This jar file must be
signed with the release key. If updated, also update the UUID `BACKUP_JAR_SCHEMA_VERSION` in `DriverApp`.
