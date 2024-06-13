import React, { useState, useEffect } from "react";
import {
  Text,
  View,
  NativeModules,
  StyleSheet,
  SafeAreaView,
  ImageBackground,
  TouchableOpacity,
  Image,
} from "react-native";
import Icon from "react-native-vector-icons/Ionicons";
import { styleNameToImageUrl } from "./constants";
import type { AvailableModels } from "./types";
import { applyStyle } from "./utils";
import { getImageUri } from "./utils";

const { StyleTransferModule } = NativeModules;

export default function App() {
  const [imageUri, setImageUri] = useState("");
  const [styleImageUri, setStyleImageUri] = useState("");
  const [currentStyle, setCurrentStyle] = useState<AvailableModels | null>(
    null
  );

  const handleCameraPress = async (isCamera: boolean) => {
    const imageUri = await getImageUri(isCamera);
    if (typeof imageUri === "string") {
      setImageUri(imageUri as string);
      setStyleImageUri(imageUri as string);
      setCurrentStyle(null);
    }
  };

  useEffect(() => {
    const initModule = async () => {
      try {
        await StyleTransferModule.initModules();
      } catch (err) {
        console.error(err);
      }
    };
    initModule();
  }, []);

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.mainContainer}>
        <Text style={styles.titleText}>Image editor</Text>
        <View style={styles.imageContainer}>
          {imageUri && (
            <Image
              source={{
                uri: styleImageUri ? (styleImageUri as string) : imageUri,
              }}
              style={styles.imageComponent}
            />
          )}
        </View>
        <View style={styles.managePhotoContainer}>
          <TouchableOpacity
            onPress={async () => await handleCameraPress(true)}
          >
            <Icon name="camera-outline" size={36} color="navy" />
          </TouchableOpacity>
          <TouchableOpacity
            onPress={async () => await handleCameraPress(false)}
          >
            <Icon name="images-outline" size={36} color="navy" />
          </TouchableOpacity>
        </View>
        <View style={styles.bottomContainer}>
          <Text style={styles.filtersText}>Filters</Text>
          <View style={styles.stylePickerContainer}>
            {(Object.keys(styleNameToImageUrl) as AvailableModels[]).map(
              (style, index) => (
                <TouchableOpacity
                  key={index}
                  style={[styles.stylePickerTouchableOpacity]}
                  onPress={async () => {
                    if (style === currentStyle) return;
                    setCurrentStyle(style);
                    const outputUri = await applyStyle(imageUri, style);
                    setStyleImageUri(outputUri);
                  }}
                >
                  <ImageBackground
                    source={styleNameToImageUrl[style]}
                    style={styles.stylePickerImageBackground}
                    imageStyle={styles.stylePickerButtonImageStyle}
                  >
                    {currentStyle === style && (
                      <View style={styles.selectedBorderStyle} />
                    )}
                  </ImageBackground>
                </TouchableOpacity>
              )
            )}
          </View>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  mainContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    borderTopWidth: 4,
    borderColor: "navy",
  },
  titleText: {
    fontSize: 24,
    color: "navy",
    marginVertical: "5%",
  },
  imageContainer: {
    width: "90%",
    height: "65%",
  },
  imageComponent: {
    height: "100%",
    width: "100%",
    borderRadius: 10,
  },
  bottomContainer: {
    height: "15%",
    width: "100%",
    borderTopColor: "navy",
    borderTopWidth: 4,
    alignItems: "center",
    justifyContent: "center",
  },
  stylePickerContainer: {
    flexDirection: "row",
    justifyContent: "space-between",
    height: "75%",
    width: "90%",
    backgroundColor: "white",
  },
  stylePickerImageBackground: {
    width: "100%",
    height: "100%",
  },
  stylePickerTouchableOpacity: {
    width: "20%",
    height: "100%",
  },
  stylePickerButtonImageStyle: {
    flex: 1,
    width: null,
    height: null,
    resizeMode: "cover",
    borderRadius: 10,
  },
  filtersText: {
    color: "navy",
    fontSize: 20,
    marginVertical: "2%",
  },
  managePhotoContainer: {
    flexDirection: "row",
    justifyContent: "space-around",
    width: "50%",
    marginTop: "5%",
    height: "5%",
    marginBottom: "5%",
  },
  selectedBorderStyle: {
    ...StyleSheet.absoluteFillObject,
    borderWidth: 4,
    borderColor: "navy",
    borderRadius: 10,
    padding: 2,
  },
});
