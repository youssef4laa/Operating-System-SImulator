# 🎯 START HERE - OS Simulator GUI Setup

## Quick Start (5 Minutes)

You've received a complete GUI implementation for your OS Simulator. Here's how to get started:

### Step 1: Install JavaFX (Required)

**On macOS with Homebrew** (Easiest):
```bash
brew install javafx-sdk
export JAVAFX_PATH_ENV=$(brew --cellar javafx-sdk)/*/libexec
```

**On macOS/Linux/Windows** (Manual):
1. Download from: https://gluonhq.com/products/javafx/
2. Extract to a location
3. Set environment variable: `export JAVAFX_PATH_ENV=/path/to/javafx-sdk`

**Add to your shell config** to make it permanent:
- macOS: Add to `~/.zprofile` or `~/.bashrc`
- Linux: Add to `~/.bashrc`
- Windows: Set as environment variable

**Need detailed help?** → See [JAVAFX_SETUP.md](JAVAFX_SETUP.md)

### Step 2: Build and Run

```bash
cd /path/to/OSProject/os
bash build-gui.sh
```

If successful, the GUI will launch automatically!

### Step 3: Learn the Basics

While the GUI is running, read: [GUI_QUICK_REFERENCE.md](GUI_QUICK_REFERENCE.md)

It has:
- Button descriptions
- Color legends
- Quick fixes
- Basic workflows

## Common Installation Issues

### "JavaFX not found"
```bash
# Check if environment variable is set
echo $JAVAFX_PATH_ENV

# Should show a path like /usr/local/Cellar/javafx-sdk/17.0.1/libexec
```

### "Module javafx.controls not found"
→ Go back to Step 1, re-install JavaFX

### "build-gui.sh: command not found"
```bash
# Make sure you're in the right directory
cd /Users/youssef/Code/Operating\ Systems/OSProject/os

# Make script executable
chmod +x build-gui.sh

# Try again
bash build-gui.sh
```

## First Time Using the GUI?

### Recommended First Session:
1. **Initialize** - Click the [Initialize] button
2. **Choose Step Mode** - Select "Step" radio button
3. **Start** - Click [Start]
4. **Execute** - Click [Step] to run one instruction
5. **Observe** - Watch:
   - Memory grid update
   - Debug console log
   - Queues change
   - Timeline update
6. **Repeat** - Click [Step] again
7. **Understanding** - Read Debug Console output

**Time needed**: 10-15 minutes

## What You Got

✅ **Complete GUI with**:
- Real-time memory and queue visualization
- Step-by-step execution mode for learning
- Automatic execution mode for testing
- Debug console with real-time output
- Timeline statistics
- Support for all 3 scheduling algorithms (RR, HRRN, MLFQ)
- Easy debugging

✅ **Complete Documentation**:
- Installation guide
- User guide
- Quick reference
- Implementation details
- Troubleshooting help

## File Structure

```
Your Project /
├─ os/
│  ├─ src/os/               (Source code)
│  ├─ bin/                  (Compiled files - created by build)
│  └─ build-gui.sh          (Build script YOU RUN)
│
├─ DOCUMENTATION_INDEX.md   ← Read this next!
├─ GUI_QUICK_REFERENCE.md   ← Read this while trying GUI
├─ GUI_USER_GUIDE.md        ← Read when you want full instructions
├─ README_GUI.md            ← Read for technical details
├─ JAVAFX_SETUP.md          ← Read if you have installation issues
└─ GUI_IMPLEMENTATION_SUMMARY.md  ← Read to understand code
```

## Documentation Quick Links

**Start with these files in order:**

