/**
 * OS Simulator - Operating System simulator with GUI and mutex support
 * Requires Java 21+ with Swing support
 */
module os {
    requires java.base;
    requires java.desktop;  // For Swing/AWT
    requires java.logging;   // For logging support
}