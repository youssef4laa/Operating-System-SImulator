#!/bin/bash
# Build and run the OS Simulator GUI with Swing
# Requires: Java 11+ (no external dependencies needed - Swing is built-in)
# Run from: /Users/youssef/Code/Operating Systems/OSProject/os

echo "=========================================="
echo "OS Simulator GUI - Builder Script"
echo "=========================================="
echo ""
echo "✓ Using Swing (built-in to Java)"
echo ""
echo "Compiling Java files..."

# Create bin directory if it doesn't exist
mkdir -p bin

# Compile (no JavaFX modules needed - Swing is part of standard Java)
javac -d bin src/os/*.java 2>&1 | tee build.log

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilation successful!"
    echo ""
    echo "Running GUI..."
    echo "=========================================="
    java -cp bin os.Main "$@"
    
else
    echo ""
    echo "❌ Compilation failed. See build.log for details."
    exit 1
fi

