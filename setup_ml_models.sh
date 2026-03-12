#!/bin/bash

# Configuration
ASSET_DIR="feature/search-semantic/src/main/assets"
MODEL_URL="https://huggingface.co/intfloat/multilingual-e5-small/resolve/main/model.safetensors"
# Note: In production, you'd need the TFLite version of this model.
# I'm providing links to standard models. Conversion to TFLite usually happens via Python.

echo "Creating assets directory..."
mkdir -p "$ASSET_DIR"

echo "Downloading vocabulary file..."
curl -L "https://huggingface.co/intfloat/multilingual-e5-small/resolve/main/vocab.txt" -o "$ASSET_DIR/vocab.txt"

# Important: Multilingual E5 Small is typically ~400MB in its standard form.
# A TFLite version is usually exported from this model for Android.
# I will download a placeholder or small model if you need immediate testing.

echo ""
echo "Setup finished."
echo "Please place your 'multilingual_e5_small.tflite' model file in $ASSET_DIR/"
echo "If you don't have the TFLite version, you can export it using TensorFlow's TFLite Converter."
