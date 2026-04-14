#!/bin/bash
# Build and run the OS Simulator GUI
# Run from: /Users/youssef/Code/Operating Systems/OSProject/os

echo "=========================================="
echo "OS Simulator GUI - Builder Script"
echo "=========================================="

# Compile with JavaFX modules
JAVAFX_PATH="/path/to/javafx-sdk"  # Update this path

# If JAVAFX_PATH not set, try common locations
if [ ! -d "$JAVAFX_PATH" ]; then
    # Try Homebrew location
    if [ -d "/usr/local/Cellar/javafx-sdk" ]; then
        JAVAFX_PATH="/usr/local/Cellar/javafx-sdk/latest"
    # Try common Linux location
    elif [ -d "/opt/javafx-sdk" ]; then
        JAVAFX_PATH="/opt/javafx-sdk"
    else
        echo "ERROR: JavaFX SDK not found. Please set JAVAFX_PATH."
        echo "Download from: https://gluonhq.com/products/javafx/"
        exit 1
    fi
fi

echo "Using JavaFX from: $JAVAFX_PATH"
echo ""
echo "Compiling..."

# Compile all Java files
javac \
    --module-path "$JAVAFX_PATH/lib" \
    --add-modules javafx.controls,javafx.fxml \
    -d bin \
    src/os/*.java \
    src/module-info.java 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "Running GUI..."
    
    # Run the application
    java \
        --module-path "$JAVAFX_PATH/lib" \
        --add-modules javafx.controls,javafx.fxml \
        -cp bin \
        os.OSSimulatorGUI
else
    echo "❌ Compilation failed!"
    exit 1
fi
