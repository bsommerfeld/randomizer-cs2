# <img src="https://github.com/user-attachments/assets/ab28eba7-4b88-47b4-be10-ac4487d66e23" alt="randomizer" width="24" height="24" style="vertical-align: middle;" />andomizer-CS2

[![Version](https://img.shields.io/badge/version-1.3.3-blue.svg)](https://github.com/bsommerfeld/randomizer-cs2/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/java-24-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk24-archive-downloads.html)
[![NPM Downloads](https://img.shields.io/npm/d18m/randomizer-cs2)](https://www.npmjs.com/package/randomizer-cs2)
[![GitHub Stars](https://img.shields.io/github/stars/bsommerfeld/randomizer-cs2?style=social)](https://github.com/bsommerfeld/randomizer-cs2/stargazers)

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Installation](#-installation)
- [Usage Guide](#-usage-guide)
- [Creating Custom Sequences](#-creating-custom-sequences)
- [Development](#-development)
- [Contributing](#-contributing)
- [Team](#-team)
- [License](#-license)
- [FAQ](#-faq)

## 🎮 Overview

**Randomizer-CS2** is a powerful JavaFX application that adds an element of unpredictability to Counter-Strike 2
gameplay. It allows you to create and trigger random action sequences without modifying the game itself.

Whether you want to prank friends, create entertaining content, or add a challenge to your gameplay, Randomizer-CS2
offers a user-friendly solution with no risk of VAC bans.

> **The Story Behind Randomizer-CS2**
>
> This project began as a fun way to troll friends during spectated matches. We wanted to make players jump at the worst
> moments, fire unexpectedly, or drop weapons mid-fight. Without access to a dedicated server, we created this software
> solution that anyone could use to trigger random actions. What started as Randomizer-CSGO has evolved into the more
> refined Randomizer-CS2.

## ✨ Features

- **Custom Action Sequences**: Build, save, and manage personalized game action sequences
- **Random Triggering System**: Actions execute at random intervals within your configured time range
- **Intuitive Sequence Builder**: Drag-and-drop interface for easy sequence creation
- **Game Focus Detection**: Automatically pauses when CS2 isn't the active window
- **Safe to Use**: Works without game injection or modification (no VAC ban risk)
- **Modern Interface**: Clean, responsive JavaFX UI with smooth animations
- **Resource Efficient**: Minimal system resource usage
- **Sequence Sharing**: Import/export capabilities for sharing custom sequences
- **Regular Updates**: Ongoing development with community feedback integration

## 📸 Screenshots

<table>
  <tr>
    <td>
      <p align="center">
        <img src="https://github.com/user-attachments/assets/761f46f2-bffb-4aab-9a75-e5a42f784578" alt="Randomizer Home Screen" width=500>
        <br>
        <em>Home Dashboard</em>
      </p>
    </td>
    <td>
      <p align="center">
        <img src="https://github.com/user-attachments/assets/005f9d92-6791-411e-8d23-457f74fd909c" alt="Randomizer Builder" width=500>
        <br>
        <em>Sequence Builder Interface</em>
      </p>
    </td>
  </tr>
  <tr>
    <td>
      <p align="center">
        <img src="https://github.com/user-attachments/assets/9ba84d69-472f-4775-bdba-8529bd14028a" alt="Randomizer Settings" width=500>
        <br>
        <em>Settings Configuration Panel</em>
      </p>
    </td>
    <td>
      <p align="center">
        <img src="https://github.com/user-attachments/assets/404e50f5-b5cb-40ea-ba77-5e0ab428b735" alt="Randomizer in Action" width=500>
        <br>
        <em>Randomizer Live in Action</em>
      </p>
    </td>
  </tr>
</table>

## 🚀 Installation

### System Requirements

- **Operating System**: Windows 10/11
- **Java**: JDK 24 or higher
- **Memory**: 100MB minimum
- **Disk Space**: 50MB minimum
- **Additional**: Internet connection for updates and GitHub integration

### Installation Steps

1. **Download**: Get the latest release from
   our [releases page](https://github.com/bsommerfeld/randomizer-cs2/releases/)
2. **Install**: Run the installer and follow the on-screen instructions
3. **Launch**: Start Randomizer-CS2 from your desktop or start menu
4. **Configure**: Set up your preferences in the settings menu
5. **Create or Import**: Build your first action sequence or import pre-made ones

### Pre-made Sequences

Save time by downloading
our [pre-made and playtested sequences](https://github.com/bsommerfeld/randomizer-cs2/tree/master/.randomizer/sequences)!

## 🎯 Usage Guide

### Basic Operation

1. **Launch the Application**: Start Randomizer-CS2
2. **Select a Sequence**: Choose from your saved sequences or create a new one
3. **Configure Timing**: Set minimum and maximum wait times between random triggers
4. **Start the Randomizer**: Click the "Start" button to activate
5. **Launch CS2**: Open Counter-Strike 2
6. **Play Normally**: The randomizer will trigger actions at random intervals
7. **Stop Anytime**: Click "Stop" in Randomizer-CS2 or simply close CS2

### Tips for Best Results

- **Start with Simple Sequences**: Begin with basic actions before creating complex combinations
- **Test in Casual Modes**: Try your sequences in casual game modes before using in competitive
- **Adjust Timing**: Find the sweet spot between too frequent and too rare triggers
- **Use Focus Detection**: Enable the automatic pause feature when tabbing out of CS2
- **Share the Fun**: Record reactions and share with friends or on social media

## 🔧 Creating Custom Sequences

### Using the Sequence Builder

1. **Access the Builder**: Click on "Builder" in the main menu
2. **Browse Available Actions**: Explore the action palette on the bottom panel
3. **Create Your Sequence**: Drag actions from the palette to the sequence editor
4. **Arrange Actions**: Order them as desired (they'll execute in sequence when triggered)
5. **Action Duration**: Change the duration of the action by clicking on it.
6. **Save Your Creation**: Give it a name and description for easy reference

### Available Actions

- **Movement**: Jump, Duck, Move (various directions + mouse)
- **Weapons**: Primary/Secondary weapon switch, Drop current weapon
- **Combat**: Fire, Reload, Use knife
- **Utility**: Use grenades, Deploy equipment
- **Custom**: Create combinations of multiple actions

### Sharing Your Sequences

1. **Export**: Locate your sequences folder via the "Open Sequences Folder" button
2. **Share Files**: Send the sequence files to friends
3. **Import**: Recipients can place files in their sequences folder

## 💻 Development

### Project Structure

Randomizer-CS2 consists of three main modules:

- **randomizer-desktop**: The main application UI and controllers
- **randomizer-model**: Core data models and business logic
- **github-api-client**: Integration with GitHub for repository information

### Building from Source

1. **Prerequisites**:
    - Java JDK 24 or higher
    - Maven 3.6+
    - Git

2. **Clone the Repository**:
   ```bash
   git clone https://github.com/bsommerfeld/randomizer-cs2.git
   cd randomizer-cs2
   ```

3. **Build with Maven**:
   ```bash
   mvn clean install
   ```

4. **Run the Application**:
   ```bash
   java -jar randomizer-desktop/randomizer/target/randomizer.jar
   ```

### Technology Stack

- **UI Framework**: JavaFX
- **Build System**: Maven
- **Dependency Injection**: Guice
- **Logging**: SLF4J with Logback
- **Persistence**: GSON

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### Ways to Contribute

- **Report Bugs**: Open an issue if you find a bug
- **Suggest Features**: Have an idea? Create a feature request
- **Improve Documentation**: Help make our docs clearer
- **Submit Code**: Fork the repo and submit a pull request
- **Create Sequences**: Share your creative action sequences
- **Spread the Word**: Tell others about Randomizer-CS2

### Contribution Guidelines

1. **Fork the Repository**: Create your own fork of the project
2. **Create a Branch**: Make your changes in a new branch
3. **Follow Coding Standards**: Maintain the existing code style
4. **Write Tests**: Add tests for new features
5. **Submit a Pull Request**: Open a PR with a clear description of your changes

## 👥 Team

- **Programming**: [Benjamin Sommerfeld (bsommerfeld)](https://github.com/bsommerfeld)
- **UX Design**: [Kjell Witzurke (bustolio)](https://github.com/bustolio)

Want to join our team? Reach out to us on GitHub!

## 📄 License

Randomizer-CS2 is licensed under the [MIT License](LICENSE).

## ❓ FAQ

### Is Randomizer-CS2 safe to use?

Yes! Randomizer-CS2 doesn't inject code into CS2 or modify game files. It works by simulating keyboard and mouse inputs,
which is completely safe and won't trigger VAC bans.

### Can I use this in competitive matches?

While technically possible, we recommend using Randomizer-CS2 in casual game modes or private matches to avoid
negatively impacting competitive gameplay for others. Or queue as a 5-stack!

### How do I create my own sequences?

Use the built-in Sequence Builder to drag and drop actions into a custom sequence. See
the [Creating Custom Sequences](#-creating-custom-sequences) section for detailed instructions.

### Does this work with other games?

Randomizer-CS2 is specifically designed for Counter-Strike 2, but the underlying technology could work with other games.
Future versions might include support for additional games.

### Where can I get help if I have issues?

Open an issue on our [GitHub repository](https://github.com/bsommerfeld/randomizer-cs2/issues).

---

<p align="center">
  <img src="https://github.com/user-attachments/assets/efffd234-5f9e-4f13-b8a3-539257139d92" width="250" height="auto">
  <br>
  <em>Made with ❤️ by the Randomizer-CS2 Team</em>
</p>

<p align="center">
  <a href="https://github.com/bsommerfeld/randomizer-cs2/stargazers">⭐ Star us on GitHub</a> •
  <a href="https://github.com/bsommerfeld/randomizer-cs2/releases">📥 Download Latest Release</a> •
  <a href="https://github.com/bsommerfeld/randomizer-cs2/issues">🐛 Report Bug</a>
</p>
