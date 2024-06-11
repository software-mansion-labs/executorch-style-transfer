import type { AvailableModels } from "./types";

export const styleNameToImageUrl: { [key in AvailableModels]: number } = {
  candy: require("./assets/candy.jpg"),
  mosaic: require("./assets/mosaic.jpg"),
  rain_princess: require("./assets/rain_princess.jpg"),
  udnie: require("./assets/udnie.jpg"),
};