1. **[JAVAFX_SETUP.md](JAVAFX_SETUP.md)** (if you haven't installed JavaFX)
   - How to install JavaFX
   - Environment variables
   - Troubleshooting

2. **[GUI_QUICK_REFERENCE.md](GUI_QUICK_REFERENCE.md)** (while using GUI)
   - Button functions
   - Color meanings
   - Quick tips

3. **[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** (to find anything)
   - Navigation guide
   - Feature index
   - Help for any question

4. **[GUI_USER_GUIDE.md](GUI_USER_GUIDE.md)** (when you want full details)
   - Complete feature list
   - Detailed workflows
   - Advanced topics

5. **[README_GUI.md](README_GUI.md)** (for technical understanding)
   - System architecture
   - Component details
   - How it all works together

## 30-Second Test

After running `bash build-gui.sh`:

```
1. Wait for GUI window to appear (might take 5-10 seconds)
2. Click [Initialize] button
3. Click [Start] button
4. Click [Step] button
5. You should see changes in:
   - Memory panel (left)
   - Debug Console (center)
   - Timeline (right)
6. Congratulations! It works! ✅
```

## Need Help?

### Installation Problems?
→ [JAVAFX_SETUP.md](JAVAFX_SETUP.md)

### Don't know how to use something?
→ [GUI_QUICK_REFERENCE.md](GUI_QUICK_REFERENCE.md)

### Want detailed instructions?
→ [GUI_USER_GUIDE.md](GUI_USER_GUIDE.md)

### Lost or confused?
→ [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)

### Want to understand the code?
→ [README_GUI.md](README_GUI.md)

## System Requirements

| Component | Requirement |
|-----------|-------------|
| Java | 11 or higher (17+ preferred) |
| JavaFX | 17 or higher |
| Memory | 4GB minimum |
| Screen | 1400x900 minimum |

## Quick Commands Reference

```bash
# Navigate to project
cd /Users/youssef/Code/Operating\ Systems/OSProject/os

# Set JavaFX path (on macOS with Homebrew)
export JAVAFX_PATH_ENV=$(brew --cellar javafx-sdk)/*/libexec

# Check if JavaFX is set
echo $JAVAFX_PATH_ENV

# Build and run
bash build-gui.sh

# Or run CLI mode (without GUI)
java -cp bin os.Main -cli

# Clear previous builds
rm -rf bin
mkdir -p bin
```

## What's Different From Before?

**Old (Console Only)**:
```
[Output text only]
[No visualization]
[Hard to debug]
```

**New (With GUI)**:
```
✅ Real-time memory visualization
✅ Queue animation
✅ Debug console with timestamps
✅ Step-by-step execution
✅ Algorithm comparison
✅ Mutex monitoring
✅ Statistics tracking
```

## Next Steps

**Right now:**
1. Install JavaFX (if needed)
2. Run `bash build-gui.sh`
3. Click around the GUI

**In the next 10 minutes:**
1. Read GUI_QUICK_REFERENCE.md
2. Try each button
3. Run one full simulation with Step mode

**Later:**
1. Read GUI_USER_GUIDE.md
2. Compare different algorithms
3. Use Debug mode to understand execution

## Troubleshooting Checklist

- [ ] Java installed: `java --version` shows 11+
- [ ] JavaFX environment variable set: `echo $JAVAFX_PATH_ENV` shows a path
- [ ] In correct directory: `pwd` shows `.../os`
- [ ] build-gui.sh exists here: `ls build-gui.sh`
- [ ] Make it executable: `chmod +x build-gui.sh`
- [ ] Run the script: `bash build-gui.sh`
- [ ] GUI window appears (might take 5-10 seconds)
- [ ] You can click buttons
- [ ] Debug console has output

If any of these fail, check the corresponding documentation file above.

## FAQ

**Q: Do I need JavaFX installed?**  
A: Yes, and you must set the environment variable.

**Q: How long will installation take?**  
A: 5-10 minutes typically. 30 minutes if you have issues.

**Q: Can I use the GUI without installation?**  
A: No, JavaFX is required. Alternative: Run in CLI mode with `java -cp bin os.Main -cli`

**Q: What if the GUI won't launch?**  
A: Check JAVAFX_SETUP.md troubleshooting section.

**Q: How do I run it again after installation?**  
A: Just run `cd os && bash build-gui.sh`

**Q: Can I modify the code?**  
A: Yes! See README_GUI.md for architecture details.

---

## You're Ready! 🚀

1. Install JavaFX
2. Run `bash build-gui.sh`
3. Read while using the GUI
4. Enjoy better understanding of OS concepts!

**Questions?** Check the documentation files listed at the top.

---

**Good luck! Happy simulating! 🎉**

For any issues, the [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md) can point you to the right answer.
