# MLFQ Mutex Quick Reference

## Overview
MLFQ Mutex prevents priority inversion in multi-level feedback queue schedulers through priority inheritance and dynamic priority boost.

## Quick Start

### Automatic Setup (Recommended)
When using MLFQ algorithm, features are automatically enabled:
```java
Scheduler scheduler = new Scheduler();
scheduler.algorithm = "MLFQ";  // Enables MLFQ Mutex automatically
```

### Manual Configuration
```java
Scheduler scheduler = new Scheduler();
MutexManager mutexMgr = scheduler.getMutexManager();

// Enable priority inheritance
mutexMgr.configureMLFQPriorityInheritance(true);

// Enable priority boost to Q0 on unblock
mutexMgr.configureMLFQPriorityBoost(true);
mutexMgr.configureMLFQBoostLevel(0);

// Get detailed MLFQ status
System.out.println(mutexMgr.getMLFQStatus());
```

## Key Concepts

### Priority Inheritance
When high-priority process P1 (Q0) blocks waiting for mutex held by low-priority P3 (Q3):
- P3 temporarily inherits Q0 priority
- P3 gets scheduled with higher priority
- P3 executes faster and releases mutex
- P3 returns to Q3 when done

### Priority Boost
When low-priority process P4 (Q3) is unblocked from mutex:
- P4 is temporarily boosted to Q0
- P4 gets one quantum at high priority
- P4 gets fair chance before returning to Q3

## Configuration Options

| Method | Default | Purpose |
|--------|---------|---------|
| `configureMLFQPriorityInheritance(boolean)` | true (MLFQ) | Enable/disable priority inheritance |
| `configureMLFQPriorityBoost(boolean)` | true (MLFQ) | Enable/disable priority boost |
| `configureMLFQBoostLevel(int)` | 0 | Queue level to boost to (0-3) |

## Monitoring

### Check MLFQ Mutex Status
```java
String status = mutexMgr.getMLFQStatus();
System.out.println(status);
```

Output shows:
- Mutex status (locked/free)
- Current owner and queue level
- Waiting processes and their original queue levels
- Inheritance/boost configuration

### Individual Mutex Status
```java
Mutex m = mutexMgr.getFileMutex();
System.out.println(m.getMLFQStatus());
```

## Operational Behavior

### Timeline Example
```
Clock 0:
  P1 (Q0) executes
  P1 tries semWait file
  → Mutex held by P3 (Q3)
  → P1 blocks, P3 inherits Q0 priority
  Log: "[MLFQ MUTEX] Priority inheritance: P3 boosted to queue Q0"

Clock 1:
  MLFQ selects P3 (now Q0 priority)
  P3 executes, semSignal file
  → P1 unblocked, boosted to Q0
  Log: "[MLFQ MUTEX] Priority boost: P1 boosted from Q0 to Q0"
  → P3 returns to Q3

Clock 2:
  P1 executes with boosted priority
  After quantum: P1 returns to Q0
  Log: "[MLFQ MUTEX] Temporary boost expired: P1 returns to Q0"
```

## Testing MLFQ Mutex

### Test Priority Inheritance
1. Create P1 (Q0) that blocks on mutex held by P3 (Q3)
2. Verify log shows: "Priority inheritance: P3 boosted to queue Q0"
3. Verify P3 executes and releases mutex promptly
4. Verify log shows: "Priority inheritance released: P3 returns to queue Q3"

### Test Priority Boost
1. Create P4 (Q3) blocked on mutex
2. Release mutex → P4 unblocked
3. Verify log shows: "Priority boost: P4 boosted from Q3 to Q0"
4. Let P4 use boosted quantum
5. Verify log shows: "Temporary boost expired: P4 returns to Q3"

## Common Scenarios

### Scenario 1: Multiple Processes Waiting
```
Mutex held by P3 (Q3)
Waiting: P1 (Q0), P2 (Q1), P4 (Q3)

When P3 releases:
→ P1 unblocked first (FIFO)
→ P1 already Q0, no boost needed
→ P2 unblocked second
→ P2 boosted from Q1 to Q0
→ P4 unblocked third
→ P4 boosted from Q3 to Q0
```

### Scenario 2: Avoiding Starvation
```
Without MLFQ Mutex:
- P1 (Q0) waits for P3 (Q3) to release mutex
- P3 never scheduled (P1 has higher priority)
- P1 starves forever

With MLFQ Mutex:
- P1 (Q0) blocks on P3's mutex
- P3 inherits Q0 priority
- P3 scheduled and runs
- Mutex released, P1 unblocked
- System progresses
```

## Performance Notes

- **Minimal Overhead**: One integer field per process
- **Fast Operations**: Inheritance/boost are O(1)
- **Memory Efficient**: Uses HashMap for queue level tracking
- **Backward Compatible**: Non-MLFQ algorithms unaffected

## Troubleshooting

### Issue: Priority Inheritance Not Working
**Solution**: Verify MLFQ algorithm is selected
```java
scheduler.algorithm = "MLFQ";
```

### Issue: Processes Not Getting Boosted
**Solution**: Check that priority boost is enabled
```java
mutexMgr.configureMLFQPriorityBoost(true);
```

### Issue: Want to Disable Features
**Solution**: Disable individually
```java
mutexMgr.configureMLFQPriorityInheritance(false);
mutexMgr.configureMLFQPriorityBoost(false);
```

## Related Documentation

- **MLFQ_MUTEX_IMPLEMENTATION.md** - Detailed implementation guide
- **MUTEX_IMPLEMENTATION.md** - Basic mutex operations
- **GUI_USER_GUIDE.md** - MLFQ algorithm explanation

---

**Last Updated**: April 19, 2026
**Status**: Production Ready ✅
