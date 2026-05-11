# Create: Cosmonautics

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1+-orange.svg)](https://neoforged.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-blue.svg)](https://www.minecraft.net/)

**Create: Cosmonautics** is a high-fidelity industrial-aerospace expansion for the **Create** mod. It enables the construction of physics-driven launch vehicles, orbital stations, and interstellar exploration systems.

Featuring dynamic rigid body physics, the mod integrates mechanics such as thrust-to-weight ratios, center of mass alignment, and atmospheric drag, providing an immersive aerospace experience within the Minecraft ecosystem.

---

## Core Systems and Components

### 1. Advanced Propulsion Systems
The mod features various engine types with unique performance characteristics:
*   **Rocket Thruster**: A standard liquid rocket engine. It consumes liquid fuels (Kerosene, Diesel, Gasoline, or Lava) and provides steady linear thrust.
*   **Vector Thruster**: Features Thrust Vector Control (TVC). The nozzle can be gimbaled via redstone inputs, allowing for precise attitude control and maneuvering without specialized reaction wheels.
*   **Booster Thruster**: A high-thrust solid rocket motor. Once ignited via redstone, it provides maximum thrust until its fuel supply (Coal Blocks) is exhausted, making it ideal for the initial launch phase.

### 2. Physical Mechanics
*   **Dynamic Gravity**: Gravity strength scales with altitude. Players and ships experience microgravity (Zero-G) upon reaching space altitudes or the dedicated space dimension.
*   **Reentry Effects**: Descending through the atmosphere at high velocities triggers intense visual heating effects and atmospheric drag.

### 3. Space Environment
*   **Seamless Transitions**: The mod manages transitions between the Overworld and the Space dimension. Ships reaching threshold altitudes automatically jump dimensions, maintaining their momentum and entity attachments.
*   **Procedural Asteroids**: Space is populated with procedurally generated asteroids of varying sizes and compositions. These can be mined for rare resources like Titanium.
*   **Atmospheric Hazard**: High altitudes and the vacuum of space require specialized life-support equipment. Players without oxygen systems will face rapid suffocation.

### 4. EVA and Utility
*   **Jetpack System**: Provides personal mobility in microgravity and planetary environments. Features throttle control and state synchronization across the server.
*   **Magnetic Boots**: Allows players to remain securely attached to ship surfaces even during high-acceleration maneuvers or Zero-G conditions.


---

## Technical Specifications and Requirements

### Dependencies
*   **Java 21**
*   **NeoForge** 21.1.228+
*   **Create** 0.6+ (for Minecraft 1.21.1)
*   **Sable API** (Core physics backend)

### Setup for Developers
1. Clone the repository.
2. Synchronize the Gradle project with your IDE (IntelliJ or VS Code). For manual setup, run `./gradlew neoForgeIdeSync` to prepare the environment.
3. Use `./gradlew runClient` to launch a test instance or `./gradlew build` to compile the mod.

---

## License
This project is licensed under the **GNU General Public License v3**. Detailed terms can be found in the [LICENSE](LICENSE) file.
