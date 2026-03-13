# Smart Delivery System — Agent-Based Simulation

A multi-agent delivery simulation built with **JADE** (Java Agent DEvelopment Framework) and **JXMapViewer2**, running on a live OpenStreetMap of M'sila, Algeria.

Three autonomous delivery agents negotiate orders in real time using the **Contract Net Protocol**, drive along real road routes fetched from the OSRM routing engine, and report back to a central warehouse agent — all visualized on an interactive map with a live status dashboard.

![Simulation Screenshot](screenshot.png)

---

## Features

- **Live OSM map** — real tiles of M'sila, draggable and zoomable
- **3 autonomous agents** (red, green, purple) — each an independent JADE agent running in its own thread
- **Contract Net Protocol** — warehouse broadcasts CFPs, agents bid based on estimated travel time, winner is selected automatically
- **Real road routing** — routes fetched from the free OSRM API (`router.project-osrm.org`), with straight-line fallback when offline
- **Animated movement** — agents move along waypoints at 30 km/h simulated speed
- **Dynamic order pins** — yellow when waiting, switches to the assigned agent's color when picked up
- **Live side panel** — order table with status, assigned agent, and elapsed time (stops counting when delivered)
- **Dark mode UI** — color-coded status cells, dark scrollbars, stats strip

---

## Architecture

```
Main.java
├── SimulationWindow      ← Swing GUI (map + side panel)
│   └── MapPanel          ← JXMapViewer + custom painters
├── WarehouseAgent        ← JADE agent: generates orders, runs CNP
├── DeliveryAgent (×3)    ← JADE agent: bids, drives, delivers
├── MapRegistry           ← static bridge: agent threads → Swing EDT
└── model/
    ├── Order             ← lifecycle: PENDING → ASSIGNED → IN_TRANSIT → DELIVERED
    └── Location          ← lat/lon + haversine distance
```

### Agent Interaction (Contract Net Protocol)

```
Warehouse ──CFP──────────────► Agent 1, 2, 3
          ◄──PROPOSE (bid)──── Agent 1 (free)
          ◄──PROPOSE (bid)──── Agent 2 (free)
          ◄──REFUSE (busy)──── Agent 3
          ──ACCEPT_PROPOSAL──► Agent 2 (lowest bid)
          ──REJECT_PROPOSAL──► Agent 1
          ◄──INFORM (done)──── Agent 2
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Agent framework | JADE 4.5.0 |
| Map rendering | JXMapViewer2 2.6 |
| Map tiles | OpenStreetMap |
| Road routing | OSRM (free public API) |
| GUI | Java Swing |
| Build | Maven |
| Java | JDK 21 |

---

## Getting Started

### Prerequisites

- JDK 21+
- Maven 3.8+
- Internet connection (for OSM tiles and OSRM routing — simulation still works offline with straight-line fallback)

### Run

```bash
git clone https://github.com/Younes-Barkat/Delivery-System-Simulation.git
cd Delivery-System-Simulation
mvn clean package
mvn exec:java -Dexec.mainClass="com.smartdelivery.Main"
```

### `pom.xml` dependencies

```xml
<repositories>
  <repository>
    <id>jade-repo</id>
    <url>https://jade.tilab.com/maven/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.tilab.jade</groupId>
    <artifactId>jade</artifactId>
    <version>4.5.0</version>
  </dependency>
  <dependency>
    <groupId>org.jxmapviewer</groupId>
    <artifactId>jxmapviewer2</artifactId>
    <version>2.6</version>
  </dependency>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.36</version>
  </dependency>
</dependencies>
```

---

## Project Structure

```
src/main/java/com/smartdelivery/
├── Main.java
├── agents/
│   ├── WarehouseAgent.java
│   ├── DeliveryAgent.java
│   └── MapRegistry.java
├── gui/
│   ├── MapPanel.java
│   └── SimulationWindow.java
└── model/
    ├── Location.java
    └── Order.java
```

---

## How It Works

1. **Startup** — warehouse registers in the JADE Yellow Pages, then waits 3 seconds for all delivery agents to boot and register
2. **Order generation** — warehouse spawns 3 random orders at startup (max 6 active at once), then replaces each delivered order after a 6-second cooldown
3. **Dispatch** — every 2.5 seconds, the warehouse polls the pending queue and broadcasts a CFP to all registered delivery agents
4. **Bidding** — each free agent calculates its estimated travel time and replies with a PROPOSE; busy agents send REFUSE
5. **Assignment** — warehouse picks the lowest bid, sends ACCEPT to the winner and REJECT to the rest
6. **Delivery** — winning agent fetches a road route from OSRM, walks along the waypoints at 250ms per step, then notifies the warehouse on arrival
7. **Return** — agent drives back to the warehouse along the return route and marks itself available again

---

## Map Controls

| Action | Control |
|--------|---------|
| Pan | Click and drag |
| Zoom in/out | Mouse scroll wheel |
| Reset view | Buttons in floating legend |

---

## Simulation Parameters

| Parameter | Value | Location |
|-----------|-------|----------|
| Max active orders | 6 | `WarehouseAgent.java` |
| Agent speed | 30 km/h | `DeliveryAgent.java` |
| Waypoint step delay | 250 ms | `DeliveryAgent.java` |
| Order respawn delay | 6 s | `WarehouseAgent.java` |
| Dispatch tick | 2.5 s | `WarehouseAgent.java` |
| City bounding box | 35.685–35.740 N, 4.505–4.580 E | `WarehouseAgent.java` |

---

## Author

**Younes Barkat**
GitHub: [@Younes-Barkat](https://github.com/Younes-Barkat)

---

## License

MIT
