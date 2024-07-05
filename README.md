# Executorch Style Transfer Demo App

Follow these steps to set up and run the app:

### IOS


1. **Install dependencies**

    ```sh
    yarn
    ```

2. **Run the app on iOS**

    ```sh
    yarn expo run:ios
    ```


### Android

1. **Install dependencies**
    ```sh
    yarn
    ```

2. **Get prebuilt libexecutorch runtime**
    ```sh
    cd android/app/src/main/jniLibs/arm64-v8a
    wget https://github.com/software-mansion-labs/executorch-style-transfer/releases/download/v1.0.0/android-libexecutorch-xnnpack.zip
    unzip android-libexecutorch-xnnpack.zip
    ```

3. **Run the app**
    ```sh
    yarn expo run:android
    ```
    