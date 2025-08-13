# Blockchain Framework For Federated Learning (Java)

A modular, multi-node blockchain prototype for **federated learning**. Nodes communicate over TCP sockets, use cryptographic hashing & signatures, and run a BFT-inspired voting flow to agree on **model update blocks**. Designed for ML workflows (e.g., Keras weights), but flexible enough for custom payloads.

---

## Table of Contents

- [Overview](#overview)  
- [Architecture](#architecture)  
- [Components](#components)  
- [Setup](#setup)  
- [Usage](#usage)  
- [CSV Format](#csv-format)  
- [Repository Layout](#repository-layout)  
- [Diagrams & Visuals (Placeholders)](#diagrams--visuals-placeholders)  
- [Limitations & Improvements](#limitations--improvements)  

---

## Overview

- **Distributed nodes** exchange serialized messages over TCP (`localhost`), propose blocks, vote, and commit once quorum is reached.  
- **Blocks** contain hashes of the **data** and **model** (e.g., Keras weights) and are **digitally signed** by the proposer.  
- **Consensus** uses a **BFT-inspired quorum** (quorum = `2 * faulty + 1`).  
- **Federated learning ready**: nodes can **embed model updates** as block payloads for verifiable aggregation.  
- **Config-driven** multi-node orchestration via `nodes.csv`, with **automatic keypair generation** per node (including `Genesis`).

---

## Architecture

> **High level**: Node → proposes block → network broadcast → peers validate & vote → quorum reached → block committed → next round.

**Core flow**
1. **Genesis**: Chain starts with a trusted `Genesis` block (signed by `Genesis` key).
2. **Propose**: A node forges a block (hashes model/data, signs payload) and broadcasts `PROPOSE`.
3. **Vote**: Peers validate (hashes, signatures, previous hash, round) and broadcast `VOTE`.
4. **Commit**: On quorum (`2*faulty + 1`), block is committed, round increments, state resets.

---

## Components

| Module | Description |
|---|---|
| `Block.java` | Immutable block with `index`, `timestamp`, `dataHash`, `modelHash`, `previousHash`, `hash`, `signature`, `blockProposerId`. |
| `Blockchain.java` | Chain management: genesis creation, forging, validation (hash, sig, link, model consistency), commit. |
| `CryptoUtils.java` | SHA-256 hashing (strings/files), digital signatures, key load (Base64), bulk public key loader from `validators/`. |
| `KeyPairGeneratorTool.java` | Generates **2048-bit RSA** keypairs and writes `validators/<id>/public.key` & `private.key`. |
| `Message.java` | Serializable message: `{ blockId, type (PROPOSE/VOTE), block, senderId }`. |
| `MessageHandler.java` | Consensus router: routes messages, records votes, handles quorum, commit, round/timeout management. |
| `PortLink.java` | TCP messaging: per-node server socket, threaded handlers, broadcast to peer ports. |
| `Node.java` | Node runtime: constructs network + blockchain + handler; CLI to print chain / propose blocks. |
| `NodeConfigLoader.java` | Parses `nodes.csv` → `NodeConfig` list. Builds peer lists, sets `faultyCount` from boolean. |
| `NodeTerminalLauncher.java` | **Compiles** all sources, **auto-generates keys** if missing (for `Genesis` and each node), launches nodes in **macOS Terminal** tabs. |
| `Launcher.java` | Entry point that calls `NodeTerminalLauncher.materialise("nodes.csv", "<project-root>").` |

> ⚙️ **Federated learning note**: The `model` attribute (and data path) is meant for ML workflows (e.g., Keras weights). Swap in any custom payload if you’re not doing FL.

---

## Setup

### Prerequisites
- Java 11+  
- macOS (for the Terminal automation via AppleScript in the launcher)  
  - Linux/Windows users: run nodes manually (see **Usage**) or adapt the launcher.  
- `bash`, `zsh`, `javac` available in PATH

### Fault Tolerance Requirement
To tolerate up to **f** faulty or malicious nodes in the BFT-inspired consensus,  
the total number of nodes **n** must satisfy:

    3f + 1 ≤ n

Example:  
- For `f = 1`, you need at least `n = 4` total nodes.  
- For `f = 2`, you need at least `n = 7` total nodes.

This ensures quorum (`2f + 1` votes) can be reached even with faulty nodes present.

### 1) Clone
```bash
git clone https://github.com/<your-username>/<your-repo>.git
cd <your-repo>
```

### 2) Prepare `nodes.csv`
See [CSV Format](#csv-format).

### 3) Launch (auto-compile + auto-keygen + spawn terminals)
Use the provided `Launcher` (edit the path to your repo if needed):

```java
// Launcher.java
public class Launcher {
    public static void main(String[] args) {
        new NodeTerminalLauncher().materialise("nodes.csv", "/absolute/path/to/your/repo");
    }
}
```

Compile & run:
```bash
javac Launcher.java
java Launcher
```

The launcher will:
- `find` and **compile** all `*.java` into `out/`
- **create keys** under `validators/<id>/` if missing (including `Genesis`)
- **open Terminal tabs** and start each node with its parameters

> 🧪 Prefer manual control? See **Usage** for direct `java Node ...` commands.

---

## Usage

### Manual run (per node)
```bash
# Example
java -cp out Node node1 densenet121 5001 5002,5003 0 10
#           ^id   ^model          ^myPort ^peerPorts  ^faultyCount ^timeoutSecs
```

**In the node’s CLI:**
- `0` → Print chain  
- `1` → Propose block (enter model/data path when prompted)

---

## CSV Format

**Expected header & columns (comma-separated):**
```
id,port,model,timeoutSeconds,faulty
```

**Example `nodes.csv`:**
```
id,port,model,timeoutSeconds,faulty
node1,5001,model_v1.h5,10,false
node2,5002,model_v1.h5,10,false
node3,5003,model_v1.h5,10,true
```

- `faulty` is a boolean → internally mapped to `faultyCount` (1 for `true`, else 0).  
- Peers are auto-derived from all other `port`s.

---

## Repository Layout

```
.
├─ Block.java
├─ Blockchain.java
├─ CryptoUtils.java
├─ KeyPairGeneratorTool.java
├─ Launcher.java
├─ Message.java
├─ MessageHandler.java
├─ Node.java
├─ NodeConfigLoader.java
├─ NodeTerminalLauncher.java
├─ PortLink.java
├─ nodes.csv                # you create this
├─ validators/              # keys auto-generated here
│  └─ <id>/{public.key,private.key}
├─ model/                   # optional: store your model files
└─ out/                     # compiled classes written here
```

---

## Diagrams & Visuals 

### 1) System Architecture 
```
+-----------------+       TCP Sockets       +-----------------+
|   Node A        | <---------------------> |    Node B       |
|  - Blockchain   |                         |  - Blockchain   |
|  - MessageHandler|                         |  - MessageHandler|
|  - PortLink     |                         |  - PortLink     |
+-----------------+                         +-----------------+
         ^                                           ^
         |                                           |
         v                                           v
+-----------------+                         +-----------------+
|   Node C        | <---------------------> |    Node D       |
|  - Blockchain   |                         |  - Blockchain   |
|  - MessageHandler|                         |  - MessageHandler|
|  - PortLink     |                         |  - PortLink     |
+-----------------+                         +-----------------+

Message flow: PROPOSE → VOTE → COMMIT
```
### 2) Consensus Sequence
```
Node A        Node B        Node C
  |             |             |
  |--PROPOSE--->|             |
  |--PROPOSE--------------->  |
  |             |             |
  |<--VOTE------|             |
  |             |<--VOTE------|
  |             |             |
  |--COMMIT---> |             |
  |--COMMIT--------------->   |
  |             |             |
Quorum reached → State reset → Next round
```
### 3) Class Overview 
```
+--------------+
|   Node       |
+--------------+
| - id         |
| - peers[]    |
| - blockchain |
| - handler    |
+--------------+
       |
       v
+--------------+       +--------------+
| Message      |       | PortLink     |
+--------------+       +--------------+
| - type       |       | - myPort     |
| - payload    |       | - peerPorts[]|
+--------------+       +--------------+
       |
       v
+--------------+
| MessageHandler|
+--------------+
| - queue      |
| - process()  |
+--------------+

Other:  
Block <-> Blockchain  
CryptoUtils (sign, verify, hash)
```
### 4) FL Flow 
```
[Keras Model Training @ Node] 
        |
        v
[Model Weights Update]
        |
        v
[Propose Block with Update]
        |
        v
[Consensus (BFT-inspired)]
        |
        v
[Block Commit to Ledger]
        |
        v
[Aggregated Global Model]
```
---

## Limitations & Improvements

- **Hardcoded paths**: Key dirs like `validators/<id>/` and some file paths are implicit.  
  → *Make configurable via env vars/CLI/props; add path validation.*
- **Localhost only**: `PortLink` dials `localhost` per peer.  
  → *Support remote IPs, TLS sockets, and configurable hosts.*
- **Ephemeral sockets**: Each broadcast opens/closes a connection.  
  → *Use connection pooling/persistent channels (NIO/Netty).*
- **No persistence**: Chain held in memory only.  
  → *Add durable storage (files/DB), snapshots, replay on startup.*
- **Basic consensus**: BFT-inspired but simplified, single timeout thread.  
  → *Richer leader election, view changes, multiple timeouts.*
- **Minimal logging and metrics**: Console prints only.  
  → *Integrate structured logging, metrics, health checks.*
- **macOS-specific launcher**: AppleScript + Terminal.  
  → *Provide cross-platform scripts (Linux `gnome-terminal`, Windows `cmd`/PowerShell) or Docker Compose.*

---

### Credits

Built by **Prem Savla**. Designed for ML + distributed systems experimentation; adaptable for broader ledger use cases.
