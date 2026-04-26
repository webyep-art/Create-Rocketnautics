# Create: Rocketnautics 🚀

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1+-orange.svg)](https://neoforged.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-blue.svg)](https://www.minecraft.net/)

**Create: Rocketnautics** is an industrial-aerospace addon for the **Create** mod. It allows players to build realistic, physics-based rockets, launch them into space, and control them with precision.

Unlike many space mods where rockets are just scripted "elevators", this mod utilizes the **Sable / Valkyrien Skies 2** physics engine, turning ships into real physical objects. Thrust, center of mass, orbital mechanics, and stage separation - it's all here, similar to *Kerbal Space Program*!

---

## 🛠️ Main Mechanics & Blocks

### 1. Rocket Thruster 🔥
Classic Liquid Rocket Engine (LRE).
* **Fuel**: Lava. The engine can store a small amount internally but primarily draws from adjacent Fluid Tanks.
* **Control**: Thrust is controlled via the kinetic energy system (Create). You must provide rotation to its built-in pump. Higher RPM results in stronger thrust and faster lava consumption.
* **Physics**: Provides a constant Linear Impulse directly to the rocket's center of mass.

### 2. Vector Thruster 🎛️
Advanced version of the LRE with Thrust Vector Control (TVC).
* **Fuel & Power**: Same as the Rocket Thruster (Lava + Rotation).
* **Main Feature**: The nozzle can tilt! By providing analog redstone signals to the block's sides, you can physically tilt the nozzle (Gimbal).
* **Physics**: Tilting the nozzle changes the thrust angle, creating torque that allows the ship to maneuver, roll, and turn during flight.

### 3. Booster Thruster 🧨
Powerful Solid Rocket Motor (SRM) for overcoming thick atmospheric layers.
* **Fuel**: Coal Blocks. The booster takes solid fuel from attached inventories or nearby blocks.
* **Control**: Activated by a simple redstone signal.
* **Features**: Like real boosters, it *cannot be turned off* or throttled. Once ignited, it roars at full power until all fuel is consumed.

### 4. Stage Separator 💥
Mechanism for creating multi-stage rockets.
* **Principle**: Installed between rocket stages.
* **Activation**: When triggered by redstone, this block instantly "explodes" (disappears), breaking the physical connection between parts of the ship.

---

## 🌌 Space Exploration
The mod includes a full **Space** dimension. By overcoming gravity and the upper atmosphere, players enter orbit where different rules apply: vacuum, microgravity, and the ability to build real space stations using Create mechanisms.

---

## 🏗️ Development & Contributing

### Requirements
* **Java 21**
* **NeoForge** 21.1.228+
* **Create** 1.21.1
* **Sable / Valkyrien Skies 2**

### Setup
1. Clone the repository.
2. Run `./gradlew genSources` to set up the development environment.
3. Use `./gradlew runClient` to test the mod.

---

## 📄 License
This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
