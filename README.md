# DRIVER-Android Android client for adding DRIVER records


## Build Status
[![Build Status](https://travis-ci.org/WorldBank-Transport/DRIVER-Android.svg?branch=develop)](https://travis-ci.org/WorldBank-Transport/DRIVER-Android)

## Developing
See the steps under [Setup](#setup), below. If you need to deploy to an existing stack or set up a new stack
then you should read this whole document thoroughly.

## Generating a Keystore
A valid keystore is required for most of the remaining steps. If you are developing an app that is
targeted at an existing installation of DRIVER, then that installation should have a pre-existing
keystore that you can use.

However, if you need to build an app for a newly launched installation of DRIVER, you will need to
generate a completely new keystore.

To generate a keystore, run:
`keytool -genkey -v -keystore driver.keystore -alias driver -keyalg RSA -keysize 2048 -validity 10000`

You will be asked to provide a password and some information about yourself and your organization.

Next, you will also need to generate a certificate from the keystore. To do this, run:
`keytool -exportcert -keystore driver.keystore -alias driver -file driver_android_certificate.pem`

The keystore file should be kept private since it is used to verify the authenticity of the Android
app that you build. The certificate file should be placed on a publicly accessible web server at a URL that _uses HTTPS and does not redirect_.
GitHub Pages is a good option.

You should [copy the keystore file](https://github.com/WorldBank-Transport/DRIVER/#developing) to the `gradle/data` file of your DRIVER installation and
configure the keystore password in the Ansible group\_vars before launching the new stack. If you
have already launched the stack, you can put these two items into place and then re-run the Celery
playbook.

## Setup
Before running app from Android Studio, copy over the two templates:
```
cp templates/configurables.xml.template app/src/main/res/values/configurables.xml
```

- Set the URL for your DRIVER API server in `configurables.xml` under `api_server_url`.
- Set the "signing\_cert\_pem\_url" to be the URL to the keystore certificate file (if building for
  an existing stack, this should already be available; if building for a new stack, you will need to
  upload the certificate file somewhere)
- If you want to support Google Sign-In, follow the instructions below to set it up, and then fill
  in the "oauth\_client\_id".

```
cp templates/release.properties.template app/release.properties
```

- Fill in the password to the keystore. Both passwords should be the same.

## Setting up Google Sign-In
This will enable users to sign in with their Google account using OpenID, instead of requiring them
to have a username and password set up. Note that logging in with Google the first time will create
a read-only user account; to use this app, the user will need to have at least 'analyst' access
granted to their account.

  - Create Android OAuth credentials in Google API Developer console. Copy downloaded credentials
    json to `app` directory.  Note that the certificate fingerprint in the API console must be for
    the signing keystore file.  The console will provide you with a command that you can run to
    generate this fingerprint.
  - Set the client ID for the API console web application configuration (not Android) in
    `configurables.xml` under `oauth_client_id`.

## Updating base models
A library jar lives at `app/src/main/assets/models.jar` that is used as the initial model class
definition, the backup model classes in case of update failure, and also for running tests. This jar
file must be signed with the release key. To generate this file, select the schema that you want to
use as your initial class definition, and then access
`https://{your-driver-domain}/api/jars/{schema-uuid}/`. You will receive a 201 response. Wait about a
minute and then reload, passing in the query parameter `format=jar` to download the jar file (eg. `https://{your-driver-domain}/api/jars/{schema-uuid}/?format=jar`). You should receive a jar file that you can use for models.jar. If you
update models.jar, also update the UUID `BACKUP_JAR_SCHEMA_VERSION` in `DriverApp`.

## Building for Release
To build a signed version of the app that will reload model files when there is a schema update:

  - Copy the keystore file also used to sign the updated model files into the `app` directory.
  - Edit the `release.properties` file in the `app` directory to set the properties for the
    keystore.
  - Go to Build -> Generate Signed APK... and follow the prompts.
  - Assuming building is successful, you will have an `app-release.apk` file. This file can be
    placed at any secure web-accessible location and downloaded to users' Android phones.

## Troubleshooting

### JAR file structure

A proper models JAR should have a structure like so (you can use the `jar` command or simply unzip the file since JAR files are ZIP files):

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

The output from this command can also be used to verify that the certificate matches up with the publicly-accessible certificate that the app is using to check against the keystore (see [Generating a Keystore](#generating-a-keystore)).

Note that the certificate 1) must be accessible over HTTPS and 2) the URL cannot redirect. You can do `curl <signing_cert_pem_url>` to easily verify that there is no redirect.

### Updating models JAR

If you are running (or debugging) the app and your changes to the default `models.jar` do not appear to be taking effect, this may be due to the model classes having already been loaded by the class loader persisting in memory.

To see changes to `models.jar`, you may need to try the following:

* Clear the cache in Android Studio and restart
* Manually uninstall the app on the device
* Restart the device before reinstall
* Ensure there's only one model JAR in the project
