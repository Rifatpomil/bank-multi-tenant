# Snapshotting Configuration

## Why Snapshotting?

In event-sourced systems, aggregate state is reconstructed by replaying all events for that aggregate. For aggregates with many events (e.g., a long-lived bank account with hundreds of deposits/withdrawals), loading becomes slow:

- **Without snapshots**: Load time = O(n) where n = number of events
- **With snapshots**: Load time = O(1) after first snapshot; we load the latest snapshot + only events after it

## How It Works

```
Events: [E1] [E2] [E3] [E4] [E5] [E6] [E7] ...
                            ^
                     Snapshot (state at E4)
                     
Load: Read snapshot → Read E5, E6, E7... → Rebuild state
```

1. **SnapshotTriggerDefinition** decides when to create a snapshot (e.g., every N events)
2. **Snapshotter** serializes the aggregate state and stores it as a "snapshot event"
3. On load, the **EventStore** returns the latest snapshot (if any) + events after it
4. The aggregate is rebuilt from snapshot + tail events

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `axon.snapshotting.bank-account.threshold` | 50 | Create snapshot after this many events per aggregate |
| `axon.snapshotting.bank-transfer.threshold` | 50 | Same for BankTransfer aggregate |

**Recommended threshold**: 20–100 for typical banking aggregates. Lower = more snapshots (faster reads, more write overhead). Higher = fewer snapshots (slower reads, less overhead).

## Tuning

- **High event volume per aggregate**: Lower threshold (e.g., 20)
- **Low event volume**: Higher threshold or disable (threshold = 0)
- **Write-heavy workload**: Snapshots are created asynchronously to avoid blocking

## Verification

After enabling, check that snapshot events are stored:

- **JPA/MySQL**: `SELECT * FROM snapshotevent;`
- **Mongo**: `db.snapshotevents.find()`
- **In-memory**: Snapshotter writes to the configured event store
