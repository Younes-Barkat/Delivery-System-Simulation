# Smart Delivery System — Agent-Based Simulation

A multi-agent delivery simulation built with **JADE** and **JXMapViewer2**, running on a live OpenStreetMap of M'sila, Algeria.

Before the simulation starts, you pick how many agents you want (up to 10) and how many orders should be active at once (up to 15). Then you watch them negotiate, drive along real roads, and compete for deliveries — all on a live map with a dark-mode dashboard.

![Simulation Screenshot](screenshot.png)

---

## What it does

Agents are autonomous — they register themselves, listen for job offers, calculate their own bids, drive their own routes, and report back when done. The warehouse never tells anyone where to go. It just announces jobs and picks whoever bids lowest.

The whole coordination mechanism is the **Contract Net Protocol**: warehouse broadcasts a call for proposals, agents bid with their estimated travel time, the closest available agent wins, everyone else gets rejected. Simple rule, decent results.

```
Warehouse ──[ CFP ]──────────────► D1, D2, D3...
          ◄──[ PROPOSE: 97s  ]──── D2 (free, closer)
          ◄──[ PROPOSE: 142s ]──── D1 (free, farther)
          ◄──[ REFUSE: BUSY  ]──── D3 (busy)
          ──[ ACCEPT ]──────────► D2
          ──[ REJECT ]──────────► D1
          ◄──[ INFORM: DONE  ]──── D2 (after delivery)
```

Routes come from the free **OSRM** public API — real streets, real turns. If the API is unreachable, agents fall back to a straight-line approximation and keep going.

---

## Features

- **1 to 10 delivery agents** — each gets a unique color, configurable before launch
- **1 to 15 concurrent orders** — the warehouse fills slots automatically via a background ticker
- **Live OpenStreetMap** of M'sila — draggable, zoomable, real tiles
- **Real road routing** via OSRM, with offline fallback
- **Color-coded pins** — yellow while waiting, switches to the agent's color when picked up
- **Dark mode dashboard** — live order table, event log, stats strip (delivered / active / agents)
- **Order timer** — counts up per row, freezes automatically when delivered
- **End-of-simulation summary** — total deliveries, avg wait time, fastest/slowest delivery, agent leaderboard with progress bars
- **Launch dialog** — configure agents and order count before each run

---

## Project structure

```
src/main/java/com/smartdelivery/
├── Main.java
├── agents/
│   ├── WarehouseAgent.java      generates orders, runs Contract Net
│   ├── DeliveryAgent.java       bids, drives, delivers, returns
│   └── MapRegistry.java         thread-safe bridge between JADE and Swing
├── gui/
│   ├── LaunchDialog.java        pre-simulation config screen
│   ├── SimulationWindow.java    main window — map + side panel + stats
│   ├── MapPanel.java            JXMapViewer + 4 custom painters
│   └── SummaryDialog.java       end-of-simulation analytics
└── model/
    ├── Order.java               PENDING → ASSIGNED → IN_TRANSIT → DELIVERED
    └── Location.java            lat/lon + haversine distance
```

---

## How agents are built

Each delivery agent runs three JADE behaviours in sequence:

**WaitForJob** (CyclicBehaviour) — listens forever for CFPs. If free, calculates ETA and bids. If busy, refuses. Only calls `block()` when genuinely idle — this is important, blocking while delivering would stall the movement loop.

**GoDeliver** (Behaviour) — fetches the OSRM route on first call, then walks along waypoints one step at a time at 250ms per step. Finishes when it reaches the destination.

**GoBack** (Behaviour) — same idea, but drives back to the warehouse. Sets `free = true` when it arrives so the agent can take the next job.

The warehouse uses two TickerBehaviours simultaneously — one dispatches pending orders every 2.5 seconds, the other fills empty order slots every 1.5 seconds. A WakerBehaviour delays the first spawn by 3 seconds so agents have time to register before the CFPs start flying.

---

## Agent colors

10 agents, 10 colors. Consistent across the map dot, route line, delivery pin, and summary leaderboard.

| Agent | Color |
|---|---|
| Delivery-1 | Red |
| Delivery-2 | Green |
| Delivery-3 | Purple |
| Delivery-4 | Cyan |
| Delivery-5 | Orange |
| Delivery-6 | Pink |
| Delivery-7 | Lime |
| Delivery-8 | Sky Blue |
| Delivery-9 | Coral |
| Delivery-10 | Mint |

---

## Getting started

You need JDK 21+ and Maven. An internet connection is recommended (for map tiles and OSRM routing) but not required — it degrades gracefully offline.

```bash
git clone https://github.com/Younes-Barkat/Delivery-System-Simulation.git
cd Delivery-System-Simulation
mvn clean package
mvn exec:java -Dexec.mainClass="com.smartdelivery.Main"
```

A launch dialog appears first. Pick your agent count and order limit, then hit Start.

### Maven dependencies

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

## Map controls

| Action | How |
|---|---|
| Pan | Click and drag |
| Zoom | Mouse scroll wheel |
| Zoom buttons | `+` / `−` in the bottom-left corner |

---

## Tunable parameters

Most things that affect simulation behavior are constants at the top of their respective files. Easy to find and change.

| What | Default | Where |
|---|---|---|
| Agent speed | 30 km/h | `DeliveryAgent.SPEED` |
| Step delay (visual speed) | 250 ms | `DeliveryAgent.STEP` |
| Dispatch interval | 2500 ms | `WarehouseAgent.TICK` |
| Order fill interval | 1500 ms | `WarehouseAgent` |
| Post-delivery spawn delay | 6000 ms | `WarehouseAgent.SPAWN_DELAY` |
| M'sila bounding box | 35.685–35.740 N, 4.505–4.580 E | `WarehouseAgent` |

Max agents and max orders are set at runtime via the launch dialog.

---

## A note on bugs that were fixed

A few non-obvious issues came up during development that are worth documenting in case anyone extends this:

**Duplicate order IDs** — using `System.currentTimeMillis() % 10000` caused ID collisions when multiple orders spawned in the same millisecond. Fixed by using a simple `int seq` counter instead.

**Agent taking two orders at once** — the ACCEPT handler didn't check `free` before accepting, so a stale ACCEPT message from a previous bid cycle could be picked up while already delivering. Fixed by adding `&& free` to the guard condition.

**Agents not moving after assignment** — `WaitForJob` was calling `block()` unconditionally, which suspended the JADE thread and prevented `GoDeliver` from being scheduled. Fixed by making `block()` conditional on `free`.

**GUI flickering** — agents were touching Swing components from JADE background threads. Fixed by wrapping all repaint calls in `SwingUtilities.invokeLater()` inside `MapPanel` and `SimulationWindow`.

---

## Tech stack

| | |
|---|---|
| Agent framework | JADE 4.5.0 |
| Map rendering | JXMapViewer2 2.6 |
| Map tiles | OpenStreetMap |
| Road routing | OSRM (public free API) |
| GUI | Java Swing |
| Build | Maven |
| Java | JDK 21 |

---

## Authors

Barkat Younes — [@Younes-Barkat](https://github.com/Younes-Barkat)
Benkhelil Mohamed El Amin
Rashwane Attoui

Under the supervision of **Dr. Meliouh Amel** — M'sila University, Department of Computer Science

---

## License

MIT