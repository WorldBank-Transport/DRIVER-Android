# DRIVER-Android Android client for adding DRIVER records


## Build Status
[![Build Status](https://travis-ci.org/WorldBank-Transport/DRIVER-Android.svg?branch=develop)](https://travis-ci.org/WorldBank-Transport/DRIVER-Android)

## Getting Started
To set up Android Studio to build and run this project, there are a few steps you need to perform first.

- [Generate a keystore](#generate-a-keystore)
- [Generate and host a certificate file](#generate-and-host-a-certificate-file)
- [Build `models.jar` file for the DRIVER environment's default Record Schema](#build-modelsjar)
- [(Optional) Set up Google Single-Sign-On (SSO)](#optional-set-up-single-sign-on-sso)
- [Set up project configuration files](#set-up-project-configuration-files)
- [Build for release](#build-for-release)

### Generate a keystore
A keystore file is used to create certificates and sign resources, and having one is needed for most of the steps. It should be kept private since it is used to verify the authenticity of the Android app that you build.

If you're developing for an existing environment it's possible a keystore will already exist (Potentially in `gradle/data/` or another location where the environment's configuration is kept) and you can skip this step. Otherwise, if the file doesn't exist or you're developing an Android application for your environment for the first time, you will need to generate one.

To generate the keystore, run:
```
keytool -genkey -v -keystore driver.keystore -alias driver -keyalg RSA -keysize 2048 -validity 10000
```

You will be asked to provide a password and some information about yourself, your organizational team, and your organization. Make sure to keep the password for later.

Once you have generated the keystore file, you will need to configure the target DRIVER environment with it so that the `models.jar` file you will generate later can be signed correctly. You should copy the keystore file to the `gradle/data/` directory of your DRIVER installation and `app/driver.keystore` of your DRIVER-Android repository and store the keystore password in the appropriate Ansible group\_vars file under `keystore_password`.

If your environment has not been created yet, it should be safe to do that now. Otherwise you will want to run the `celery.yml` playbook [from the DRIVER deployment steps](https://github.com/WorldBank-Transport/DRIVER/blob/master/doc/system-administration.md) for the changes to take effect.

### Generate and host a certificate file
A certificate file signed by the keystore file will ultimately be used by Android to verify the APK's authenticity, and one will need to be created and hosted publicly for users to access.

Projects that already have a keystore file may already have a certificate as well, but if not you can use the keystore file to generate a certificate:
```
keytool -exportcert -keystore driver.keystore -alias driver -file driver_android_certificate.pem
```

The certificate file should then be placed on a publicly accessible web server at a URL that _uses HTTPS and does not redirect_.
Storing the certificate file as a publicly readable Amazon S3 object or on GitHub Pages are both good options.

### Build models.jar
DRIVER uses Gradle to dynamically generate a `models.jar` file that will contain the default Record Schema for the application for the initial model class definition, the backup model classes in case of update failure, and also for running tests.

To trigger Gradle to build this file on a DRIVER environment, determine the UUID of the Record Schema you want the application to use and in a web browser load the URL  `https://{your-driver-domain}/api/jars/{schema-uuid}/`.

You should get back a 201 Created response while the Gradle process builds the `models.jar` file asynchronously. After a minute or so, refreshing the page should produce a 200 OK response. Once you get that response, if you append `?format=jar` to the URL, it will then download the file.

> **Note** If you do not get a 200 OK response after several minutes, check:
> - Is `driver-gradle` running on the celery server?
> - Is there any output in `/var/log/upstart/driver-gradle.log` when you reload the `/api/jars/` URL?
> If there is no output, restarting the service with `sudo service driver-gradle restart` often resolves communication issues. You may need to reload the `/api/jars` URL again after to trigger a new build.

Once downloaded, copy the `models.jar` file to  `app/src/main/assets/models.jar`.

The app requires the `models.jar` file to have been signed by the keystore. You can use `jarsigner` to verify that the `models.jar` file has been signed correctly:
```
$ jarsigner -verify -certs -keystore app/driver.keystore app/src/main/assets/models.jar

jar verified.
```

### (Optional) Set up Single-Sign-On (SSO)
This will enable users to sign in with their Google account using OpenID, instead of requiring them
to have a username and password set up.

> **Note** The Android app requires users to have at least Analyst permissions within DRIVER to use, but users created by SSO default to Public permissions. Users created this way will need to have their account permissions upgraded by an Administrator user to be able to use the app.

- Create Android OAuth credentials in the [Google APIs Developer Console](https://console.developers.google.com/)
  - For OAuth to work your environment must already be configured with OAuth credentials for front-end use - it may be appropriate to configure these under the same account.
- Download the credentials JSON file
- Copy the credentials JSON file to the DRIVER-Android `app/` directory.

### Set up project configuration files
If starting a new Android project, in the DRIVER-Android repository, create a `configurables.xml` file from the template:
```
cp templates/configurables.xml.template app/src/main/res/values/configurables.xml
```

Customize fields with their appropriate values:
- Set the URL for your DRIVER API server in `configurables.xml` under `api_server_url`.
- Set the URL to the hosted certificate file in `signing_cert_pem_url`.
- Set the label for the default Record Type (eg. "Incident") in `record_type_label`.
  - Note: You can see all Record Types by going to `https://{your-driver-domain}/api/recordtypes/`.
- Set the UUID for the Record Schema the `models.jar` file was made from in `backup_jar_schema_version`.
- (For SSO) Set the Client ID for the target environment's Google APIs Developer Console "Web Application" credentials (These should be found in `oauth_client_id` of the appropriate group\_vars file) in `oauth_client_id`.

Next, if needed create a `release.properties` file from template:
```
cp templates/release.properties.template app/release.properties
```

Fill in the password to the keystore. Both passwords should be the same.

### Build for release
Once the project configuration has been set up, you can use Android Studio to build and test the app.

To build a APK for public release, go to Build -> Generate Signed APK... and follow the prompts.

Assuming building is successful, you will have an `app-release.apk` file. This file can be placed at any secure web-accessible location and downloaded to users' Android phones.

## Troubleshooting

### JAR file structure

A proper `models.jar` file should have a structure like so (you can use the `jar` command or simply unzip the file since JAR files are ZIP files):

```
$ jar tvf app/src/main/assets/models.jar
   163 Wed Oct 31 13:34:54 EDT 2018 META-INF/MANIFEST.MF
   254 Wed Oct 31 13:34:54 EDT 2018 META-INF/DRIVER.SF
  5229 Wed Oct 31 13:34:54 EDT 2018 META-INF/DRIVER.RSA
     0 Wed Oct 31 13:34:54 EDT 2018 META-INF/
 35600 Wed Oct 31 13:34:54 EDT 2018 classes.dex
```

### Verifying Signature/Certificate

The app requires `app/driver.keystore` to have been used to sign the JAR file at `app/src/main/assets/models.jar`. You can use `jarsigner` to verify that the models JAR file has been signed correctly:

```
$ jarsigner -verify -certs -keystore app/driver.keystore app/src/main/assets/models.jar

jar verified.
```

You can also use `keytool` to confirm that the password used when creating the keystore is what you expect:

```
keytool -list -v -keystore app/driver.keystore -storepass <secret>
```

The output from this command can also be used to verify that the certificate matches up with the publicly-accessible certificate that the app is using to check against the keystore (see [Generate a keystore](#generate-a-keystore)).

Note that the certificate 1) must be accessible over HTTPS and 2) the URL cannot redirect. You can do `curl <signing_cert_pem_url>` to easily verify that there is no redirect.

### Updating models JAR

If you are running (or debugging) the app and your changes to the default `models.jar` do not appear to be taking effect, this may be due to the model classes having already been loaded by the class loader persisting in memory.

To see changes to `models.jar`, you may need to try the following:

* Clear the cache in Android Studio and restart
* Manually uninstall the app on the device
* Restart the device before reinstall
* Ensure there's only one model JAR in the project
