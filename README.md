# Blueprint Hell (NetworkOpSim) — Real-Time Network Topology Simulator

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Build](https://img.shields.io/badge/Build-Apache%20Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Architecture](https://img.shields.io/badge/Architecture-MVC%20%7C%20Client--Server-blue)](#)

A real-time network topology simulation game and software architecture case study developed at **Sharif University of Technology**. Players act as network operators, designing topologies and routing heterogeneous data packets across specialized network systems while managing packet physics, noise corruption, and cyber threats.

---

## 🏛️ Architectural Evolution & Case Study

Alongside the core simulation engine, our team authored an architectural case study titled **"Architecture of a Real-Time Multiplayer Simulator: A Case Study of the NetworkOpSim Game"**, documenting the progression of the system across three distinct engineering phases:

1. **Phase 1 — Monolithic Prototype:** Rapid prototyping of core packet movement, wire routing, and GUI rendering.
2. **Phase 2 — Decoupled MVC & Design Patterns:** Extraction of a platform-agnostic `GameEngine` and implementation of the **Strategy Pattern** (`SystemBehavior`) to model extensible network nodes in adherence to SOLID principles.
3. **Phase 3 — Multi-Module Client-Server Architecture:** Separation into four Maven modules (`Shared`, `Core`, `Client`, `Server`) featuring DTO-based socket communication, **AOP-inspired Dynamic Proxies** for centralized exception handling, and deterministic replay validation for anti-cheat integrity.

---

## ✨ Core Simulation Mechanics & Features

- **Specialized Network Nodes:**
  - `SOURCE` & `SINK`: Network entry and destination endpoints.
  - `VPN`: Encrypts passing packets into `PROTECTED` states or enforces confidential distancing.
  - `SPY` & `SABOTEUR`: Intercepts routing paths, injects wave noise, or converts packets into `TROJAN` payloads.
  - `ANTI_TROJAN`: Radial scanning nodes that neutralize Trojan packets back into safe messenger streams.
  - `DISTRIBUTE` & `MERGE`: Fragments high-volume packets (`LARGE_8`, `LARGE_10`) into bit-streams and reconstructs them downstream.
- **2D Packet Physics & Impact Waves:** Custom vector physics (`Vector2D`) modeling wire-adhesion stability, off-wire friction, radial collision shockwaves, and dynamic wire bend-points.
- **Tactical Modifiers & Skills:** In-game modifiers including *Atar* (shockwave suppression), *Airyaman* (collision phase-shift), *Anahita* (noise purification), and localized wire effects (*Aergia*, *Sisyphus*, *Eliphas*).
- **State Persistence:** Automatic and manual JSON serialization (`SaveManager` & `LevelLoader`) preserving full topology, active packet vectors, and wire bend-points across 5 progressive levels.

---

## 🛠️ Tech Stack

- **Language:** Java 17
- **Frameworks:** JavaFX / Swing (GUI & AnimationTimer loop), JSON (`org.json`)
- **Build & Dependency Management:** Apache Maven (`mvnw`)
- **Software Engineering Patterns:** MVC, Strategy Pattern, Dynamic Proxy (AOP), DTO, Spatial Grid Collision Detection

---

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher
- Apache Maven 3.8+ (or use the included Maven Wrapper `mvnw`)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/amirhossein2004-mozafari/BlueprintHellGame.git
cd BlueprintHellGame

# Compile and run using Maven Wrapper
./mvnw clean compile javafx:run
👥 Authors & Contributors
Amirhossein Mozafari — Department of Computer Science, Sharif University of Technology
Collaborative case study co-authored with Abolfazl Moslemi, Shayan Salehi, and Shayan Esfandiari.
