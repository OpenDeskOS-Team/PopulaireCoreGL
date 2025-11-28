# PopulaireCoreGL 🚀

PopulaireCoreGL is a simulated BIOS environment written in Java using OpenGL (LWJGL 2.x). It aims to provide a realistic BIOS/UEFI-like interface. The project is cross-platform (Windows, Linux, macOS) and supports booting external OS JARs, disk management, and BIOS configuration.

---

## ✨ Features
- 🖥️ **BIOS/UEFI Simulation**: Realistic graphical BIOS interface with multiple tabs (Information, Boot, Advanced, Updates, Exit)
- 💾 **Boot Order Management**: Detects disks and allows users to configure boot order
- 📦 **External OS Booting**: Boots external Java-based OS JARs from `/cd` or `/disks` folders
- 🔄 **Update System**: Built-in updater for downloading and applying new versions
- 🛠️ **Hardware Detection**: Detects some GPU bugs (e.g., Intel Xe driver issues)
- ⚡ **Configurable RAM and Fast Boot**: Adjust RAM allocation and enable/disable fast boot
- 🖋️ **Custom Fonts**: Uses bundled VGA/FreeMono fonts for retro look
- 🌐 **Cross-Platform**: Runs on Windows, Linux, and macOS (Java 8+ required)

---

## 🛠️ Installation

### Prerequisites
- ☕ Java 8 or higher (JDK recommended)
- [LWJGL 2.9.3](https://legacy.lwjgl.org/) (included via Gradle)
- 🪟 Windows: Native DLLs are provided in `libs/` and `src/main/resources/natives/windows64/`

### Build Instructions
1. **Clone the repository**
   ```powershell
   git clone <repo-url>
   cd PopulaireCoreGL
   ```
2. **Build the project**
   ```powershell
   ./gradlew build
   ```
   The output JAR will be in `build/libs/`.

3. **Run the application**
   ```powershell
   java -jar build/libs/PopulaireCoreGL-<version>.jar
   ```

---

## 🚦 Running PopulaireCoreGL in Production

For end users, running PopulaireCoreGL is simple and does not require building from source. Follow these steps:

### 1️⃣ Download the Latest Release
- 📥 Go to the GitHub Releases page of PopulaireCoreGL.
- 📦 Download the latest `PopulaireCoreGL-<version>.jar` from the published packages.
- 📁 Place the JAR file in a dedicated folder (e.g., `PopulaireCoreGL/`).

### 2️⃣ First Launch Behavior
- 🏁 On the first launch, PopulaireCoreGL will automatically:
  - 📤 Extract required native libraries (DLLs/SOs) into the same folder as the JAR.
  - 📂 Create the folders `cd/` and `disks/` in the same directory for external OS and disk management.

### 3️⃣ How to Run

#### 🪟 On Windows
- 🖱️ **Double-click** the JAR file (if Java is installed and associated with `.jar` files), or
- Use a batch script. Example `run.bat`:
  ```bat
  @echo off
  java -jar PopulaireCoreGL-<version>.jar
  pause
  ```
  Save this as `run.bat` in the same folder and double-click to launch.

#### 🐧 On Linux/macOS
- Use a shell script. Example `run.sh`:
  ```sh
  #!/bin/sh
  java -jar PopulaireCoreGL-<version>.jar
  read -p "Press enter to exit"
  ```
  Make it executable:
  ```sh
  chmod +x run.sh
  ./run.sh
  ```

---

## 🕹️ Usage
- ⏳ On launch, you have 5 seconds to press `F2` or `DELETE` to enter the BIOS, or `F12` for the boot menu.
- ⚙️ Configure boot order, RAM, and other settings in the BIOS interface.
- 💽 To boot an external OS, place a compatible JAR in `/cd` or `/disks`.
- 🔄 Use the update tab to check for and apply updates.

---

## 🤝 Contributing
1. 🍴 Fork the repository and create a feature branch.
2. 📝 Follow Java code style conventions and document public methods/classes.
3. 📬 Submit pull requests with clear descriptions.
4. 🐞 Report issues or feature requests via GitHub Issues.

---

## 📄 License
This project is licensed under the **OpenDesk Base-Project License v1.0**. See [LICENSE](LICENSE) for details.
- 🏠 Free for personal and non-commercial use
- 🛠️ Modifications allowed (with attribution and license inclusion)
- 🚫 Redistribution of the base prohibited without written consent

---

## 📬 Contact & Support
- 👤 Maintainer: OpenDeskTeam
- ❓ For support, open an issue on the GitHub repository.

---

*PopulaireCoreGL is an educational and experimental project. Not intended for use as a real firmware replacement.*
